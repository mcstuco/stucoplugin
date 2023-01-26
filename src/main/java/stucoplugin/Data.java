package stucoplugin;

import java.util.ArrayList;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Location;

public class Data {

  private ArrayList<Assignment> assignments;
  private ArrayList<Student> students;

  private String filetext;

  public Data {
    assignments = new ArrayList<Assignment>();
    students = new ArrayList<Student>();
    filetext = null;
  }

  public int load_csv(String path) {
    // some method to read file into "filetext", return 1 if fail

    String[] lines = filetext.split("\n");
    String[] headers = lines[0].split(",");
    String[] descriptions = lines[1].split(",");

    for (int i = 2; i < headers.length; i++) {
      assignments.add(new Assignment(headers[i], descriptions[i]));
    }

    for (int i = 2; i < lines.length; i++) {
      String splitline = lines[i].split(",");
      Student newStudent = new Student(splitline[0], splitline[1]);
      for (int j = 0; j < assignments.length; j++) {
        if (!splitline[j+2].empty()) {
          newStudent.submissions.add(assignments[j].name, new Submission(splitline[j+2]));
        }
      }
    }
  }

  public int save_csv(String path) {
    // String newfiletext = "some loading of new csv";
    if (!newfiletext == filetext) {
      return 1;
    }

    StringBuilder sb = new StringBuilder();

    // line 1 (header)
    sb.append("UUID,Name");
    for (Assignment a : assignments) {
      sb.append(',').append(a.name);
    }
    sb.append('\n');

    // line 2 (descriptions)
    sb.append("UUID of student,Most recent username");
    for (Assignment a : assignments) {
      sb.append(',').append(a.description);
    }
    sb.append('\n');

    // lines 3+ (student infos)
    for (Student s : students) {
      sb.append(s.uuid.toString()).append(',').append(s.name);
      for (Assignment a : assignments) {
        sb.append(',');
        if (s.submisisons.contains(a.name)) {
          sb.append(s.submissions.get(a.name).toString());
        }
      }
    }

    // do some shit to save the file
  }


}


class Assignment {

  String name;
  String description;

  Assignment(String name, String desc) {
    this.name = name;
    this.description = desc;
  }

}

class Student {

  UUID uuid;
  String name;
  HashMap<String, Submission> submissions;

  Student(UUID uuid, String name) {
    this.uuid = uuid;
    this.name = name;
    submissions = new HashMap<String, Submission>();
  }

  Student(String uuid, String name) {
    Student(new UUID(uuid), name);
  }

}

class Submission {

  Location location;
  String grade;

  Submission(String infostr) {
    String locstr = infostr.split("|")[0];
    String gradestr = infostr.split("|")[1];
    String[] xyzyp = locstr.split(":");
    this.location = new Location(
      Integer.parseInt(xyzyp[0]),
      Integer.parseInt(xyzyp[1]),
      Integer.parseInt(xyzyp[2]),
      Integer.parseInt(xyzyp[3]),
      Integer.parseInt(xyzyp[4]),
    )
    this.grade = gradestr;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.valueOf((int) location.getX())).append(':');
    sb.append(String.valueOf((int) location.getY())).append(':');
    sb.append(String.valueOf((int) location.getZ())).append(':');
    sb.append(String.valueOf((int) location.getYaw())).append(':');
    sb.append(String.valueOf((int) location.getPitch()));

    sb.append('|');
    sb.append(grade);
    return sb.toString();
  }

}