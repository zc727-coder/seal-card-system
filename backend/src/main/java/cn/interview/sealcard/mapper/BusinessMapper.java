package cn.interview.sealcard.mapper;

import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BusinessMapper {
  @SelectProvider(type = BusinessSql.class, method = "select")
  List<Map<String, Object>> select(
      @Param("table") String table,
      @Param("where") String where,
      @Param("suffix") String suffix,
      @Param("args") Map<String, Object> args);

  @SelectProvider(type = BusinessSql.class, method = "count")
  long count(
      @Param("table") String table,
      @Param("where") String where,
      @Param("args") Map<String, Object> args);

  @InsertProvider(type = BusinessSql.class, method = "insert")
  @Options(useGeneratedKeys = true, keyProperty = "row.id")
  int insert(@Param("table") String table, @Param("row") Map<String, Object> row);

  @UpdateProvider(type = BusinessSql.class, method = "update")
  int update(
      @Param("table") String table,
      @Param("row") Map<String, Object> row,
      @Param("where") String where,
      @Param("args") Map<String, Object> args);
}
