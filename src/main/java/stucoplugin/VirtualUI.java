package stucoplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

@FunctionalInterface
interface VirtualUICallback {
  void call(VirtualUI ui, Player player, ItemStack item, int slot, int index);

  public static VirtualUICallback withConfirm(String name, String message, VirtualUICallback callback) {
    VirtualUICallback callbackWrapper = (_ui, _player, _item, _slot, _index) -> {
      VirtualUICallback closeInv = (_ui2, _player2, _item2, _slot2, _index2) -> {
        _player2.closeInventory();
      };
      VirtualUICallback confirm = (_ui2, _player2, _item2, _slot2, _index2) -> {
        _player2.closeInventory();
        callback.call(_ui2, _player2, _item2, _slot2, _index2);
      };
      VirtualUI ui = new VirtualUI(name, 3 * 9);
      ui.addItemStack(0, Material.RED_STAINED_GLASS_PANE, "Cancel", null, false, closeInv);
      ui.addItemStack(1, Material.RED_STAINED_GLASS_PANE, "Cancel", null, false, closeInv);
      ui.addItemStack(2, Material.RED_STAINED_GLASS_PANE, "Cancel", null, false, closeInv);
      ui.addItemStack(3, Material.RED_STAINED_GLASS_PANE, "Cancel", null, false, closeInv);
      if (message != null)
        ui.addItemStack(4, Material.PAPER, message, null, false, null);
      ui.addItemStack(5, Material.GREEN_STAINED_GLASS_PANE, "Confirm", null, false, confirm);
      ui.addItemStack(6, Material.GREEN_STAINED_GLASS_PANE, "Confirm", null, false, confirm);
      ui.addItemStack(7, Material.GREEN_STAINED_GLASS_PANE, "Confirm", null, false, confirm);
      ui.addItemStack(8, Material.GREEN_STAINED_GLASS_PANE, "Confirm", null, false, confirm);
      ui.showToPlayer(_player);
    };
    return callbackWrapper;
  }
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
    this.maxInventorySize = (int) Math.ceil(this.maxInventorySize / 9.0) * 9;

