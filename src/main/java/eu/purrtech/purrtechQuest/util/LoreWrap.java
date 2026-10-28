package eu.purrtech.purrtechQuest.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * Breaks one long lore line into several, because item lore is never wrapped for us: an admin-typed quest
 * description or objective label would otherwise be a single line as wide as the text itself.
 * <p>
 * Works on the already-built {@link Component}, so every colored/styled part of the text keeps its own style
 * on whichever line it lands on. Lines break at spaces (a single word longer than a whole line is split in
 * the middle), and a {@code \n} in the text forces a break. The width is measured in characters — an
 * approximation, since the client draws letters at different widths — and is read live from the
 * {@code lore-wrap-width} setting in {@code config.yml} ({@link #widthWhen}); {@code 0} turns wrapping off.
 */
public final class LoreWrap {

    public static final int DEFAULT_WIDTH = 40;

    private static volatile IntSupplier width = () -> DEFAULT_WIDTH;

    private LoreWrap() {
    }

    public static void widthWhen(IntSupplier setting) {
        width = setting;
    }

    public static int width() {
        return width.getAsInt();
    }

    public static List<Component> wrap(Component line) {
        return wrap(line, "");
    }

    /**
     * {@code continuationIndent} is put in front of every line after the first, and counts towards the width,
     * so a wrapped bullet like {@code " - label"} can keep its text aligned under the first line's text.
     */
    public static List<Component> wrap(Component line, String continuationIndent) {
        int max = width();
        if (max <= 0) {
            return List.of(line);
        }

        List<Segment> segments = new ArrayList<>();
        flatten(line, Style.empty(), segments);
        StringBuilder plain = new StringBuilder();
        for (Segment segment : segments) {
            plain.append(segment.text());
        }
        if (plain.length() <= max && plain.indexOf("\n") < 0) {
            return List.of(line);
        }

        String text = plain.toString();
        int[] owner = new int[text.length()];
        int at = 0;
        for (int i = 0; i < segments.size(); i++) {
            for (int c = 0; c < segments.get(i).text().length(); c++) {
                owner[at++] = i;
            }
        }

        List<int[]> ranges = breakIntoLines(text, max, continuationIndent.length());
        List<Component> out = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) {
            TextComponent.Builder builder = Component.text();
            if (i > 0 && !continuationIndent.isEmpty()) {
                builder.append(Component.text(continuationIndent));
            }
            int start = ranges.get(i)[0];
            int end = ranges.get(i)[1];
            while (start < end) {
                int segment = owner[start];
                int stop = start;
                while (stop < end && owner[stop] == segment) {
                    stop++;
                }
                builder.append(Component.text(text.substring(start, stop), segments.get(segment).style()));
                start = stop;
            }
            out.add(builder.build());
        }
        return out;
    }

    private static List<int[]> breakIntoLines(String text, int max, int indentLength) {
        List<int[]> ranges = new ArrayList<>();
        int n = text.length();
        int pos = 0;
        boolean first = true;
        while (pos < n) {
            if (!first) {
                while (pos < n && text.charAt(pos) == ' ') {
                    pos++;
                }
                if (pos >= n) {
                    break;
                }
            }
            int capacity = Math.max(1, first ? max : max - indentLength);
            int limit = Math.min(n, pos + capacity);
            int newline = text.indexOf('\n', pos);
            int end;
            int next;
            if (newline >= 0 && newline <= limit) {
                end = newline;
                next = newline + 1;
            } else if (limit >= n) {
                end = n;
                next = n;
            } else if (text.charAt(limit) == ' ') {
                end = limit;
                next = limit;
            } else {
                int space = text.lastIndexOf(' ', limit - 1);
                end = space > pos ? space : limit;
                next = end;
            }
            int trimmed = end;
            while (trimmed > pos && text.charAt(trimmed - 1) == ' ') {
                trimmed--;
            }
            ranges.add(new int[]{pos, trimmed});
            pos = next;
            first = false;
        }
        return ranges;
    }

    private record Segment(String text, Style style) {
    }

    /** Flattens the tree into runs of text, each with the style it actually renders with (child over parent). */
    private static void flatten(Component component, Style inherited, List<Segment> out) {
        Style effective = inherited.merge(component.style());
        String text = component instanceof TextComponent t
                ? t.content()
                : PlainTextComponentSerializer.plainText().serialize(component.children(List.of()));
        if (!text.isEmpty()) {
            out.add(new Segment(text, effective));
        }
        for (Component child : component.children()) {
            flatten(child, effective, out);
        }
    }
}
