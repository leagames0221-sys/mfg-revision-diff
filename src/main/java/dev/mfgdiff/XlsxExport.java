package dev.mfgdiff;

import dev.mfgdiff.DiffResult.Status;
import dev.mfgdiff.RowModel.Row;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Writes a comparison as an .xlsx file with three sheets: the summary (まとめ), the steps (工程) and the
 * bill rows (部品). Every cell that holds text from an input file is a string cell; no formula is ever
 * written (docs/adr/0004-excel-export-poi.md).
 */
public final class XlsxExport {
    public static final String SHEET_SUMMARY = "まとめ";
    public static final String SHEET_STEPS = "工程";
    public static final String SHEET_PARTS = "部品";

    /** Changed row: "lightyellow" (aras-bom-compare ChangedRowColor); changed cell: "yellow" (ChangedCellColor). */
    public static final String COLOR_CHANGED_ROW = "FFFFE0";
    public static final String COLOR_CHANGED_CELL = "FFFF00";
    /** Added / removed / moved rows (author's decision D18: pale green, pale pink, pale blue). */
    public static final String COLOR_ADDED_ROW = "E2F5E2";
    public static final String COLOR_REMOVED_ROW = "FBE3E8";
    public static final String COLOR_MOVED_ROW = "E3EEFB";
    private static final String COLOR_HEADER = "D9D9D9";

    /** An Excel cell holds at most 32767 characters. */
    private static final int MAX_CELL = 32767;

    private XlsxExport() {
    }

    /**
     * True when a spreadsheet may read the text as a formula if it is typed into the cell again: the
     * first character is one of the characters the OWASP CSV Injection page lists (docs/sources.md S11.5, S11.6).
     */
    static boolean startsLikeFormula(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        return "=+-@\t\r\n＝＋－＠".indexOf(s.charAt(0)) >= 0;
    }

    private static final class Styles {
        private final XSSFWorkbook wb;
        private final Map<String, XSSFCellStyle> cache = new HashMap<>();
        private final XSSFFont bold;

        Styles(XSSFWorkbook wb) {
            this.wb = wb;
            this.bold = wb.createFont();
            this.bold.setBold(true);
        }

        /** @param fill RGB hex or null; quotePrefix = Excel's own "text, not a formula" mark on the cell. */
        XSSFCellStyle get(String fill, boolean header, boolean quotePrefix) {
            String key = fill + "|" + header + "|" + quotePrefix;
            return cache.computeIfAbsent(key, k -> {
                XSSFCellStyle st = wb.createCellStyle();
                if (fill != null) {
                    st.setFillForegroundColor(new XSSFColor(rgb(fill), null));
                    st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
                if (header) {
                    st.setFont(bold);
                }
                st.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
                st.setQuotePrefixed(quotePrefix);
                return st;
            });
        }
    }

    private static byte[] rgb(String hex) {
        return new byte[] {(byte) Integer.parseInt(hex.substring(0, 2), 16), (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)};
    }

    /** Always a string cell, whatever the text is. */
    private static void text(Cell cell, String value, Styles st, String fill, boolean header) {
        String v = value == null ? "" : value;
        if (v.length() > MAX_CELL) {
            v = v.substring(0, MAX_CELL);
        }
        cell.setCellValue(v);
        cell.setCellStyle(st.get(fill, header, startsLikeFormula(v)));
    }

    private static void number(Cell cell, int value, Styles st, String fill) {
        if (value > 0) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(st.get(fill, false, false));
    }

    static String statusWord(Status s) {
        return switch (s) {
            case SAME -> "同じ";
            case CHANGED -> "変更";
            case ADDED -> "追加";
            case REMOVED -> "削除";
        };
    }

    static String rowFill(Row r) {
        if (r.status() == Status.CHANGED) {
            return COLOR_CHANGED_ROW;
        }
        if (r.status() == Status.ADDED) {
            return COLOR_ADDED_ROW;
        }
        if (r.status() == Status.REMOVED) {
            return COLOR_REMOVED_ROW;
        }
        return r.moved() ? COLOR_MOVED_ROW : null;
    }

