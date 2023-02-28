package stucoplugin;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class EventListener implements Listener {

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInventoryClick(InventoryClickEvent event) {
    int size = event.getInventory().getSize();
    if (size < 18) {
      return;
    }

    // check if last item is close
    if (event.getInventory().getItem(size - 1) != null) {
      if (event.getInventory().getItem(size - 1).getType() == Material.BARRIER) {
        if (event.getInventory().getItem(size - 1).getItemMeta().getDisplayName()
            .equals(VirtualUI.CLOSE_NAME)) {
          UUID uuid = null;
          try {
            uuid = UUID.fromString(event.getInventory().getItem(size - 9 + 1).getItemMeta()
                .getLore().get(1).split(" ")[2]);
          } catch (IllegalArgumentException e) {
            event.getWhoClicked().sendMessage("UUID is not valid");
            return;
          }

          // then it is our inventory
          event.setCancelled(true);
          VirtualUI ui = VirtualUI.uiMap.get(uuid);

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
                  if (event.getCurrentItem().getItemMeta().getDisplayName().equals(VirtualUI.NEXT_PAGE_NAME)) {
                    event.getWhoClicked().closeInventory();
                    ui.showToPlayer((Player) event.getWhoClicked(), currentPage + 1);
                    return;
                  } else if (event.getCurrentItem().getItemMeta().getDisplayName()
                      .equals(VirtualUI.PREVIOUS_PAGE_NAME)) {
                    event.getWhoClicked().closeInventory();
                    ui.showToPlayer((Player) event.getWhoClicked(), currentPage - 1);
                    return;
                  }
                }
              }

              // check for callback
              if (ui.callbacks.get(clickedItemIndex) != null) {
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
