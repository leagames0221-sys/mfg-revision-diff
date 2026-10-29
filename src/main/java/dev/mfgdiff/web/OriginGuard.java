package dev.mfgdiff.web;

import com.sun.net.httpserver.Headers;
import java.net.URI;
import java.util.Set;

/**
 * Decides whether a request may reach the server. Two checks:
 * <ul>
 * <li>{@link #hostAllowed}: the Host header must name this server (127.0.0.1 or localhost with the port), so a
 * web page that reached the server through a changed DNS answer is refused (DNS rebinding,
 * docs/sources.md S11.4);</li>
 * <li>{@link #sourceAllowed}: a POST must come from a page of this server. The target origin is fixed in the
 * program, not read from the request. The Origin header is compared as a whole string; without it the
 * origin of the Referer is compared; with neither, or with {@code Origin: null}, the request is refused
 * (OWASP CSRF Prevention Cheat Sheet, "Using Standard Headers to Verify Origin", docs/sources.md S11.2).</li>
 * </ul>
 */
final class OriginGuard {
    private final Set<String> hosts;
    private final Set<String> origins;

    OriginGuard(int port) {
        this.hosts = Set.of("127.0.0.1:" + port, "localhost:" + port);
        this.origins = Set.of("http://127.0.0.1:" + port, "http://localhost:" + port);
    }

    boolean hostAllowed(Headers h) {
        String host = h.getFirst("Host");
        return host != null && hosts.contains(host);
    }

    boolean sourceAllowed(Headers h) {
        String site = h.getFirst("Sec-Fetch-Site");
        if (site != null && !site.equals("same-origin")) {
            return false;
        }
        String origin = h.getFirst("Origin");
        if (origin != null) {
            return origins.contains(origin);
        }
        String referer = h.getFirst("Referer");
        if (referer != null) {
            try {
                URI u = URI.create(referer);
                if (u.getScheme() == null || u.getHost() == null || u.getPort() < 0) {
                    return false;
                }
                return origins.contains(u.getScheme() + "://" + u.getHost() + ":" + u.getPort());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }
}
