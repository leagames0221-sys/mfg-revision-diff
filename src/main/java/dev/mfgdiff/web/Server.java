package dev.mfgdiff.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.mfgdiff.B2mmlReader;
import dev.mfgdiff.Definition;
import dev.mfgdiff.DiffResult;
import dev.mfgdiff.Differ;
import dev.mfgdiff.NodeMatcher;
import dev.mfgdiff.Report;
import dev.mfgdiff.SecureXml;
import dev.mfgdiff.XlsxExport;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The screen: a small web server on the JDK's own {@code com.sun.net.httpserver}, listening on 127.0.0.1
 * only. {@code java -cp ... dev.mfgdiff.web.Server [--port 8080]}.
 *
 * <pre>
 * GET  /                          upload form
 * POST /compare                   two B2MML files (or sample=1) -> 303 to /result/{id}
 * GET  /result/{id}?filter=       result page
 * GET  /result/{id}/export.xlsx   Excel file
 * GET  /app.css, /app.js          static files
 * </pre>
 */
public final class Server {
    /** Largest request body, the same value as the largest input file (SecureXml.MAX_BYTES, 8 MiB). */
    public static final int MAX_BODY = SecureXml.MAX_BYTES;
    public static final int DEFAULT_PORT = 8080;

    private static final String CSP = "default-src 'none'; script-src 'self'; style-src 'self'; form-action 'self'; "
            + "base-uri 'none'; frame-ancestors 'none'";
    private static final Pattern RESULT_PATH = Pattern.compile("/result/([0-9a-f]{32})(/export\\.xlsx)?");

    private final HttpServer http;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final OriginGuard guard;
    private final ResultStore store = new ResultStore();

    private Server(int port) throws IOException {
        // Only the loopback address: nothing outside this PC can connect (docs/sources.md S11.4).
        this.http = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        this.guard = new OriginGuard(http.getAddress().getPort());
        http.createContext("/", this::handle);
        http.setExecutor(pool);
    }

    /** Starts a server. Port 0 picks a free port (used by the tests). */
    public static Server start(int port) throws IOException {
        Server s = new Server(port);
        s.http.start();
        return s;
    }

    public int port() {
        return http.getAddress().getPort();
    }

