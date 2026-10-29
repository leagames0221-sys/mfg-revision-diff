package dev.mfgdiff.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A small reader for {@code multipart/form-data} (RFC 7578) bodies that are already in memory and already
 * limited in size. Each part has a field name, an optional file name and its bytes. A body that does not
 * follow the format is rejected with an {@link IOException}; nothing is guessed.
 */
final class Multipart {
    /** Most parts accepted in one body (author's decision, docs/sources.md D19). */
    static final int MAX_PARTS = 8;
    /** Longest header block of one part. */
    private static final int MAX_HEADER = 4096;

    record Part(String name, String filename, byte[] data) {
    }

    private static final Pattern BOUNDARY = Pattern.compile("boundary=(?:\"([^\"]{1,70})\"|([^;\\s\"]{1,70}))", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME = Pattern.compile("(?:^|;)\\s*name=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILENAME = Pattern.compile("(?:^|;)\\s*filename=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);

    private Multipart() {
    }

    static List<Part> parse(String contentType, byte[] body) throws IOException {
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("multipart/form-data")) {
            throw new IOException("not multipart/form-data");
        }
        Matcher bm = BOUNDARY.matcher(contentType);
        if (!bm.find()) {
            throw new IOException("no boundary");
        }
        String boundary = bm.group(1) != null ? bm.group(1) : bm.group(2);
        byte[] dash = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] delim = ("\r\n--" + boundary).getBytes(StandardCharsets.ISO_8859_1);

        // The body starts with the first delimiter (a preamble is allowed by RFC 2046 and skipped).
        int p = indexOf(body, dash, 0);
        if (p < 0) {
            throw new IOException("no first delimiter");
        }
        p += dash.length;
        List<Part> parts = new ArrayList<>();
        while (true) {
            if (startsWith(body, p, "--")) {
                return parts; // close delimiter
            }
            if (!startsWith(body, p, "\r\n")) {
                throw new IOException("bad delimiter line");
            }
            p += 2;
            int hEnd = indexOf(body, new byte[] {'\r', '\n', '\r', '\n'}, p);
            if (hEnd < 0 || hEnd - p > MAX_HEADER) {
                throw new IOException("bad part header");
            }
            String headers = new String(body, p, hEnd - p, StandardCharsets.UTF_8);
            int start = hEnd + 4;
            int end = indexOf(body, delim, start);
            if (end < 0) {
                throw new IOException("part is not closed");
            }
            String name = null;
            String filename = null;
            for (String line : headers.split("\r\n")) {
                int colon = line.indexOf(':');
                if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Disposition")) {
                    String v = line.substring(colon + 1).trim();
                    if (!v.toLowerCase(java.util.Locale.ROOT).startsWith("form-data")) {
                        throw new IOException("not form-data");
                    }
                    Matcher n = NAME.matcher(v);
                    if (n.find()) {
                        name = n.group(1);
                    }
                    Matcher f = FILENAME.matcher(v);
                    if (f.find()) {
                        filename = f.group(1);
                    }
                }
            }
            if (name == null) {
                throw new IOException("part without a name");
            }
            if (parts.size() >= MAX_PARTS) {
                throw new IOException("more than " + MAX_PARTS + " parts");
            }
            parts.add(new Part(name, filename, Arrays.copyOfRange(body, start, end)));
            p = end + delim.length;
        }
    }

    static Part find(List<Part> parts, String name) {
        for (Part p : parts) {
            if (p.name().equals(name)) {
                return p;
            }
        }
        return null;
    }

    private static boolean startsWith(byte[] a, int at, String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        if (at + b.length > a.length) {
            return false;
        }
        for (int i = 0; i < b.length; i++) {
            if (a[at + i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] a, byte[] pat, int from) {
        outer:
        for (int i = from; i <= a.length - pat.length; i++) {
            for (int j = 0; j < pat.length; j++) {
                if (a[i + j] != pat[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
