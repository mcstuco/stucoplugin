package stucoplugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

@FunctionalInterface
interface VirtualUICallback {
  void call(VirtualUI ui, Player player, ItemStack item, int slot, int index);
}

public class VirtualUI {
  public static final String HW_ADVANCEMENT_UI_TITLE = "HW Advancement - Review and Hit Confirm";
  public static final String NEXT_PAGE_NAME = "Next Page";
  public static final String PREVIOUS_PAGE_NAME = "Previous Page";
  public static final String CURRENT_PAGE_NAME = "Current Page";
  public static final String CLOSE_NAME = "Close";

  public String name;
  public int maxInventorySize;
  public UUID uuid;
  public List<ItemStack> ui;
  public List<VirtualUICallback> callbacks;

  public static HashMap<UUID, VirtualUI> uiMap = new HashMap<UUID, VirtualUI>();

  public VirtualUI(String name, int maxInventorySize) {
    // maxInventorySize is the max size is 54
    this.name = name;
    this.maxInventorySize = maxInventorySize;
    this.uuid = UUID.randomUUID();

    // maxInventorySize cannot be less than 18
    if (maxInventorySize < 18) {
      this.maxInventorySize = 18;
    }

    // 0, 9, 18, 27, 36, 45, or 54 slots of type
    this.maxInventorySize = (int) Math.ceil(maxInventorySize / 9.0) * 9;

    ui = new ArrayList<ItemStack>();
    callbacks = new ArrayList<VirtualUICallback>();
    uiMap.put(this.uuid, this);
  }

  public ItemStack getItemStack(Material material, String name, List<String> lore, Boolean glow) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(name);
    meta.setLore(lore);
    if (glow) {
      meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    item.setItemMeta(meta);
    return item;
  }

