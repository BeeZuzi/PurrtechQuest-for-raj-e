package eu.purrtech.purrtechQuest.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;

/**
 * Turns a raw string an admin typed (a quest's {@code display-name}, a reward's {@code name}, ...) into the
 * actual {@link Component} shown to a player: legacy {@code &e}/{@code &#RRGGBB} codes and MiniMessage tags
 * both work (see {@link LegacyColors}), the result is shown in the tiny font ({@link TinyFont}), and
 * {@code colorIfAbsent} keeps text with no styling of its own looking exactly like {@code fallbackColor} —
 * so a quest author's own styling always wins, never a color forced on top of it. Falls back to plain text
 * on a malformed value (e.g. a stray {@code <} typed into the field) so a bad value degrades to ugly rather
 * than making whatever it's shown on (a GUI icon, a granted item) impossible to build at all.
 */
public final class DisplayText {

    private DisplayText() {
    }

    public static Component component(String raw, TextColor fallbackColor) {
        try {
            return TinyFont.convert(MiniMessage.miniMessage().deserialize(LegacyColors.toMiniMessage(raw))
                    .colorIfAbsent(fallbackColor));
        } catch (ParsingException e) {
            return TinyFont.convert(Component.text(raw, fallbackColor));
        }
    }
}
