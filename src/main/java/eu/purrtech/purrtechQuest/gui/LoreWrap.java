package eu.purrtech.purrtechQuest.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Word-wraps lore lines longer than a width in visible characters: a line breaks at the last space that fits,
 * a word is never cut (one longer than the width stays whole on its own line), colours/styles carry over to
 * the continuation, and the line's leading spaces ({@code " - "} bullets) are repeated as its indent.
 */
final class LoreWrap {

    private LoreWrap() {
    }

    private record Seg(String text, Style style) {
    }

    static List<Component> wrap(List<Component> lore, int width) {
        if (width <= 0) {
            return lore;
        }
        List<Component> out = new ArrayList<>();
        for (Component line : lore) {
            out.addAll(wrapLine(line, width));
        }
        return out;
    }

    static List<Component> wrapLine(Component line, int width) {
        List<Seg> segs = new ArrayList<>();
        flatten(line, Style.empty(), segs);
        StringBuilder sb = new StringBuilder();
        segs.forEach(s -> sb.append(s.text()));
        String plain = sb.toString();
        if (plain.length() <= width) {
            return List.of(line);
        }

        int indent = 0;
        while (indent < plain.length() && " -•*".indexOf(plain.charAt(indent)) >= 0) {
            indent++;
        }
        List<Component> lines = new ArrayList<>();
        int pos = indent;
        boolean first = true;
        while (pos < plain.length()) {
            int end = pos + Math.max(width - indent, 1); // the indent is repeated on every line
            if (end >= plain.length()) {
                end = plain.length();
            } else {
                int space = plain.lastIndexOf(' ', end);
                if (space <= pos) { // a single word wider than the line: keep it whole
                    space = plain.indexOf(' ', end);
                    if (space < 0) {
                        space = plain.length();
                    }
                }
                end = space;
            }
            lines.add(slice(segs, first, pos, end, indent));
            pos = end;
            while (pos < plain.length() && plain.charAt(pos) == ' ') {
                pos++;
            }
            first = false;
        }
        return lines;
    }

    /** The styled characters [from, to) of the flattened text; a continuation line starts with {@code indent} spaces, the first keeps its own prefix. */
    private static Component slice(List<Seg> segs, boolean first, int from, int to, int indent) {
        Component result = Component.empty();
        if (first) {
            from = 0; // first line: keep the original leading spaces too
        } else {
            result = result.append(Component.text(" ".repeat(indent), segs.get(0).style()));
        }
        int offset = 0;
        for (Seg seg : segs) {
            int a = Math.max(from, offset);
            int b = Math.min(to, offset + seg.text().length());
            if (a < b) {
                result = result.append(Component.text(seg.text().substring(a - offset, b - offset), seg.style()));
            }
            offset += seg.text().length();
        }
        return result;
    }

    private static void flatten(Component c, Style parent, List<Seg> out) {
        Style style = c.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        if (c instanceof TextComponent text && !text.content().isEmpty()) {
            out.add(new Seg(text.content(), style));
        }
        c.children().forEach(child -> flatten(child, style, out));
    }
}
