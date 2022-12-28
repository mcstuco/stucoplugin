package stucoplugin;

import java.util.ArrayList;
import java.util.UUID;
import java.util.HashMap;

import org.bukkit.Location;

public class Data {

  private ArrayList<Assignment> assignments;
  private ArrayList<Student> students;

  public Data {
    assignments = new ArrayList<Assignment>();
    students = new ArrayList<Student>();
  }

  public void load_from_csv(String path) {

  }


}


class Assignment {

  private String name;
  private STring description;

}

class Student {

  private String name;
  private UUID uuid;
  private HashMap<String, Location> submissions;

}