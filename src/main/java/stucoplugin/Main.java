package stucoplugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class Main extends JavaPlugin {

  public static DBConnect db;
  StucoTabCompleter tabCompleter;
  int numfails = 3;
  public static String term;

  @Override
  public void onEnable() {
    // Load configuration
    saveDefaultConfig();
    numfails = getConfig().getInt("maxfails");
    String path = getConfig().getString("dbpath");
    term = getConfig().getString("term");

    // Connect to database
    try {
      db = new DBConnect(path);
    } catch (Exception e) {
      this.getLogger().severe("Failed to connect to database! System will be disabled.");
      this.numfails = 0;
    }

    // Load tab completer
    tabCompleter = new StucoTabCompleter(this);
    this.getCommand("hw").setTabCompleter(this.tabCompleter);
    // this.getCommand("hwadmin").setTabCompleter(this.tabCompleter);

    // Register events
    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(new EventListener(), this);
  }

  @Override
  public void onDisable() {
    if (db != null)
      db.close();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
    // Check for plugin disabled
    if (this.numfails <= 0) {
      sender
          .sendMessage("The plugin has encountered multiple errors and is disabled, please contact an administrator.");
      return true;
    }

    // Only players can safely use these commands
    if (!(sender instanceof Player)) {
      sender.sendMessage("You cannot use this command from the console!");
      return true;
    }

    try {

      if (cmd.getName().equalsIgnoreCase("hw")) {
        // base command usage hint
        if (args.length == 0) {
          sender.sendMessage("Usage: /hw [submit/list/advancement]");
          return true;
        }

        // process commands
        if (args[0].equalsIgnoreCase("submit")) {
          hwSubmit((Player) sender, args);
        } else if (args[0].equalsIgnoreCase("list")) {
          hwList((Player) sender, args);
        } else {
          sender.sendMessage("Usage: /hw [submit/list]");
        }

      } else if (cmd.getName().equalsIgnoreCase("hwadmin")) {
        // base command usage hint
        if (args.length == 0) {
          sender.sendMessage("Usage: /hwadmin [grade/show/tp/advancement/list]");
          return true;
        }

        // process commands
        if (args[0].equalsIgnoreCase("grade")) {
          hwadminGrade((Player) sender, args);
        } else if (args[0].equalsIgnoreCase("show")) {
          hwadminShow((Player) sender, args);
        } else if (args[0].equalsIgnoreCase("tp")) {
          hwadminTp((Player) sender, args);
        } else if (args[0].equalsIgnoreCase("advancement")) {
          hwAdvancement((Player) sender, args);
        } else if (args[0].equalsIgnoreCase("list")) {
          hwadminList((Player) sender, args);
        } else {
          sender.sendMessage("Usage: /hwadmin [grade/show/tp/advancement/list]");
        }
      }

    } catch (Exception e) {
      numfails--;
      sender.sendMessage("An error occurred while processing your command. Please contact an instructor.");
      e.printStackTrace();
    }

    return true;
  }

  private void hwSubmit(Player p, String[] args) throws SQLException {
    // check args length
    if (args.length != 2) {
      p.sendMessage("Usage: /hw submit [id]");
      return;
    }

    // Define useful variables
    DBConnect.Query q;
    String hwid = args[1];

    // Check if student in system
    q = db.queryDB("SELECT andrewID FROM intro2mc_student WHERE IGN = ?;", p.getName());
    if (!q.next()) {
      p.sendMessage(
          "You are not registered for the system! Please contact the instructors if you believe this is a mistake.");
      q.close();
      return;
    }
    String andrewID = q.getString("andrewID");
    q.close();

    // Check if assignment in system
    q = db.queryDB("SELECT id FROM intro2mc_assignment WHERE name = ? AND term = ? AND userSubmittable = true;", hwid,
        term);
    if (!q.next()) {
      p.sendMessage("Assignment " + hwid + " does not exist or is not submittable.");
      q.close();
      return;
    }
    int assignmentIndex = q.getInt("id");
    q.close();

    // Check current submission
    q = db.queryDB("SELECT id FROM intro2mc_submission WHERE assignment_id = ? AND student_id = ?;", assignmentIndex,
        andrewID);
    boolean exists = q.next();
    q.close();

    // Submit hw
    YamlConfiguration temp = new YamlConfiguration();
    temp.set("location", p.getLocation());
    String loc = temp.saveToString();
    long currentTime = System.currentTimeMillis();

    if (exists) {
      db.updateDB("UPDATE intro2mc_submission SET updated_at = ?, details = ? " +
          "WHERE assignment_id = ? AND student_id = ?;",
          currentTime, loc, assignmentIndex, andrewID);
      p.sendMessage("Submission for assignment " + hwid + " successfully updated!");
    } else {
      db.updateDB("INSERT INTO intro2mc_submission " +
          "(created_at, updated_at, assignment_id, student_id, details, grade) " +
          "VALUES (?, ?, ?, ?, ?, \"U\");",
          currentTime, currentTime, assignmentIndex, andrewID, loc);
      p.sendMessage("Successfully submitted " + hwid + " at your current location!");
    }
  }

  private void hwList(Player p, String[] args) throws SQLException {
    DBConnect.Query q;
    q = db.queryDB("SELECT name, description FROM intro2mc_assignment " +
        "WHERE term = ? AND userSubmittable = true ORDER BY created_at ASC;", term);
    StringBuilder sb = new StringBuilder();
    sb.append("----List of currently submittable homework for ").append(term).append("----");
    while (q.next()) {
      sb.append('\n').append(q.getString("name"));
      sb.append(": ").append(q.getString("description"));
    }
    q.close();
    p.sendMessage(sb.toString());

    VirtualUI ui = StudentUI.getHWUI(p, p);
    ui.showToPlayer(p);
  }

  private void hwadminList(Player p, String[] args) throws SQLException {
    // check args length
    if (args.length != 2) {
      p.sendMessage("Usage: /hwadmin list [andrewID]");
      return;
    }

    // Check if student in system
    String andrewID = args[1];
    DBConnect.Query q = db.queryDB("SELECT uuid FROM intro2mc_student WHERE andrewID = ?;", andrewID);
    if (!q.next()) {
      p.sendMessage("Student " + andrewID + " does not exist.");
      q.close();
      return;
    }
    String uuid = q.getString("uuid");
    q.close();

    UUID playerUUID = UUID.fromString(uuid);
    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
    VirtualUI ui = StudentUI.getHWUI(p, player);
    ui.showToPlayer(p);
  }

  // final command is /hwadmin grade [id] [studentid] [grade]
  // stopping at [id] produces a clickable list
  // grade is "Ungraded", "Pass", or "Fail"
  private void hwadminGrade(Player p, String[] args) throws SQLException {
    // Usage hint
    if (args.length <= 1) {
      p.sendMessage("Usage: /hwadmin grade [id]");

      // list submissions
    } else if (args.length == 2) {

      // Define useful variables
      DBConnect.Query q;
      String hwid = args[1];

      // Check if assignment in system
      q = db.queryDB("SELECT id FROM intro2mc_assignment WHERE name = ? AND term = ?;", hwid, term);
      if (!q.next()) {
        p.sendMessage("Assignment " + hwid + " does not exist.");
        q.close();
        return;
      }
      int assignmentIndex = q.getInt("id");
      q.close();

      // list all submissions
      ComponentBuilder cb = new ComponentBuilder("--------Submissions for " + hwid + "--------");
      q = db.queryDB(
          "SELECT student_id, details, grade FROM intro2mc_submission WHERE assignment_id = ? ORDER BY grade ASC;",
          assignmentIndex);
      while (q.next()) {
        String studentID = q.getString("student_id");
        String details = q.getString("details");
        String grade = q.getString("grade");
        // start
        cb.append("\n");
        // student name
        cb.append(studentID).color(ChatColor.RESET);
        // tp or not
        try {
          YamlConfiguration temp = new YamlConfiguration();
          temp.loadFromString(details);
          Location loc = temp.getLocation("location");
          cb.append("(tp)").color(ChatColor.AQUA)
              .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hwadmin tp " + hwid + " " + studentID));
        } catch (Exception e) {
        }
        // separator
        cb.append("................").color(ChatColor.DARK_GRAY);
        // pass
        cb.append("pass").color(grade.equals("P") ? ChatColor.GREEN : ChatColor.GRAY)
            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hwadmin grade " + hwid + " " + studentID + " pass"));
        cb.append(".").color(ChatColor.DARK_GRAY);
        // redo
        cb.append("redo").color(grade.equals("R") ? ChatColor.RED : ChatColor.GRAY)
            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/hwadmin grade " + hwid + " " + studentID + " redo"));
      }
      p.spigot().sendMessage(cb.create());

      // grade submission
    } else if (args.length == 4) {
      String gradeChar;
      switch (args[3]) {
        case "ungraded":
          gradeChar = "U";
          break;
        case "pass":
          gradeChar = "P";
          break;
        case "redo":
          gradeChar = "R";
          break;
        default:
          p.sendMessage("/hwadmin grade [hwid] [andrewid] [ungraded/pass/redo]");
          return;
      }

      // Check if assignment in system
      DBConnect.Query q = db.queryDB("SELECT id FROM intro2mc_assignment WHERE name = ? AND term = ?;", args[1], term);
      if (!q.next()) {
        p.sendMessage("Assignment " + args[1] + " does not exist.");
        q.close();
        return;
      }
      int assignmentIndex = q.getInt("id");
      q.close();

      // try update
      int rowschanged = db.updateDB("UPDATE intro2mc_submission SET grade = ? " +
          "WHERE assignment_id = ? AND student_id = ?;",
          gradeChar, assignmentIndex, args[2]);
      if (rowschanged == 0) {
        p.sendMessage("Student didn't submit this assignment!");
      } else {
        p.sendMessage("Grade successfully updated.");
      }

    } else {
      p.sendMessage("/hwadmin grade [id] [studentid] [grade]");
    }
  }

  private void hwAdvancement(Player p, String[] args) throws SQLException {
    if (args.length != 3) {
      p.sendMessage(
          "/hwadmin advancement [hwid] [advancement] (for example: /hwadmin advancement hw1 story/mine_diamond)");
      return;
    }
    Advancement adv = Bukkit.getAdvancement(NamespacedKey.minecraft(args[2]));

    // Check if assignment in system
    DBConnect.Query q = db.queryDB("SELECT id FROM intro2mc_assignment WHERE name = ? AND term = ?;", args[1], term);
    if (!q.next()) {
      p.sendMessage("Assignment " + args[1] + " does not exist.");
      q.close();
      return;
    }
    int assignmentIndex = q.getInt("id");
    q.close();

    // List all student andrewID(varchar(200)) and uuid(varchar(200))
    VirtualUI ui = new VirtualUI("Advancement for " + args[1], 54);
    q = db.queryDB("SELECT andrewID, uuid FROM intro2mc_student;");
    ArrayList<String> andrewIDs = new ArrayList<String>();
    ArrayList<Boolean> completeds = new ArrayList<Boolean>();
    while (q.rs.next()) {
      String andrewID = q.rs.getString("andrewID");
      String uuid = q.rs.getString("uuid");
      andrewIDs.add(andrewID);

      // setting items in virtual inventory
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
      Player player = offlinePlayer.getPlayer(); // Load the player's profile from disk
      if (player == null) {
        ui.addItemStack(-1, offlinePlayer.getUniqueId(), andrewID, Arrays.asList("Player Offline"), false, null);
        completeds.add(false);
      } else {
        AdvancementProgress progress = player.getAdvancementProgress(adv);
  
        Boolean completed = progress.isDone();
        completeds.add(completed);
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(adv.toString() + " " + (completed ? "completed" : "not completed"));
        if (progress.getAwardedCriteria().size() == 0) {
          lore.add("Awarded Criteria: (none)");
        } else {
          lore.add("Awarded Criteria:");
          for (String criteria : progress.getAwardedCriteria()) {
            lore.add(" - " + criteria);
          }
        }
        if (progress.getRemainingCriteria().size() == 0) {
          lore.add("Remaining Criteria: (none)");
        } else {
          lore.add("Remaining Criteria:");
          for (String criteria : progress.getRemainingCriteria()) {
            lore.add(" - " + criteria);
          }
        }
        ui.addItemStack(-1, player.getUniqueId(), andrewID, lore, completed, null);
      }
    }
    q.close();
    ui.addLineBreak(1, Material.BLACK_STAINED_GLASS_PANE);
    ui.addItemStack(-1, Material.GREEN_WOOL, "Confirm",
        Arrays.asList("update " + adv.toString() + " to HW" + args[1]), true,
        (VirtualUI callbackUI, Player player, ItemStack item, int slot, int index) -> {
          int totalRowsChanged = 0;
          long currentTime = System.currentTimeMillis();
          ArrayList<String> completedAndrewIDs = new ArrayList<String>();
          ArrayList<String> notCompletedAndrewIDs = new ArrayList<String>();
          for (int i = 0; i < andrewIDs.size(); i++) {
            String andrewID = andrewIDs.get(i);
            Boolean completed = completeds.get(i);
            if (completed) {
              try {
                int rowschanged = db.updateDB("INSERT INTO intro2mc_submission " +
                    "(created_at, updated_at, assignment_id, student_id, details, grade) " +
                    "VALUES (?, ?, ?, ?, ?, \"P\");",
                    currentTime, currentTime, assignmentIndex, andrewID, "automatically submitted by player " + player.getDisplayName() + " (" + player.getUniqueId() + ")");
                totalRowsChanged += rowschanged;
                completedAndrewIDs.add(andrewID);
              } catch (Exception e) {
                p.sendMessage("Error updating grade for " + andrewID + ". Maybe they already submitted?");
              }
            } else {
              notCompletedAndrewIDs.add(andrewID);
            }
          }
          p.sendMessage("Grade successfully updated for " + String.valueOf(totalRowsChanged) + " students.");
          p.sendMessage("Completed: " + String.join(", ", completedAndrewIDs));
          p.sendMessage("Not completed: " + String.join(", ", notCompletedAndrewIDs));
        });
    ui.showToPlayer(p.getUniqueId(), 0);
  }

  private void hwadminShow(Player p, String[] args) {
  }

  private void hwadminTp(Player p, String[] args) throws SQLException {
    if (args.length != 3) {
      p.sendMessage("/hwadmin tp [hwid] [andrewid]");
      return;
    }

    DBConnect.Query q;

    // Check if assignment in system
    q = db.queryDB("SELECT id FROM intro2mc_assignment WHERE name = ? AND term = ?;", args[1], term);
    if (!q.next()) {
      p.sendMessage("Assignment " + args[1] + " does not exist.");
      q.close();
      return;
    }
    int assignmentIndex = q.getInt("id");
    q.close();

    // try to get details
    q = db.queryDB("SELECT details FROM intro2mc_submission WHERE assignment_id = ? AND student_id = ?;", assignmentIndex,
        args[2]);
    if (!q.next()) {
      p.sendMessage("Student didn't submit this assignment!");
      q.close();
      return;
    }
    String details = q.getString("details");
    q.close();

    // try to tp
    try {
      YamlConfiguration temp = new YamlConfiguration();
      temp.loadFromString(details);
      Location loc = temp.getLocation("location");
      p.teleport(loc);
    } catch (Exception e) {
      p.sendMessage("This submission/assignment is not a teleportable location!");
    }
  }

}