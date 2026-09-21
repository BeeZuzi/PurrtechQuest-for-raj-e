package eu.purrtech.purrtechQuest.util;

import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lowercase small-caps ("tiny font") text, the same alphabet lingojam's Tiny Text Generator produces:
 * {@code s} and {@code x} have no small-caps form and stay as they are, {@code q} is {@code ǫ}, {@code f} is
 * {@code ғ}, and accented letters (á, č, ř, …) have no small-caps form either, so they stay ordinary lowercase.
 * <p>
 * Applied when text is shown to a player, never to what is stored, so an admin's original wording stays
 * readable (and sortable) in the editor and the quest files. Converting is idempotent — text that is already
 * tiny (e.g. pasted from the generator) comes out unchanged.
 */
public final class TinyFont {

    private static final Map<Character, Character> MAP = Map.ofEntries(
            Map.entry('a', 'ᴀ'), Map.entry('b', 'ʙ'), Map.entry('c', 'ᴄ'), Map.entry('d', 'ᴅ'),
            Map.entry('e', 'ᴇ'), Map.entry('f', 'ғ'), Map.entry('g', 'ɢ'), Map.entry('h', 'ʜ'),
            Map.entry('i', 'ɪ'), Map.entry('j', 'ᴊ'), Map.entry('k', 'ᴋ'), Map.entry('l', 'ʟ'),
            Map.entry('m', 'ᴍ'), Map.entry('n', 'ɴ'), Map.entry('o', 'ᴏ'), Map.entry('p', 'ᴘ'),
            Map.entry('q', 'ǫ'), Map.entry('r', 'ʀ'), Map.entry('t', 'ᴛ'), Map.entry('u', 'ᴜ'),
            Map.entry('v', 'ᴠ'), Map.entry('w', 'ᴡ'), Map.entry('y', 'ʏ'), Map.entry('z', 'ᴢ'));

    /** What must survive untouched inside a raw string: MiniMessage tags, %placeholders% and &-color codes. */
    private static final Pattern PROTECTED = Pattern.compile(
            "</?[#!?a-zA-Z][^<>\\s]*>|%[^%\\s]*%|&#[0-9a-fA-F]{6}|&[0-9a-fk-orA-FK-OR]");

    private static final Pattern ANY_TEXT = Pattern.compile("(?s).+");

    private TinyFont() {
    }

    /**
     * Converts a raw string that may still contain MiniMessage tags, {@code %placeholders%} or {@code &}
     * color codes — those are left alone so they keep working, only the text between them is converted.
     */
    public static String convert(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        Matcher matcher = PROTECTED.matcher(raw);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            out.append(plain(raw.substring(last, matcher.start()))).append(matcher.group());
            last = matcher.end();
        }
        return out.append(plain(raw.substring(last))).toString();
    }

    /** Converts every piece of text in an already-parsed component, keeping its colors and formatting. */
    public static Component convert(Component component) {
        return component.replaceText(builder -> builder
                .match(ANY_TEXT)
                .replacement((match, text) -> text.content(plain(match.group()))));
    }

    private static String plain(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (char c : text.toLowerCase().toCharArray()) {
            out.append(MAP.getOrDefault(c, c));
        }
        return out.toString();
    }
}
