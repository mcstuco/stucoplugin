package stucoplugin;

import java.sql.*;

public class DBConnect {

  Connection conn;

  public DBConnect(String path) {
    try {
      String url = "jdbc:sqlite:" + path;
      conn = DriverManager.getConnection(url);
    } catch (SQLException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Failed to connect to database: " + path);
    }
  }

  public void close() {
    try {
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public Query queryDB(String sql, Object... args) throws SQLException {
    Query q = new Query(conn, sql);
    for (int i = 0; i < args.length; i++) {
      Object o = args[i];
      if (o instanceof Integer) {
        q.setInt(i+1, (Integer) o);
      } else if (o instanceof String) {
        q.setString(i+1, (String) o);
      } else if (o instanceof Boolean) {
        q.setBoolean(i+1, (Boolean) o);
      } else {
        q.close();
        throw new IllegalArgumentException("Query argument type not recognized!");
      }
    }
    q.execute();
    return q;
  }

  public int updateDB(String sql, Object... args) throws SQLException {
    PreparedStatement pstmt = conn.prepareStatement(sql);
    for (int i = 0; i < args.length; i++) {
      Object o = args[i];
      if (o instanceof Integer) {
        pstmt.setInt(i+1, (Integer) o);
      } else if (o instanceof String) {
        pstmt.setString(i+1, (String) o);
      } else if (o instanceof Long) {
        pstmt.setString(i+1, (new Timestamp((Long) o)).toString());
      } else {
        pstmt.close();
        throw new IllegalArgumentException("Query argument type not recognized!");
      }
    }
    pstmt.setQueryTimeout(1);
    int res = pstmt.executeUpdate();
    pstmt.close();
    return res;
  }

  public class Query {

    PreparedStatement pstmt;
    ResultSet rs;

    public Query(Connection conn, String sql) throws SQLException {
      pstmt = conn.prepareStatement(sql);
    }

    public void setInt(int i, int o) throws SQLException {pstmt.setInt(i, o);}
    public void setString(int i, String o) throws SQLException {pstmt.setString(i, o);}
    public void setBoolean(int i, Boolean o) throws SQLException {pstmt.setBoolean(i, o);}

    public void execute() throws SQLException {
      pstmt.setQueryTimeout(1);
      rs = pstmt.executeQuery();
    }

    public boolean next() throws SQLException {
      return rs.next();
    }

    public int getInt(String name) throws SQLException {return rs.getInt(name);}
    public String getString(String name) throws SQLException {return rs.getString(name);}

    public void close() throws SQLException {
      if (rs != null) rs.close();
      pstmt.close();
    }

  }

}