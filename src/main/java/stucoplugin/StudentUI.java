package stucoplugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StudentUI {
  public static VirtualUI getHWUI(Player sender, OfflinePlayer p) throws SQLException {
    DBConnect.Query q;
    DBConnect db = Main.db;
    String term = Main.term;

    // START SHOWING VirtualUI
    // check if student in system
    q = db.queryDB("SELECT andrewID, name, uuid, discord FROM intro2mc_student WHERE IGN = ?;", p.getName());
    if (!q.next()) {
      sender.sendMessage(
          "The player " + p.getName()
              + " is not registered for the system! Please contact the instructors if you believe this is a mistake.");
      q.close();
      return null;
    }
    String andrewID = q.getString("andrewID");
    String studentName = q.getString("name");
    String uuid = q.getString("uuid");
    String discord = q.getString("discord");
    q.close();

    // getting assignments
    VirtualUI ui = new VirtualUI("MCStudio - " + p.getName(), 54);
    q = db.queryDB("SELECT id, name, description, term, userSubmittable, gradeReleased FROM intro2mc_assignment " +
        "WHERE term = ? ORDER BY created_at ASC;", term);

    ArrayList<String> lore = new ArrayList<String>();
    lore.add("Andrew ID: " + andrewID);
    lore.add("Student Name: " + studentName);
    lore.add("UUID: " + uuid);
    lore.add("Discord: " + discord);
    ui.addItemStack(-1, p.getUniqueId(), p.getName(), null, false, null);
    ui.addLineBreak(1, Material.BLACK_STAINED_GLASS_PANE);

    List<ItemStack> items = new ArrayList<ItemStack>();
    while (q.next()) {
      int assignmentIndex = q.getInt("id");
      String assignmentName = q.getString("name");
      String desc = q.getString("description");
      String assignmentTerm = q.getString("term");
      Boolean submittable = q.getBoolean("userSubmittable");
      Boolean gradeReleased = q.getBoolean("gradeReleased");

      List<String> assignmentLore = new ArrayList<String>();
      assignmentLore.add("Description: " + desc);
      assignmentLore.add("Term: " + assignmentTerm);
      assignmentLore.add("Achievement Based: " + !submittable);
      assignmentLore.add("Grade Released: " + gradeReleased);
      assignmentLore.add("");

      Boolean submittedOnce = false;

      // getting submissions
      DBConnect.Query qq = db.queryDB(
          "SELECT student_id, details, grade, created_at, updated_at FROM intro2mc_submission WHERE assignment_id = ? AND student_id = ? ORDER BY grade ASC;",
          assignmentIndex, andrewID);
      while (qq.next()) {
        String studentID = qq.getString("student_id");
        String details = qq.getString("details");
        String grade = qq.getString("grade");
        String gradeInterpretation = "Error";
        switch (grade) {
          case "U":
            gradeInterpretation = "&fUNGRADED";
            break;
          case "P":
            gradeInterpretation = "&aPASS";
            break;
          case "R":
            gradeInterpretation = "&4REDO";
            break;
          default:
            gradeInterpretation = "&cERROR";
            break;
        }
        String submissionTime = qq.getString("created_at");
        String updateTime = qq.getString("updated_at");
        submittedOnce = true;

        assignmentLore.add("Submission Time: " + submissionTime);
        assignmentLore.add("Update Time: " + updateTime);
        try {
          YamlConfiguration temp = new YamlConfiguration();
          temp.loadFromString(details);
          Location loc = temp.getLocation("location");
          assignmentLore.add("Location: (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
        } catch (Exception e) {
        }
        assignmentLore.add("Grade: " + gradeInterpretation);
        assignmentLore.add("");
      }
      qq.close();
      if (submittedOnce) {
        items.add(ui.getItemStack(Material.MAP, assignmentName, assignmentLore, gradeReleased));
      } else {
        assignmentLore.add("No submissions yet!");
        items.add(ui.getItemStack(Material.PAPER, assignmentName, assignmentLore, gradeReleased));
      }
    }

    items.sort((a, b) -> {
      String aName = a.getItemMeta().getDisplayName();
      String bName = b.getItemMeta().getDisplayName();
      return aName.compareTo(bName);
    });
    for (ItemStack item : items) {
      ui.addItemStack(-1, item, null);
    }

    q.close();
    return ui;
  }
}
