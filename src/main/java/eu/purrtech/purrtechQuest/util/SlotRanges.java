package eu.purrtech.purrtechQuest.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses a slot list from config such as {@code "9-36"} or {@code "10-16, 19-25, 28"} — comma-separated
 * single slots and inclusive ranges, in the order written. Slots outside {@code 0..size-1}, reserved slots,
 * duplicates and unparsable parts are skipped rather than failing, so a typo only loses that one part.
 */
public final class SlotRanges {

    private SlotRanges() {
    }

    public static List<Integer> parse(String spec, int size, Set<Integer> reserved) {
        Set<Integer> slots = new LinkedHashSet<>();
        if (spec == null) {
            return List.of();
        }
        for (String part : spec.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int from;
            int to;
            try {
                int dash = trimmed.indexOf('-');
                if (dash < 0) {
                    from = to = Integer.parseInt(trimmed);
                } else {
                    from = Integer.parseInt(trimmed.substring(0, dash).trim());
                    to = Integer.parseInt(trimmed.substring(dash + 1).trim());
                }
            } catch (NumberFormatException e) {
                continue;
            }
            int step = from <= to ? 1 : -1;
            for (int slot = from; slot != to + step; slot += step) {
                if (slot >= 0 && slot < size && !reserved.contains(slot)) {
                    slots.add(slot);
                }
            }
        }
        return new ArrayList<>(slots);
    }
}
