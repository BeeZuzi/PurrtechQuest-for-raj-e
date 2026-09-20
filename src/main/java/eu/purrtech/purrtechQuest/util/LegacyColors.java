package eu.purrtech.purrtechQuest.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the {@code &}-style codes admins are used to ({@code &e}, {@code &l}, {@code &#FFD700}) into
 * MiniMessage tags, so free text typed into the quest editor (reward names, quest names) can be colored
 * without learning MiniMessage. Like legacy codes, a color code resets any formatting before it, so
 * {@code &l&cText} comes out red and not bold, exactly as it would in-game. A {@code &} that isn't followed
 * by a valid code is left alone ("Kováři &amp; spol.").
 */
public final class LegacyColors {

    private static final Pattern CODE = Pattern.compile("&(#[0-9a-fA-F]{6}|[0-9a-fA-Fk-oK-OrR])");

    private static final Map<Character, String> TAGS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"),
            Map.entry('k', "obfuscated"), Map.entry('l', "bold"), Map.entry('m', "strikethrough"),
            Map.entry('n', "underlined"), Map.entry('o', "italic"), Map.entry('r', "reset"));

    private LegacyColors() {
    }

    public static String toMiniMessage(String text) {
        if (text == null || text.indexOf('&') < 0) {
            return text;
        }
        Matcher matcher = CODE.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String code = matcher.group(1);
            String replacement;
            if (code.charAt(0) == '#') {
                replacement = "<reset><" + code + ">";
            } else {
                char c = Character.toLowerCase(code.charAt(0));
                boolean isColor = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
                replacement = (isColor ? "<reset>" : "") + "<" + TAGS.get(c) + ">";
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