    ui = new ArrayList<ItemStack>();
    callbacks = new ArrayList<VirtualUICallback>();
    uiMap.put(this.uuid, this);
  }

  public ItemStack getItemStack(Material material, String name, List<String> lore, Boolean glow) {
    ItemStack item = setNameLore(new ItemStack(material), name, lore);
    if (glow) {
      item = setGlow(item);
    }
    return item;
  }

  public ItemStack getItemStack(UUID playerUUID, String name, List<String> lore, Boolean glow) {
    ItemStack item = getItemStack(Material.PLAYER_HEAD, name, lore, glow);
    SkullMeta meta = (SkullMeta) item.getItemMeta();
    meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerUUID));
    item.setItemMeta(meta);
    return item;
  }

  public void setItemStack(List<ItemStack> ui) {
    this.ui = ui;
  }

  private ItemStack setGlow(ItemStack itemStack) {
    ItemMeta meta = itemStack.getItemMeta();
    if (meta == null) {
      return itemStack;
    }
    meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    itemStack.setItemMeta(meta);
    return itemStack;
  }

  private String formatString(String line) {
    if (line.contains(": ")) {
      String[] split = line.split(": ", 1);
      if (split.length == 2) {
        line = "&n&7" + split[0] + ": &r&8" + split[1];
      } else {
        line = "&r&7" + line;
      }
    } else {
      line = "&r&7" + line;
    }
    line = line.replace("false", "&cfalse&7")
        .replace("true", "&atrue&7");
    line = line.replace("UNGRADED", "&fUNGRADED&7")
        .replace("PASS", "&aPASS&7")
        .replace("REDO", "&4REDO&7")
        .replace("ERROR", "&cERROR&7");
    line = line.replace("No submissions yet!", "&cNo submissions yet!&7");
    line = ChatColor.translateAlternateColorCodes('&', line);
    return line;
  }

  private ItemStack setNameLore(ItemStack itemStack, String name, List<String> lore) {
    ItemMeta meta = itemStack.getItemMeta();
    if (meta == null) {
      return itemStack;
    }

    // set name
    if (name == null) {
      meta.setDisplayName("");
    } else {
      name = formatString(name);
      meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&r&f" + name));
    }

    if (lore != null) {
      List<String> newLore = new ArrayList<String>();
      for (String line : lore) {
        line = formatString(line);
        newLore.add(line);
      }
      meta.setLore(newLore);
    }

    itemStack.setItemMeta(meta);
    return itemStack;
  }

  public void addItemStack(int slot, ItemStack item, VirtualUICallback callback) {
    if (slot < 0) {
      // that means we assign slots automatically
      slot = ui.size();
    }

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
    ItemStack item = getItemStack(mat, "", null, false);
    while (ui.size() % 9 != 0) {
      addItemStack(-1, item, null);
    }
    for (int i = 0; i < howManyLineBreaks; i++) {
      for (int j = 0; j < 9; j++) {
        addItemStack(-1, item, null);
      }
    }
  }

  public void addItemStack(int slot, Material material, String name, List<String> lore, Boolean glow,
      VirtualUICallback callback) {
    ItemStack item = getItemStack(material, name, lore, glow);
    addItemStack(slot, item, callback);
  }

  public void addItemStack(int slot, UUID playerUUID, String name, List<String> lore, Boolean glow,
      VirtualUICallback callback) {
    ItemStack item = getItemStack(playerUUID, name, lore, glow);
    addItemStack(slot, item, callback);
  }

  // public void setPlayerHead(ItemStack item, String textureURL) {
  // try {
  // SkullMeta meta = (SkullMeta) item.getItemMeta();
  // OfflinePlayer owner =
  // Bukkit.getOfflinePlayer(UUID.fromString("2551d748-22c7-439e-ac63-1b41911d3953"));
  // meta.setOwningPlayer(owner);
  // PlayerProfile playerProfile = Bukkit.createPlayerProfile(owner.getUniqueId(),
  // owner.getName());

  // meta.setOwnerProfile(playerProfile);
  // PlayerTextures textures = meta.getOwnerProfile().getTextures();
  // URL url = new URL("http://textures.minecraft.net/texture/" + textureURL);
  // textures.setSkin(url);
  // meta.getOwnerProfile().setTextures(textures);
  // item.setItemMeta(meta);
  // } catch (MalformedURLException e) {
  // e.printStackTrace();
  // }
  // }

  private int getLastPageIndex() {
    if (ui.size() % (this.maxInventorySize - 9) == 0) {
      return ui.size() / (this.maxInventorySize - 9) - 1;
    }
    return ui.size() / (this.maxInventorySize - 9);
  }

  private Inventory getUI(int page) {
    if (page < 0) {
      page = 0;
    }
    if (page > this.getLastPageIndex()) {
      page = this.getLastPageIndex();
    }

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

          inventory.setItem(i, item);
        } else {
          ItemStack item = getItemStack(Material.RED_STAINED_GLASS_PANE, PREVIOUS_PAGE_NAME, null, false);
          inventory.setItem(i, item);
        }
      } else if (i == pageSize + 1) {
        // second slot is for current page
        List<String> lore = new ArrayList<String>();
        lore.add("Current Page: " + (page + 1));
        lore.add("Current UUID: " + this.uuid.toString());
        lore.add("Total Page: " + (getLastPageIndex() + 1));
        ItemStack item = getItemStack(Material.COMMAND_BLOCK, CURRENT_PAGE_NAME, lore, false);
        inventory.setItem(i, item);
      } else if (i == pageSize + 2) {
        // third slot is for next page
        if (page != getLastPageIndex()) {
          ItemStack item = getItemStack(Material.ARROW, NEXT_PAGE_NAME, null, false);
          inventory.setItem(i, item);
        } else {
          ItemStack item = getItemStack(Material.RED_STAINED_GLASS_PANE, NEXT_PAGE_NAME, null, false);
          inventory.setItem(i, item);
        }
      } else if (i == pageSize + 8) {
        // last slot is for close
        ItemStack item = getItemStack(Material.BARRIER, CLOSE_NAME, null, true);
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
