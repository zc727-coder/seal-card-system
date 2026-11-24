package cn.interview.sealcard.adapter;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 本地共享持久卷替代适配器，禁止Nginx直接映射该目录。 */
@Component
public class LocalPrivateImageStorage implements ImageStorage {
  private final Path root, staging, objects;
  private volatile boolean available = true;
  private volatile boolean promotionFailure = false;

  public LocalPrivateImageStorage(@Value("${seal.storage-root}") String path) throws IOException {
    root = Paths.get(path).toAbsolutePath().normalize();
    staging = root.resolve("staging");
    objects = root.resolve("objects");
    Files.createDirectories(staging);
    Files.createDirectories(objects);
  }

  public void setPromotionFailureForTest(boolean value) {
    promotionFailure = value;
  }

  public void setAvailableForTest(boolean value) {
    available = value;
  }

  private void available() throws IOException {
    if (!available) throw new IOException("Image platform unavailable");
  }

  private Path safe(Path dir, String id) throws IOException {
    if (id == null || !id.matches("[a-zA-Z0-9_-]{1,120}"))
      throw new IOException("Invalid object id");
    return dir.resolve(id);
  }

  public static String hash(byte[] b) {
    try {
      StringBuilder s = new StringBuilder();
      for (byte x : MessageDigest.getInstance("SHA-256").digest(b))
        s.append(String.format("%02x", x & 255));
      return s.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String stage(byte[] bytes) throws IOException {
    available();
    String id = UUID.randomUUID().toString();
    Files.write(safe(staging, id), bytes, StandardOpenOption.CREATE_NEW);
    return id;
  }

  public byte[] readStage(String id) throws IOException {
    available();
    return Files.readAllBytes(safe(staging, id));
  }

  public String promote(String stagedId, String id, String sha) throws IOException {
    available();
    if (promotionFailure) throw new IOException("Simulated promotion failure");
    Path target = safe(objects, id);
    if (Files.exists(target)) {
      if (!sha.equals(hash(Files.readAllBytes(target))))
        throw new IOException("Image integrity mismatch");
      return id;
    }
    byte[] data = readStage(stagedId);
    if (!sha.equals(hash(data))) throw new IOException("Staged image integrity mismatch");
    Path temp = Files.createTempFile(objects, "pending-", ".tmp");
    try {
      Files.write(temp, data);
      try {
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temp);
    }
    return id;
  }

  public InputStream open(String id) throws IOException {
    available();
    return Files.newInputStream(safe(objects, id));
  }

  public byte[] read(String id) throws IOException {
    available();
    return Files.readAllBytes(safe(objects, id));
  }

  public boolean ready() {
    return available && Files.isWritable(root);
  }

  public void cleanupUnreferenced(Set<String> referenced, long older) throws IOException {
    available();
    for (Path dir : Arrays.asList(staging, objects)) {
      try (java.util.stream.Stream<Path> paths = Files.list(dir)) {
        for (Path p : (Iterable<Path>) paths::iterator) {
          if (Files.isRegularFile(p)
              && !referenced.contains(p.getFileName().toString())
              && Files.getLastModifiedTime(p).toMillis() < older) Files.deleteIfExists(p);
        }
      }
    }
  }
}
