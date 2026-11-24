package cn.interview.sealcard.mapper;

import java.util.*;

/** SQL fragments originate exclusively in the repository/service, never from HTTP values. */
public class BusinessSql {
  private static final Set<String> TABLES =
      new HashSet<>(
          Arrays.asList(
              "seal_card",
              "seal_card_version",
              "seal_card_account",
              "seal_card_image",
              "seal_card_audit_log",
              "seal_upload_task"));

  private String table(Map<String, Object> p) {
    String t = (String) p.get("table");
    if (!TABLES.contains(t)) throw new IllegalArgumentException("Invalid table");
    return t;
  }

  private String col(String s) {
    if (!s.matches("[a-z][a-z0-9_]*") || s.equals("active_card_id"))
      throw new IllegalArgumentException("Invalid column");
    return "`" + s + "`";
  }

  private String fragment(Object s) {
    String v = s == null ? "" : s.toString();
    if (v.contains(";") || v.contains("--") || v.contains("/*"))
      throw new IllegalArgumentException("Invalid SQL fragment");
    return v;
  }

  public String count(Map<String, Object> p) {
    return "SELECT COUNT(*) FROM " + table(p) + " WHERE " + fragment(p.get("where"));
  }

  public String select(Map<String, Object> p) {
    return "SELECT * FROM "
        + table(p)
        + " WHERE "
        + fragment(p.get("where"))
        + " "
        + fragment(p.get("suffix"));
  }

  @SuppressWarnings("unchecked")
  public String insert(Map<String, Object> p) {
    Map<String, Object> row = (Map<String, Object>) p.get("row");
    StringJoiner cols = new StringJoiner(","), values = new StringJoiner(",");
    for (String k : row.keySet()) {
      cols.add(col(k));
      values.add("#{row." + k + "}");
    }
    return "INSERT INTO " + table(p) + " (" + cols + ") VALUES (" + values + ")";
  }

  @SuppressWarnings("unchecked")
  public String update(Map<String, Object> p) {
    Map<String, Object> row = (Map<String, Object>) p.get("row");
    StringJoiner set = new StringJoiner(",");
    for (String k : row.keySet()) set.add(col(k) + "=#{row." + k + "}");
    return "UPDATE " + table(p) + " SET " + set + " WHERE " + fragment(p.get("where"));
  }
}
