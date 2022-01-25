package stucoplugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

public class Main extends JavaPlugin {

  DBConnect db;
  StucoTabCompleter tabCompleter;
  int numfails = 3;
  String term;

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
    //this.getCommand("hwadmin").setTabCompleter(this.tabCompleter);
  }

  @Override
  public void onDisable() {
    if (db != null) db.close();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
    // Check for plugin disabled
    if (this.numfails <= 0) {
      sender.sendMessage("The plugin has encountered multiple errors and is disabled, please contact an administrator.");
      return true;
    }

    try {

      if (cmd.getName().equalsIgnoreCase("hw")) { // user command
        // user (non-admin) command only executable by players
        if (!(sender instanceof Player)) {
          sender.sendMessage("You cannot submit homework from the console using \"/hw\". Please use \"hwadmin\" instead.");
          return true;
        }

        // base command usage hint
        if (args.length == 0) {
          sender.sendMessage("Usage: /hw [submit]");
          return true;
        }

        // process commands
        if (args[0].equalsIgnoreCase("submit")) {
          this.hwSubmit((Player) sender, args);
        } else if (args[0].equalsIgnoreCase("list")) {
          this.hwList((Player) sender, args);
        } else {
          sender.sendMessage("Usage: /hw [submit]");
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
      p.sendMessage("You are not registered for the system! Please contact the instructors if you believe this is a mistake.");
      q.close();
      return;
    }
    String andrewID = q.getString("andrewID");
    q.close();

    // Check if assignment in system
    q = db.queryDB("SELECT id FROM intro2mc_assignment WHERE name = ? AND term = ? AND userSubmittable = true;", hwid, term);
    if (!q.next()) {
      p.sendMessage("Assignment " + hwid + " does not exist or is not submittable.");
      q.close();
      return;
    }
    int assignmentID = q.getInt("id");
    q.close();

    // Check current submission
    q = db.queryDB("SELECT id FROM intro2mc_submission WHERE assignment_id = ? AND student_id = ?;", assignmentID, andrewID);
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
                  currentTime, loc, assignmentID, andrewID);
      p.sendMessage("Submission for assignment " + hwid + " successfully updated!");
    } else {
      db.updateDB("INSERT INTO intro2mc_submission " +
                  "(created_at, updated_at, assignment_id, student_id, details, grade) " +
                  "VALUES (?, ?, ?, ?, ?, \"U\");",
                  currentTime, currentTime, assignmentID, andrewID, loc);
      p.sendMessage("Successfully submitted " + hwid + " at your current location!");
    }
  }

  private void hwList(Player p, String[] args) throws SQLException {
    DBConnect.Query q;
    q = db.queryDB("SELECT name, description FROM intro2mc_assignment " +
                   "WHERE term = ? AND userSubmittable = true ORDER BY created_at ASC;", term);
    StringBuilder sb = new StringBuilder();
    sb.append("--------List of currently submittable homework for ").append(term).append(" --------");
    while (q.next()) {
      sb.append(q.getString("name")).append(": ");
      sb.append(q.getString("description")).append('\n');
    }
    q.close();
    p.sendMessage(sb.toString());
  }



}