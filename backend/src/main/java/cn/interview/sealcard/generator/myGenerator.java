package cn.interview.sealcard.generator;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** 仅手动运行的基础代码生成器；不创建表、不实现业务接口、不覆盖已有文件。 */
public class myGenerator {
  private static final String[] TABLES = {
    "seal_card", "seal_card_version", "seal_card_account",
    "seal_card_image", "seal_card_audit_log", "seal_upload_task"
  };

  public static void main(String[] args) throws Exception {
    Path backend = findBackend(Paths.get(System.getProperty("user.dir")));
    boolean controllersOnly =
        java.util.Arrays.asList(args).contains("--controllers-only")
            || Files.exists(
                backend.resolve("src/main/java/cn/interview/sealcard/domain/SealCard.java"));
    System.out.println(
        controllersOnly
            ? "Generator mode: controllers only (existing domain/mapper/service files are preserved)."
            : "Generator mode: full scaffold (existing files will not be overwritten).");
    Map<String, String> env = readEnv(backend.getParent().resolve(".env"));
    String url =
        setting(
            "GENERATOR_DB_URL",
            "jdbc:mysql://127.0.0.1:"
                + setting("MYSQL_PORT", "3307", env)
                + "/seal_card?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
            env);
    String username = setting("GENERATOR_DB_USERNAME", "seal_app", env);
    String password = setting("GENERATOR_DB_PASSWORD", setting("MYSQL_PASSWORD", "", env), env);
    if (password.isEmpty()) {
      throw new IllegalStateException(
          "缺少数据库密码：配置项目根目录 .env 的 MYSQL_PASSWORD，或环境变量 GENERATOR_DB_PASSWORD。");
    }
    String author = setting("GENERATOR_AUTHOR", "zc", env);
    Path javaOutput = backend.resolve("src/main/java");
    Path xmlOutput = backend.resolve("src/main/resources/mapper");
    rejectExistingFiles(javaOutput, xmlOutput, controllersOnly);
    checkTables(url, username, password);

    FastAutoGenerator.create(url, username, password)
        .globalConfig(
            builder ->
                builder
                    .author(author)
                    .commentDate("yyyy-MM-dd")
                    .dateType(DateType.TIME_PACK)
                    .disableOpenDir()
                    .outputDir(javaOutput.toString()))
        .packageConfig(
            builder ->
                builder
                    .parent("cn.interview.sealcard")
                    .entity("domain")
                    .mapper("mapper")
                    .service("service")
                    .serviceImpl("service.impl")
                    .controller("controller")
                    .xml("mapper")
                    .pathInfo(Collections.singletonMap(OutputFile.xml, xmlOutput.toString())))
        // 使用MyBatis-Plus内置默认Controller模板。
        .templateConfig(
            builder -> {
              if (controllersOnly)
                builder.disable(
                    TemplateType.ENTITY,
                    TemplateType.MAPPER,
                    TemplateType.XML,
                    TemplateType.SERVICE,
                    TemplateType.SERVICEIMPL);
            })
        .strategyConfig(
            builder -> {
              builder.addInclude(TABLES);
              builder.controllerBuilder().enableRestStyle();
              builder
                  .entityBuilder()
                  .enableLombok()
                  .enableTableFieldAnnotation()
                  .idType(IdType.AUTO)
                  .versionColumnName("row_version")
                  // MySQL STORED GENERATED列不可由INSERT/UPDATE赋值。
                  .addIgnoreColumns("active_card_id");
              builder
                  .mapperBuilder()
                  .enableMapperAnnotation()
                  .enableBaseResultMap()
                  .enableBaseColumnList();
              builder
                  .serviceBuilder()
                  .formatServiceFileName("%sService")
                  .formatServiceImplFileName("%sServiceImpl");
            })
        .templateEngine(new VelocityTemplateEngine())
        .execute();
    System.out.println("基础代码生成完成：" + javaOutput + "；XML：" + xmlOutput);
    System.out.println("生成的CRUD骨架不代表已实现复核、权限、版本事务或追加审计规则。");
  }

  private static Path findBackend(Path start) {
    for (Path current = start.toAbsolutePath().normalize();
        current != null;
        current = current.getParent()) {
      if (Files.isRegularFile(current.resolve("pom.xml"))
          && Files.isDirectory(current.resolve("src/main/java"))) {
        return current;
      }
      Path child = current.resolve("backend");
      if (Files.isRegularFile(child.resolve("pom.xml"))
          && Files.isDirectory(child.resolve("src/main/java"))) {
        return child;
      }
    }
    throw new IllegalStateException("无法定位后端目录；请将IDEA运行配置的工作目录设为backend或项目根目录。");
  }

  private static Map<String, String> readEnv(Path file) throws IOException {
    Map<String, String> values = new HashMap<>();
    if (!Files.isRegularFile(file)) {
      return values;
    }
    for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
      String line = raw.replace("\uFEFF", "").trim();
      if (line.isEmpty() || line.startsWith("#")) continue;
      int separator = line.indexOf('=');
      if (separator <= 0) continue;
      String value = line.substring(separator + 1).trim();
      if (value.length() >= 2
          && ((value.startsWith("\"") && value.endsWith("\""))
              || (value.startsWith("'") && value.endsWith("'")))) {
        value = value.substring(1, value.length() - 1);
      }
      values.put(line.substring(0, separator).trim(), value);
    }
    return values;
  }

  private static String setting(String key, String fallback, Map<String, String> file) {
    String value = System.getenv(key);
    return value != null && !value.trim().isEmpty() ? value : file.getOrDefault(key, fallback);
  }

  private static void checkTables(String url, String username, String password) throws Exception {
    // 只读预检；失败时不会进入execute，也不会把密码打印到日志。
    try (Connection connection = DriverManager.getConnection(url, username, password);
        PreparedStatement statement =
            connection.prepareStatement(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=? AND table_type='BASE TABLE'")) {
      for (String table : TABLES) {
        statement.setString(1, table);
        try (ResultSet result = statement.executeQuery()) {
          if (!result.next() || result.getInt(1) != 1) {
            throw new IllegalStateException(
                "数据库中缺少表 " + table + "，请先在目标库执行 docs/sql/001_create_tables.sql。");
          }
        }
      }
    }
  }

  private static void rejectExistingFiles(
      Path javaOutput, Path xmlOutput, boolean controllersOnly) {
    for (String table : TABLES) {
      StringBuilder name = new StringBuilder();
      for (String part : table.split("_")) {
        name.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
      }
      String entity = name.toString();
      String base = "cn/interview/sealcard/";
      Path[] files = {
        javaOutput.resolve(base + "domain/" + entity + ".java"),
        javaOutput.resolve(base + "mapper/" + entity + "Mapper.java"),
        javaOutput.resolve(base + "service/" + entity + "Service.java"),
        javaOutput.resolve(base + "service/impl/" + entity + "ServiceImpl.java"),
        xmlOutput.resolve(entity + "Mapper.xml")
      };
      Path controller = javaOutput.resolve(base + "controller/" + entity + "Controller.java");
      if (Files.exists(controller))
        throw new IllegalStateException("Controller已存在，停止生成：" + controller);
      if (controllersOnly) continue;
      for (Path file : files) {
        if (Files.exists(file)) throw new IllegalStateException("已存在目标文件，停止生成以保护已有代码：" + file);
      }
    }
  }
}
