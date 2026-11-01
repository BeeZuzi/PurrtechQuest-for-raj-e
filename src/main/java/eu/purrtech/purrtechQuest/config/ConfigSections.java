package eu.purrtech.purrtechQuest.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A server's {@code config.yml} is written once and never touched again, so an option added in a later
 * plugin version simply isn't in it. This appends such a section — with its explanatory comments, exactly as
 * the bundled {@code config.yml} has it — to the end of the on-disk file, leaving everything already in the
 * file byte for byte as it was. Text rather than YAML on purpose: loading and re-saving the file would
 * reformat the admin's own comments and layout.
 */
public final class ConfigSections {

    private ConfigSections() {
    }

    /**
     * Appends the top-level section {@code key} from {@code bundledText} (a full config.yml) to the file
     * {@code configFile} unless that file already has a {@code key:} line. Returns whether it appended.
     */
    public static boolean appendIfMissing(Path configFile, String bundledText, String key) throws IOException {
        String current = Files.readString(configFile, StandardCharsets.UTF_8);
        if (hasKey(current, key)) {
            return false;
        }
        String block = extract(bundledText, key);
        if (block == null) {
            return false;
        }
        String separator = current.isEmpty() || current.endsWith("\n") ? "\n" : "\n\n";
        Files.writeString(configFile, current + separator + block, StandardCharsets.UTF_8);
        return true;
    }

    private static boolean hasKey(String text, String key) {
        for (String line : text.split("\\R")) {
            if (line.startsWith(key + ":")) {
                return true;
            }
        }
        return false;
    }

    /**
     * The section {@code key} with the comment lines directly above it and every following indented (or
     * comment-only) line, up to the first blank line; {@code null} if the text has no such section.
     */
    static String extract(String bundledText, String key) {
        List<String> lines = List.of(bundledText.split("\\R", -1));
        int start = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(key + ":")) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return null;
        }
        int from = start;
        while (from > 0 && lines.get(from - 1).startsWith("#")) {
            from--;
        }
        int to = start + 1;
        while (to < lines.size() && !lines.get(to).isBlank()) {
            to++;
        }
        List<String> block = new ArrayList<>(lines.subList(from, to));
        return String.join("\n", block) + "\n";
    }
}