  public ItemStack getItemStack(UUID playerUUID, String name, List<String> lore, Boolean glow) {
    ItemStack item = new ItemStack(Material.PLAYER_HEAD);
    SkullMeta meta = (SkullMeta) item.getItemMeta();
    meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
    meta.setDisplayName(name);
    meta.setLore(lore);
    if (glow) {
      meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    item.setItemMeta(meta);
    return item;
  }

  public void setItemStack(List<ItemStack> ui) {
    this.ui = ui;
  }

  private void fixColor(ItemStack itemStack) {
    ItemMeta meta = itemStack.getItemMeta();

    String name = "&r" + meta.getDisplayName();
    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

    List<String> lore = meta.getLore();
    if (lore != null) {
      List<String> newLore = new ArrayList<String>();
      for (String line : lore) {
        if (line.contains(": ")) {
          String[] split = line.split(": ", 1);
          if (split.length == 2) {
            line = "&l&7" + split[0] + ": &r&7" + split[1];
          } else {
            line = "&r&7" + line;
          }
        } else {
          line = "&r&7" + line;
        }
        line = ChatColor.translateAlternateColorCodes('&', line);
        newLore.add(line);
      }
      meta.setLore(newLore);
      itemStack.setItemMeta(meta);
    }
  }

  public void addItemStack(int slot, ItemStack item, VirtualUICallback callback) {
    if (slot < 0) {
      // that means we assign slots automatically
      slot = ui.size();
    }

    // process item's name to have normal white front
    ItemMeta meta = item.getItemMeta();
    String name = meta.getDisplayName();
    name = "&f" + name;
    name = ChatColor.translateAlternateColorCodes('&', name);
    meta.setDisplayName(name);

    // process item's lore to have normal white front
    fixColor(item);

    if (slot < ui.size()) {
      ui.set(slot, item);
      callbacks.set(slot, callback);
    } else {
      while (slot >= ui.size()) {
        ui.add(new ItemStack(Material.AIR));
        callbacks.add(null);
      }
      ui.set(slot, item);
      callbacks.set(slot, callback);
    }
  }

  public void addLineBreak(int howManyLineBreaks, Material mat) {
    while (ui.size() % 9 != 0) {
      ui.add(new ItemStack(mat));
      callbacks.add(null);
    }
    for (int i = 0; i < howManyLineBreaks; i++) {
      for (int j = 0; j < 9; j++) {
        ui.add(new ItemStack(mat));
        callbacks.add(null);
      }
    }
  }

  public void addItemStack(int slot, Material material, String name, List<String> lore, Boolean glow,
      VirtualUICallback callback) {
    ItemStack item = new ItemStack(material);
    ItemMeta meta = item.getItemMeta();
    meta.setDisplayName(name);
    meta.setLore(lore);
    if (glow) {
      meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    item.setItemMeta(meta);
    addItemStack(slot, item, callback);
  }

  public void addItemStack(int slot, UUID playerUUID, String name, List<String> lore, Boolean glow,
      VirtualUICallback callback) {
    ItemStack item = new ItemStack(Material.PLAYER_HEAD);
    SkullMeta meta = (SkullMeta) item.getItemMeta();
    meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
    meta.setDisplayName(name);
    meta.setLore(lore);
    if (glow) {
      meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
    item.setItemMeta(meta);
    addItemStack(slot, item, callback);
  }

  // public void setPlayerHead(ItemStack item, String textureURL) {
  //   try {
  //     SkullMeta meta = (SkullMeta) item.getItemMeta();
  //     OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString("2551d748-22c7-439e-ac63-1b41911d3953"));
  //     meta.setOwningPlayer(owner);
  //     PlayerProfile playerProfile = Bukkit.createPlayerProfile(owner.getUniqueId(), owner.getName());

  //     meta.setOwnerProfile(playerProfile);
  //     PlayerTextures textures = meta.getOwnerProfile().getTextures();
  //     URL url = new URL("http://textures.minecraft.net/texture/" + textureURL);
  //     textures.setSkin(url);
  //     meta.getOwnerProfile().setTextures(textures);
  //     item.setItemMeta(meta);
  //   } catch (MalformedURLException e) {
  //     e.printStackTrace();
  //   }
  // }

  private Inventory getUI(int page) {
    Inventory inventory = Bukkit.createInventory(null, this.maxInventorySize, this.name);
    int pageSize = this.maxInventorySize - 9;
    for (int i = 0; i < pageSize; i++) {
      int index = i + (page * pageSize);
      if (index >= ui.size()) {
        break;
      }
      inventory.setItem(i, ui.get(index));
    }

    // last 1 row is for page control
    for (int i = pageSize; i < this.maxInventorySize; i++) {
      if (i == pageSize) {
        // first slot is for previous page
        if (page > 0) {
          ItemStack item = getItemStack(Material.ARROW, PREVIOUS_PAGE_NAME, null, false);
          fixColor(item);
          inventory.setItem(i, item);
        } else {
          ItemStack item = getItemStack(Material.RED_STAINED_GLASS_PANE, PREVIOUS_PAGE_NAME, null, false);
          fixColor(item);
          inventory.setItem(i, item);
        }
      } else if (i == pageSize + 1) {
        // second slot is for current page
        List<String> lore = new ArrayList<String>();
        lore.add("Current Page: " + (page + 1));
        lore.add("Current UUID: " + this.uuid.toString());
        lore.add("Total Page: " + (ui.size() / pageSize + 1));
        ItemStack item = getItemStack(Material.COMMAND_BLOCK, CURRENT_PAGE_NAME, lore, false);
        fixColor(item);
        inventory.setItem(i, item);
      } else if (i == pageSize + 2) {
        // third slot is for next page
        if (page < ui.size() / pageSize) {
          ItemStack item = getItemStack(Material.ARROW, NEXT_PAGE_NAME, null, false);
          fixColor(item);
          inventory.setItem(i, item);
        } else {
          ItemStack item = getItemStack(Material.RED_STAINED_GLASS_PANE, NEXT_PAGE_NAME, null, false);
          fixColor(item);
          inventory.setItem(i, item);
        }
      } else if (i == pageSize + 8) {
        // last slot is for close
        ItemStack item = getItemStack(Material.BARRIER, CLOSE_NAME, null, true);
        fixColor(item);
        inventory.setItem(i, item);
      } else {
        // other slots are empty
        inventory.setItem(i, new ItemStack(Material.AIR));
      }
    }
    return inventory;
  }

  public void showToPlayer(UUID playerUUID, int page) {
    if (Bukkit.getPlayer(playerUUID) != null) {
      Bukkit.getPlayer(playerUUID).openInventory(getUI(page));
    }
  }

  public void showToPlayer(Player player, int page) {
    player.openInventory(getUI(page));
  }

  public void showToPlayer(UUID playerUUID) {
    showToPlayer(playerUUID, 0);
  }

  public void showToPlayer(Player player) {
    showToPlayer(player, 0);
  }

}
