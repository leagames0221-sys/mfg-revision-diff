package dev.mfgdiff.web;

import static dev.mfgdiff.web.Html.esc;

import dev.mfgdiff.DiffResult;
import dev.mfgdiff.DiffResult.Status;
import dev.mfgdiff.Report.Filter;
import dev.mfgdiff.RowModel;
import dev.mfgdiff.RowModel.Row;
import java.util.List;
import java.util.Locale;

/**
 * HTML for the three screens. Text from the input files appears only as element text and only through
 * {@link Html#esc}; attributes hold fixed words and the server's own ids. No inline script or style: the
 * page loads /app.css and /app.js.
 */
final class Pages {
    private Pages() {
    }

    private static String head(String title) {
        return "<!doctype html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + esc(title) + "</title>\n<link rel=\"stylesheet\" href=\"/app.css\">\n</head>\n<body>\n";
    }

    private static final String TAIL = "</body>\n</html>\n";

    static String index() {
        return head("版の比較")
                + "<main class=\"narrow\">\n<h1>工程表と部品表の版の比較</h1>\n"
                + "<form method=\"post\" action=\"/compare\" enctype=\"multipart/form-data\" class=\"pick\">\n"
                + "<label>版 A <input type=\"file\" name=\"fileA\" accept=\".xml,application/xml,text/xml\" required></label>\n"
                + "<label>版 B <input type=\"file\" name=\"fileB\" accept=\".xml,application/xml,text/xml\" required></label>\n"
                + "<button type=\"submit\">比較する</button>\n</form>\n"
                + "<form method=\"post\" action=\"/compare\" enctype=\"multipart/form-data\" class=\"sample\">\n"
                + "<input type=\"hidden\" name=\"sample\" value=\"1\">\n"
                + "<button type=\"submit\">見本で試す</button>\n</form>\n</main>\n"
                + TAIL;
    }

    static String error(int status, String message) {
        return head("エラー") + "<main class=\"narrow\">\n<h1>エラー " + status + "</h1>\n<p class=\"err\">" + esc(message)
                + "</p>\n<p><a href=\"/\">最初に戻る</a></p>\n</main>\n" + TAIL;
    }

    private static final Object[][] FILTERS = {
        {Filter.ALL, "すべて"}, {Filter.DIFFERENCES, "差異あり"}, {Filter.ADDED, "追加"}, {Filter.CHANGES, "変更"},
        {Filter.REMOVED, "削除"}, {Filter.UNCHANGED, "変更なし"}, {Filter.MOVED, "入れ替え"},
    };

    static String filterParam(Filter f) {
        return f.name().toLowerCase(Locale.ROOT);
    }

