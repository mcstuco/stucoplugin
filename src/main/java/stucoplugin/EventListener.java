package stucoplugin;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class EventListener implements Listener {

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onPlayerJoin(PlayerJoinEvent event) {
    // namespace can be found: https://minecraft.fandom.com/wiki/Advancement
    Advancement adv = Bukkit.getAdvancement(NamespacedKey.minecraft("story/mine_diamond"));
    Main.updateAdvancement(Bukkit.getServer().getConsoleSender(), "hw3", adv);

    adv = Bukkit.getAdvancement(NamespacedKey.minecraft("end/enter_end_gateway"));
    Main.updateAdvancement(Bukkit.getServer().getConsoleSender(), "hw4", adv);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInventoryClick(InventoryClickEvent event) {
    int size = event.getInventory().getSize();
    if (size < 18) {
      return;
    }

    // check if last item is close
    if (event.getInventory().getItem(size - 1) != null) {
      if (event.getInventory().getItem(size - 1).getType() == Material.BARRIER) {
        // then it probably is a virtual ui
        event.setCancelled(true);

        if (event.getInventory().getItem(size - 1).getItemMeta().getDisplayName()
            .contains(VirtualUI.CLOSE_NAME)) {
          UUID uuid = null;
          String uuidString = event.getInventory().getItem(size - 9 + 1).getItemMeta()
              .getLore().get(1);
          try {
            // find uuid in string based on regex
            // Pattern pattern =
            // Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
            // Pattern.CASE_INSENSITIVE);
            // uuidString = pattern.matcher(uuidString).group();
            // uuid = UUID.fromString(uuidString);
            uuidString = uuidString.substring(uuidString.length() - 36, uuidString.length());
            uuid = UUID.fromString(uuidString);
          } catch (IllegalStateException e) {
            event.getWhoClicked().sendMessage("UUID of Inventroy is not valid in " + uuidString);
            return;
          }

          VirtualUI ui = VirtualUI.uiMap.get(uuid);
          if (ui == null) {
            event.getWhoClicked().sendMessage("UUID of Inventroy is not valid in " + uuidString);
            return;
          }

          if (event.getSlot() == size - 1) {
            event.getWhoClicked().closeInventory();
            return;
          }

          // handel normal item click
          if (event.getCurrentItem() != null) {
            if (event.getCurrentItem().getType() != Material.AIR) {
              // get which item in List<ItemStack> is clicked
              int pageSize = size - 9;
              int currentPage = Integer
                  .parseInt(event.getInventory().getItem(size - 9 + 1).getItemMeta()
                      .getLore().get(0).split(" ")[2])
                  - 1;
              int clickedItemIndex = event.getSlot() + (currentPage * pageSize);

              // check for page control
              if (event.getCurrentItem().getItemMeta() != null) {
                if (event.getCurrentItem().getItemMeta().getDisplayName() != null) {
                  if (event.getCurrentItem().getItemMeta().getDisplayName().contains(VirtualUI.NEXT_PAGE_NAME)) {
                    event.getWhoClicked().closeInventory();
                    ui.showToPlayer((Player) event.getWhoClicked(), currentPage + 1);
                    return;
                  } else if (event.getCurrentItem().getItemMeta().getDisplayName()
                      .contains(VirtualUI.PREVIOUS_PAGE_NAME)) {
                    event.getWhoClicked().closeInventory();
                    ui.showToPlayer((Player) event.getWhoClicked(), currentPage - 1);
                    return;
                  }
                }
              }

              // check for callback
              if (clickedItemIndex < ui.callbacks.size() &&
                  ui.callbacks.get(clickedItemIndex) != null) {
                ui.callbacks.get(clickedItemIndex).call(ui, (Player) event.getWhoClicked(), event.getCurrentItem(),
                    event.getSlot(), currentPage);
                return;
              }

            }
          }
        }
      }
    }
  }
}