    public static byte[] write(DiffResult r, String nameA, String nameB, double f, RatioMode mode) throws IOException {
        List<Row> steps = RowModel.segmentRows(r);
        List<Row> parts = RowModel.itemRows(r);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles st = new Styles(wb);
            summary(wb.createSheet(SHEET_SUMMARY), st, r, nameA, nameB, f, mode, steps, parts);
            stepsSheet(wb.createSheet(SHEET_STEPS), st, steps);
            partsSheet(wb.createSheet(SHEET_PARTS), st, parts);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    private static void header(Sheet sh, Styles st, String... names) {
        org.apache.poi.ss.usermodel.Row h = sh.createRow(0);
        for (int c = 0; c < names.length; c++) {
            text(h.createCell(c), names[c], st, COLOR_HEADER, true);
        }
        // Freeze the header row and switch on the filter arrows (standard Excel features, docs/sources.md S11.8).
        sh.createFreezePane(0, 1);
        sh.setAutoFilter(new CellRangeAddress(0, 0, 0, names.length - 1));
    }

    private static void stepsSheet(Sheet sh, Styles st, List<Row> rows) {
        header(sh, st, "区分", "入れ替え", "A#", "B#", "A の ID", "B の ID", "名前", "何が変わったか (旧 → 新)");
        int[] widths = {8, 10, 6, 6, 14, 14, 36, 70};
        for (int c = 0; c < widths.length; c++) {
            sh.setColumnWidth(c, widths[c] * 256);
        }
        int i = 1;
        for (Row r : rows) {
            org.apache.poi.ss.usermodel.Row x = sh.createRow(i++);
            String fill = rowFill(r);
            text(x.createCell(0), statusWord(r.status()), st, fill, false);
            text(x.createCell(1), r.moved() ? "↕" : "", st, fill, false);
            number(x.createCell(2), r.a(), st, fill);
            number(x.createCell(3), r.b(), st, fill);
            boolean idChanged = r.changed("ID");
            text(x.createCell(4), r.idA(), st, idChanged ? COLOR_CHANGED_CELL : fill, false);
            text(x.createCell(5), r.idB(), st, idChanged ? COLOR_CHANGED_CELL : fill, false);
            text(x.createCell(6), r.name(), st, r.changed("Description") ? COLOR_CHANGED_CELL : fill, false);
            text(x.createCell(7), RowModel.describe(r.changes()), st, r.changes().isEmpty() ? fill : COLOR_CHANGED_CELL, false);
        }
    }

    private static void partsSheet(Sheet sh, Styles st, List<Row> rows) {
        header(sh, st, "区分", "工程", "A#", "B#", "A の ID", "B の ID", "部品", "名前", "何が変わったか (旧 → 新)");
        int[] widths = {8, 12, 6, 6, 16, 16, 14, 36, 70};
        for (int c = 0; c < widths.length; c++) {
            sh.setColumnWidth(c, widths[c] * 256);
        }
        int i = 1;
        for (Row r : rows) {
            org.apache.poi.ss.usermodel.Row x = sh.createRow(i++);
            String fill = rowFill(r);
            text(x.createCell(0), statusWord(r.status()), st, fill, false);
            text(x.createCell(1), r.segment(), st, fill, false);
            number(x.createCell(2), r.a(), st, fill);
            number(x.createCell(3), r.b(), st, fill);
            boolean idChanged = r.changed("ID");
            text(x.createCell(4), r.idA(), st, idChanged ? COLOR_CHANGED_CELL : fill, false);
            text(x.createCell(5), r.idB(), st, idChanged ? COLOR_CHANGED_CELL : fill, false);
            text(x.createCell(6), r.part(), st, fill, false);
            text(x.createCell(7), r.name(), st, r.changed("Description") ? COLOR_CHANGED_CELL : fill, false);
            text(x.createCell(8), RowModel.describe(r.changes()), st, r.changes().isEmpty() ? fill : COLOR_CHANGED_CELL, false);
        }
    }

    private static long count(List<Row> rows, Status s) {
        return rows.stream().filter(x -> x.status() == s).count();
    }

    private static void summary(Sheet sh, Styles st, DiffResult r, String nameA, String nameB, double f, RatioMode mode,
            List<Row> steps, List<Row> parts) {
        sh.setColumnWidth(0, 18 * 256);
        sh.setColumnWidth(1, 60 * 256);
        sh.setColumnWidth(2, 14 * 256);
        int i = 0;
        text(sh.createRow(i++).createCell(0), "版の比較", st, null, true);
        i++;
        org.apache.poi.ss.usermodel.Row a = sh.createRow(i++);
        text(a.createCell(0), "版 A", st, null, true);
        text(a.createCell(1), nameA, st, null, false);
        text(a.createCell(2), r.a().segments.size() + " 工程 / " + r.a().itemCount() + " 部品行", st, null, false);
        org.apache.poi.ss.usermodel.Row b = sh.createRow(i++);
        text(b.createCell(0), "版 B", st, null, true);
        text(b.createCell(1), nameB, st, null, false);
        text(b.createCell(2), r.b().segments.size() + " 工程 / " + r.b().itemCount() + " 部品行", st, null, false);
        org.apache.poi.ss.usermodel.Row m = sh.createRow(i++);
        text(m.createCell(0), "突き合わせ", st, null, true);
        text(m.createCell(1), "ratio " + mode.name().toLowerCase(java.util.Locale.ROOT) + ", F " + f, st, null, false);
        i++;
        org.apache.poi.ss.usermodel.Row h = sh.createRow(i++);
        text(h.createCell(0), "区分", st, COLOR_HEADER, true);
        text(h.createCell(1), "記号", st, COLOR_HEADER, true);
        text(h.createCell(2), "工程", st, COLOR_HEADER, true);
        text(h.createCell(3), "部品行", st, COLOR_HEADER, true);
        sh.setColumnWidth(3, 12 * 256);
        Object[][] lines = {
            {"同じ", "=", count(steps, Status.SAME), count(parts, Status.SAME), null},
            {"変更", "≠", count(steps, Status.CHANGED), count(parts, Status.CHANGED), COLOR_CHANGED_ROW},
            {"削除 (A のみ)", "A", count(steps, Status.REMOVED), count(parts, Status.REMOVED), COLOR_REMOVED_ROW},
            {"追加 (B のみ)", "B", count(steps, Status.ADDED), count(parts, Status.ADDED), COLOR_ADDED_ROW},
            {"入れ替え", "↕", steps.stream().filter(Row::moved).count(), 0L, COLOR_MOVED_ROW},
        };
        for (Object[] l : lines) {
            org.apache.poi.ss.usermodel.Row x = sh.createRow(i++);
            String fill = (String) l[4];
            text(x.createCell(0), (String) l[0], st, fill, false);
            text(x.createCell(1), (String) l[1], st, fill, false);
            Cell c2 = x.createCell(2);
            c2.setCellValue(((Long) l[2]).doubleValue());
            c2.setCellStyle(st.get(fill, false, false));
            Cell c3 = x.createCell(3);
            c3.setCellValue(((Long) l[3]).doubleValue());
            c3.setCellStyle(st.get(fill, false, false));
        }
        text(sh.createRow(i++).createCell(0), "入れ替えは「同じ」「変更」と重なる (順番だけが違う工程)。", st, null, false);
        i++;
        text(sh.createRow(i++).createCell(0), "色: 薄い黄色の行 = 変更あり、黄色のセル = 変わった欄、薄い緑 = 追加、薄い赤 = 削除、薄い青 = 入れ替えのみ。", st, null, false);
        if (!r.a().warnings.isEmpty() || !r.b().warnings.isEmpty()) {
            i++;
            text(sh.createRow(i++).createCell(0), "比べていない要素 (ここに違いがあっても工程・部品のシートには出ません)", st, null, true);
            for (String w : r.a().warnings) {
                org.apache.poi.ss.usermodel.Row x = sh.createRow(i++);
                text(x.createCell(0), "版 A", st, null, false);
                text(x.createCell(1), w.replaceFirst("^not compared: ", ""), st, null, false);
            }
            for (String w : r.b().warnings) {
                org.apache.poi.ss.usermodel.Row x = sh.createRow(i++);
                text(x.createCell(0), "版 B", st, null, false);
                text(x.createCell(1), w.replaceFirst("^not compared: ", ""), st, null, false);
            }
        }
    }
}
