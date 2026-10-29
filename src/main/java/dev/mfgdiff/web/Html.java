package dev.mfgdiff.web;

/**
 * Output encoding for HTML. Every piece of text that comes from an input file goes through {@link #esc}
 * before it is put into the page; it is only ever placed in element text, never in a script, a style, a
 * URL or an attribute name (docs/adr/0005-screen-and-upload-protection.md, OWASP XSS Prevention Cheat
 * Sheet, "HTML entity encoding", docs/sources.md S11.1).
 */
final class Html {
    private Html() {
    }

    /** &amp; &lt; &gt; &quot; and &#x27; for the characters &amp; &lt; &gt; " and '. */
    static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#x27;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
