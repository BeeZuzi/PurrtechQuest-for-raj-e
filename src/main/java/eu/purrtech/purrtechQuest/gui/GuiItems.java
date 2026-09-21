package eu.purrtech.purrtechQuest.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Small builder for GUI icons. Bukkit item display names/lore render with an implicit italic + white
 * default that looks off for our purposes, so every name/lore line is forced non-italic here.
 */
public final class GuiItems {

    private GuiItems() {
    }

    public static ItemStack icon(Material material, Component name, List<Component> lore) {
        return icon(new ItemStack(material), name, lore);
    }

    /**
     * Same as {@link #icon(Material, Component, List)}, overlaying the name/lore onto an existing base stack —
     * used where the base already carries a texture or model data (see {@link #textureHead}).
     */
    public static ItemStack icon(ItemStack base, Component name, List<Component> lore) {
        ItemStack stack = base.clone();
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * A player head showing the skin texture {@code textureHash} (the last path segment of a
     * {@code textures.minecraft.net/texture/<hash>} URL), with {@code modelData} as its custom model data when
     * given — what a resourcepack keys its custom close/arrow buttons on. Falls back to a barrier if the
     * texture URL can't be built, so a typo in the hash never breaks the menu.
     */
    public static ItemStack textureHead(String textureHash, Integer modelData) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
            profile.getTextures().setSkin(URI.create("https://textures.minecraft.net/texture/" + textureHash).toURL());
            meta.setOwnerProfile(profile);
        } catch (MalformedURLException | IllegalArgumentException e) {
            return new ItemStack(Material.BARRIER);
        }
        if (modelData != null) {
            meta.setCustomModelData(modelData);
        }
        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack filler() {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        return stack;
    }
}
