package stucoplugin;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.TabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

class StucoTabCompleter implements TabCompleter {

  Main plugin;
  String[] hwSubCommands = {"submit", "list"};
  //String[] hwadminSubCommands = {"grade"};
  long cacheLastUpdated = 0;
  List<String> assignmentsCache = new ArrayList<String>();

  public StucoTabCompleter(Main plugin) {
    this.plugin = plugin;
  }

  public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
    List<String> returnList = new ArrayList<String>();

    if (cmd.getName().equalsIgnoreCase("hw")) {

      if (args.length == 0) {
        returnList = Arrays.asList(hwSubCommands);

      } else if (args.length == 1) {
        for (String candidate : this.hwSubCommands) {
          if (candidate.startsWith(args[0])) returnList.add(candidate);
        }

      } else if (args.length == 2) {
        if (args[0].equalsIgnoreCase("submit")) {
          tryUpdateAssignmentCache();
          for (String name : assignmentsCache) {
            if (name.contains(args[1])) returnList.add(name);
          }
        }
      }

    }/* else if (cmd.getName().equalsIgnoreCase("hwadmin")) {

      if (args.length == 0) {
        returnList = Arrays.asList(this.hwadminSubCommands);
      } else if (args.length == 1) {
        for (String candidate : this.hwadminSubCommands) {
          if (candidate.startsWith(args[0])) returnList.add(candidate);
        }
      } else if (args.length == 2) {
        if (args[0].equalsIgnoreCase("grade")) {
          // TODO
        }
      }
    }*/

    return returnList;

  }

  private void tryUpdateAssignmentCache() {
    if (System.currentTimeMillis() <= cacheLastUpdated + 60*1000) return;

    try{
      DBConnect.Query q = plugin.db.queryDB("SELECT name FROM intro2mc_assignment WHERE term = ? AND userSubmittable = true;", plugin.term);
      assignmentsCache.clear();
      while (q.next()) {
        assignmentsCache.add(q.getString("name"));
      }
      q.close();
      cacheLastUpdated = System.currentTimeMillis();
    } catch (Exception e) {
      e.printStackTrace();
      plugin.numfails--;
    }
  }

}