    static String result(String id, ResultStore.Stored s, Filter filter) {
        DiffResult r = s.result();
        List<Row> steps = RowModel.segmentRows(r);
        List<Row> parts = RowModel.itemRows(r);
        List<Row> shownSteps = RowModel.filter(steps, filter);
        List<Row> shownParts = RowModel.filter(parts, filter);
        long diffs = shownSteps.stream().filter(Row::isDifference).count() + shownParts.stream().filter(Row::isDifference).count();

        StringBuilder sb = new StringBuilder(head("版の比較 結果"));
        sb.append("<header class=\"bar\">\n<h1>版の比較</h1>\n");
        sb.append("<p class=\"files\"><span>A: <b>").append(esc(s.nameA())).append("</b> (").append(r.a().segments.size())
                .append(" 工程 / ").append(r.a().itemCount()).append(" 部品行)</span> <span>B: <b>").append(esc(s.nameB()))
                .append("</b> (").append(r.b().segments.size()).append(" 工程 / ").append(r.b().itemCount())
                .append(" 部品行)</span></p>\n");
        sb.append("<p class=\"legend\"><span class=\"sym\">=</span> 同じ　<span class=\"sym\">≠</span> 変更　"
                + "<span class=\"sym\">A</span> 削除 (A のみ)　<span class=\"sym\">B</span> 追加 (B のみ)　"
                + "<span class=\"sym\">↕</span> 入れ替え</p>\n");
        sb.append("<nav class=\"filters\" aria-label=\"絞り込み\">");
        for (Object[] f : FILTERS) {
            Filter ff = (Filter) f[0];
            long n = RowModel.filter(steps, ff).size() + RowModel.filter(parts, ff).size();
            sb.append("<a href=\"/result/").append(esc(id)).append("?filter=").append(filterParam(ff)).append('"');
            if (ff == filter) {
                sb.append(" class=\"on\" aria-current=\"page\"");
            }
            sb.append('>').append((String) f[1]).append(" (").append(n).append(")</a>");
        }
        sb.append("</nav>\n");
        sb.append("<div class=\"tools\"><button type=\"button\" id=\"prev\" disabled>前の差異</button> "
                + "<span id=\"pos\" aria-live=\"polite\">0 / ").append(diffs).append("</span> "
                + "<button type=\"button\" id=\"next\" disabled>次の差異</button>　"
                + "<a class=\"btn\" href=\"/result/").append(esc(id)).append("/export.xlsx\">Excel に書き出す</a>　"
                + "<a href=\"/\">別の版を比べる</a></div>\n</header>\n<main>\n");

        sb.append("<section>\n<h2>工程 (").append(shownSteps.size()).append(")</h2>\n");
        sb.append("<table id=\"steps\">\n<thead><tr><th>区分</th><th>A#</th><th>B#</th><th>A の ID</th><th>B の ID</th>"
                + "<th>名前</th><th>何が変わったか (旧 → 新)</th></tr></thead>\n<tbody>\n");
        if (shownSteps.isEmpty()) {
            sb.append("<tr><td colspan=\"7\" class=\"none\">該当なし</td></tr>\n");
        }
        for (Row row : shownSteps) {
            rowHtml(sb, row, false);
        }
        sb.append("</tbody>\n</table>\n</section>\n");

        sb.append("<section>\n<h2>部品 (").append(shownParts.size()).append(")</h2>\n");
        sb.append("<table id=\"parts\">\n<thead><tr><th>区分</th><th>工程</th><th>A#</th><th>B#</th><th>A の ID</th><th>B の ID</th>"
                + "<th>部品</th><th>名前</th><th>何が変わったか (旧 → 新)</th></tr></thead>\n<tbody>\n");
        if (shownParts.isEmpty()) {
            sb.append("<tr><td colspan=\"9\" class=\"none\">該当なし</td></tr>\n");
        }
        for (Row row : shownParts) {
            rowHtml(sb, row, true);
        }
        sb.append("</tbody>\n</table>\n</section>\n</main>\n<script src=\"/app.js\"></script>\n");
        sb.append(TAIL);
        return sb.toString();
    }

    private static String statusClass(Status s) {
        return switch (s) {
            case SAME -> "st-same";
            case CHANGED -> "st-changed";
            case ADDED -> "st-added";
            case REMOVED -> "st-removed";
        };
    }

    private static void cell(StringBuilder sb, String cls, String text) {
        sb.append("<td");
        if (cls != null) {
            sb.append(" class=\"").append(cls).append('"');
        }
        sb.append('>').append(esc(text)).append("</td>");
    }

    private static String num(int n) {
        return n > 0 ? Integer.toString(n) : "";
    }

    private static void rowHtml(StringBuilder sb, Row r, boolean item) {
        sb.append("<tr class=\"").append(statusClass(r.status())).append(r.moved() ? " moved" : "").append('"');
        if (r.isDifference()) {
            sb.append(" data-diff=\"1\"");
        }
        sb.append('>');
        sb.append("<td class=\"kind\">").append(esc(r.symbol())).append(r.moved() ? " <span class=\"mv\">↕</span>" : "").append("</td>");
        if (item) {
            cell(sb, null, r.segment());
        }
        cell(sb, "num", num(r.a()));
        cell(sb, "num", num(r.b()));
        String idCls = r.changed("ID") ? "chg" : null;
        cell(sb, idCls, r.idA());
        cell(sb, idCls, r.idB());
        if (item) {
            cell(sb, null, r.part());
        }
        cell(sb, r.changed("Description") ? "chg" : null, r.name());
        cell(sb, r.changes().isEmpty() ? null : "chg", RowModel.describe(r.changes()));
        sb.append("</tr>\n");
    }
}
