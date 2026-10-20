package com.levantare.pricing.doc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal docx template engine on top of Apache POI that keeps the template's formatting, header, footer and logo.
 * <ul>
 *   <li>{@code {{name}}} - replaced by a value; "\n" in a value becomes a line break.</li>
 *   <li>{@code {{list.field}}} - a table row containing such tokens is repeated once per list element.</li>
 *   <li>{@code {{?flag}}} - a table row containing the marker is dropped when the flag is false.</li>
 * </ul>
 */
public final class DocxTemplateEngine {

    private static final Logger log = LoggerFactory.getLogger(DocxTemplateEngine.class);
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([^{}]+)}}");
    private static final Pattern FLAG = Pattern.compile("\\{\\{\\?(\\w+)}}");
    private static final Pattern LIST_TOKEN = Pattern.compile("\\{\\{(\\w+)\\.(\\w+)}}");

    private DocxTemplateEngine() {}

    public static byte[] render(InputStream template, Map<String, String> values,
                                Map<String, List<Map<String, String>>> lists, Map<String, Boolean> flags)
            throws IOException {
        try (XWPFDocument doc = new XWPFDocument(template)) {
            processTables(doc.getTables(), lists, flags);
            forEachParagraph(doc, p -> replaceTokens(p, values));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    // ---- tables -------------------------------------------------------------------------------------------

    private static void processTables(List<XWPFTable> tables, Map<String, List<Map<String, String>>> lists,
                                      Map<String, Boolean> flags) {
        for (XWPFTable table : tables) {
            for (XWPFTableRow row : new ArrayList<>(table.getRows())) {
                String text = rowText(row);
                Matcher flag = FLAG.matcher(text);
                if (flag.find()) {
                    if (!flags.getOrDefault(flag.group(1), false)) {
                        table.removeRow(table.getRows().indexOf(row));
                        continue;
                    }
                }
                Matcher lt = LIST_TOKEN.matcher(text);
                if (lt.find() && lists.containsKey(lt.group(1))) {
                    expandRow(table, row, lt.group(1), lists.get(lt.group(1)));
                    continue;
                }
                for (XWPFTableCell cell : row.getTableCells()) {
                    processTables(cell.getTables(), lists, flags);
                }
            }
        }
    }

    private static void expandRow(XWPFTable table, XWPFTableRow proto, String list, List<Map<String, String>> items) {
        int pos = table.getRows().indexOf(proto);
        int offset = 1;
        for (Map<String, String> item : items) {
            Map<String, String> values = new HashMap<>();
            item.forEach((k, v) -> values.put(list + "." + k, v));
            XWPFTableRow copy = new XWPFTableRow((CTRow) proto.getCtRow().copy(), table);
            // fill first: addRow() copies the row xml into the table, later changes to `copy` would be lost
            for (XWPFTableCell cell : copy.getTableCells()) {
                cell.getParagraphs().forEach(p -> replaceTokens(p, values));
            }
            table.addRow(copy, pos + offset++);
        }
        table.removeRow(table.getRows().indexOf(proto));
    }

    private static String rowText(XWPFTableRow row) {
        StringBuilder sb = new StringBuilder();
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph p : cell.getParagraphs()) sb.append(paragraphText(p)).append(' ');
        }
        return sb.toString();
    }

    // ---- paragraphs ---------------------------------------------------------------------------------------

    private static void forEachParagraph(XWPFDocument doc, java.util.function.Consumer<XWPFParagraph> fn) {
        doc.getParagraphs().forEach(fn);
        doc.getTables().forEach(t -> forEachParagraph(t, fn));
        doc.getHeaderList().forEach(h -> {
            h.getParagraphs().forEach(fn);
            h.getTables().forEach(t -> forEachParagraph(t, fn));
        });
        doc.getFooterList().forEach(f -> {
            f.getParagraphs().forEach(fn);
            f.getTables().forEach(t -> forEachParagraph(t, fn));
        });
    }

    private static void forEachParagraph(XWPFTable table, java.util.function.Consumer<XWPFParagraph> fn) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                cell.getParagraphs().forEach(fn);
                cell.getTables().forEach(t -> forEachParagraph(t, fn));
            }
        }
    }

    private static String paragraphText(XWPFParagraph p) {
        StringBuilder sb = new StringBuilder();
        for (XWPFRun r : p.getRuns()) {
            String t = r.getText(0);
            if (t != null) sb.append(t);
        }
        return sb.toString();
    }

    private static void replaceTokens(XWPFParagraph p, Map<String, String> values) {
        if (!paragraphText(p).contains("{{")) return;
        for (XWPFRun run : p.getRuns()) {
            String t = run.getText(0);
            if (t != null && t.contains("{{")) {
                String replaced = apply(t, values);
                if (!replaced.equals(t)) setText(run, replaced);
            }
        }
        // token split over several runs: merge everything into the first run
        String rest = paragraphText(p);
        if (rest.contains("{{")) {
            List<XWPFRun> runs = p.getRuns();
            setText(runs.get(0), apply(rest, values));
            for (int i = 1; i < runs.size(); i++) setText(runs.get(i), "");
        }
    }

    private static String apply(String text, Map<String, String> values) {
        Matcher m = TOKEN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1).trim();
            String v = values.get(key);
            if (v == null) {
                if (!key.startsWith("?") && !key.contains(".")) log.warn("No value for template token {{{}}}", key);
                v = "";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(v));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static void setText(XWPFRun run, String text) {
        int n = run.getCTR().sizeOfTArray();
        for (int i = n - 1; i >= 0; i--) run.getCTR().removeT(i);
        String[] lines = text.split("\n", -1);
        run.setText(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            run.addBreak();
            run.setText(lines[i]);
        }
    }
}
