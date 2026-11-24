package cn.interview.sealcard.service;

import cn.interview.sealcard.mapper.BusinessMapper;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class BusinessStore {
  private final BusinessMapper mapper;

  public BusinessStore(BusinessMapper mapper) {
    this.mapper = mapper;
  }

  public static Map<String, Object> m(Object... p) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < p.length; i += 2) m.put((String) p[i], p[i + 1]);
    return m;
  }

  public List<Map<String, Object>> rows(
      String table, String where, String suffix, Map<String, Object> args) {
    return mapper.select(table, where, suffix, args);
  }

  public List<Map<String, Object>> rows(String table, String where, Map<String, Object> args) {
    return rows(table, where, "", args);
  }

  public Map<String, Object> one(String table, String where, Map<String, Object> args) {
    List<Map<String, Object>> l = rows(table, where, "LIMIT 1", args);
    return l.isEmpty() ? null : l.get(0);
  }

  public Map<String, Object> lockCard(long id) {
    List<Map<String, Object>> l = rows("seal_card", "id=#{args.id}", "FOR UPDATE", m("id", id));
    return l.isEmpty() ? null : l.get(0);
  }

  public long count(String table, String where, Map<String, Object> args) {
    return mapper.count(table, where, args);
  }

  public long insert(String table, Map<String, Object> row) {
    mapper.insert(table, row);
    return ((Number) row.get("id")).longValue();
  }

  public int update(String table, Map<String, Object> row, String where, Map<String, Object> args) {
    return mapper.update(table, row, where, args);
  }

  public static long id(Map<String, Object> row, String key) {
    Object o = row.get(key);
    return o == null ? 0 : ((Number) o).longValue();
  }

  public static String text(Map<String, Object> row, String key) {
    Object o = row.get(key);
    return o == null ? null : o.toString();
  }
}