    public void stop() {
        http.stop(0);
        pool.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--port") && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            } else {
                System.err.println("usage: java -cp ... dev.mfgdiff.web.Server [--port 8080]");
                System.exit(2);
            }
        }
        Server s = start(port);
        System.out.println("http://127.0.0.1:" + s.port() + "/  (Ctrl+C to stop)");
    }

    // ---------------------------------------------------------------- routing

    private void handle(HttpExchange ex) throws IOException {
        try {
            if (!guard.hostAllowed(ex.getRequestHeaders())) {
                sendError(ex, 403, "このアドレスでは受け付けません。");
                return;
            }
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (path.equals("/compare")) {
                if (!method.equals("POST")) {
                    sendError(ex, 405, "POST で送ってください。");
                } else {
                    compare(ex);
                }
                return;
            }
            if (!method.equals("GET")) {
                sendError(ex, 405, "この操作はできません。");
                return;
            }
            switch (path) {
                case "/" -> send(ex, 200, "text/html; charset=utf-8", Pages.index().getBytes(StandardCharsets.UTF_8));
                case "/app.css" -> send(ex, 200, "text/css; charset=utf-8", resource("/web/app.css"));
                case "/app.js" -> send(ex, 200, "text/javascript; charset=utf-8", resource("/web/app.js"));
                default -> result(ex, path);
            }
        } catch (RuntimeException e) {
            sendError(ex, 500, "処理中にエラーが起きました。");
        } finally {
            ex.close();
        }
    }

    private void result(HttpExchange ex, String path) throws IOException {
        Matcher m = RESULT_PATH.matcher(path);
        ResultStore.Stored s = m.matches() ? store.get(m.group(1)) : null;
        if (s == null) {
            sendError(ex, 404, "見つかりません。");
            return;
        }
        if (m.group(2) != null) {
            byte[] xlsx = XlsxExport.write(s.result(), s.nameA(), s.nameB(), s.f(), s.mode());
            ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"mfg-revision-diff.xlsx\"");
            send(ex, 200, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsx);
            return;
        }
        String page = Pages.result(m.group(1), s, filterOf(ex.getRequestURI().getRawQuery()));
        send(ex, 200, "text/html; charset=utf-8", page.getBytes(StandardCharsets.UTF_8));
    }

    private static Report.Filter filterOf(String rawQuery) {
        if (rawQuery != null) {
            for (String kv : rawQuery.split("&")) {
                if (kv.startsWith("filter=")) {
                    try {
                        return Report.Filter.parse(kv.substring("filter=".length()));
                    } catch (IllegalArgumentException e) {
                        return Report.Filter.ALL;
                    }
                }
            }
        }
        return Report.Filter.ALL;
    }

    // ---------------------------------------------------------------- upload

    private static final class TooLarge extends IOException {
        TooLarge() {
            super("request body larger than " + MAX_BODY + " bytes");
        }
    }

    /**
     * Reads the body, never more than MAX_BODY bytes into memory. A larger body is read and thrown away
     * up to twice the limit, so that the browser can still receive the answer, and then refused.
     */
    private static byte[] readBody(HttpExchange ex) throws IOException {
        String cl = ex.getRequestHeaders().getFirst("Content-Length");
        long declared = -1;
        if (cl != null) {
            try {
                declared = Long.parseLong(cl.trim());
            } catch (NumberFormatException e) {
                throw new IOException("bad Content-Length");
            }
        }
        InputStream in = ex.getRequestBody();
        if (declared > MAX_BODY) {
            drain(in, 0);
            throw new TooLarge();
        }
        byte[] data = in.readNBytes(MAX_BODY + 1);
        if (data.length > MAX_BODY) {
            drain(in, data.length);
            throw new TooLarge();
        }
        return data;
    }

    private static void drain(InputStream in, long already) throws IOException {
        byte[] buf = new byte[65536];
        long total = already;
        int n;
        while (total < 2L * MAX_BODY && (n = in.read(buf)) >= 0) {
            total += n;
        }
    }

    private void compare(HttpExchange ex) throws IOException {
        if (!guard.sourceAllowed(ex.getRequestHeaders())) {
            // The body is not read: the request is not from a page of this server.
            sendError(ex, 403, "この送信元からは受け付けません。");
            return;
        }
        byte[] body;
        try {
            body = readBody(ex);
        } catch (TooLarge e) {
            sendError(ex, 413, "送信が大きすぎます (上限 " + (MAX_BODY / (1024 * 1024)) + " MiB)。");
            return;
        } catch (IOException e) {
            sendError(ex, 400, "送信を読めません。");
            return;
        }
        List<Multipart.Part> parts;
        try {
            parts = Multipart.parse(ex.getRequestHeaders().getFirst("Content-Type"), body);
        } catch (IOException e) {
            sendError(ex, 400, "送信の形式が正しくありません。");
            return;
        }
        String nameA;
        String nameB;
        byte[] dataA;
        byte[] dataB;
        Multipart.Part sample = Multipart.find(parts, "sample");
        if (sample != null && new String(sample.data(), StandardCharsets.UTF_8).equals("1")) {
            nameA = "examples/revA.xml";
            nameB = "examples/revB.xml";
            dataA = resource("/examples/revA.xml");
            dataB = resource("/examples/revB.xml");
        } else {
            Multipart.Part pa = Multipart.find(parts, "fileA");
            Multipart.Part pb = Multipart.find(parts, "fileB");
            if (pa == null || pb == null || pa.data().length == 0 || pb.data().length == 0) {
                sendError(ex, 400, "版 A と版 B のファイルを選んでください。");
                return;
            }
            nameA = shortName(pa.filename());
            nameB = shortName(pb.filename());
            dataA = pa.data();
            dataB = pb.data();
        }
        Definition da;
        Definition db;
        try {
            da = B2mmlReader.fromDocument(SecureXml.parse(dataA));
            db = B2mmlReader.fromDocument(SecureXml.parse(dataB));
        } catch (IOException | org.xml.sax.SAXException | RuntimeException e) {
            sendError(ex, 400, "B2MML の XML として読めません: " + e.getMessage());
            return;
        }
        NodeMatcher matcher = new NodeMatcher();
        DiffResult r = Differ.diff(da, db, matcher);
        String id = store.put(new ResultStore.Stored(r, nameA, nameB, matcher.f(), matcher.mode()));
        ex.getResponseHeaders().add("Location", "/result/" + id);
        send(ex, 303, "text/plain; charset=utf-8", new byte[0]);
    }

    private static String shortName(String name) {
        if (name == null || name.isBlank()) {
            return "(名前なし)";
        }
        String n = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        return n.length() > 120 ? n.substring(0, 120) : n;
    }

    private static byte[] resource(String name) throws IOException {
        try (InputStream in = Server.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("missing resource " + name);
            }
            return in.readAllBytes();
        }
    }

    // ---------------------------------------------------------------- responses

    private static void sendError(HttpExchange ex, int status, String message) throws IOException {
        send(ex, status, "text/html; charset=utf-8", Pages.error(status, message).getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        var h = ex.getResponseHeaders();
        h.set("Content-Type", type);
        h.set("Content-Security-Policy", CSP);
        h.set("X-Content-Type-Options", "nosniff");
        h.set("Cache-Control", "no-store");
        if (body.length == 0) {
            ex.sendResponseHeaders(status, -1);
            return;
        }
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
