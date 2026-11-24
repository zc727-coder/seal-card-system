package cn.interview.sealcard.adapter;

import java.io.IOException;

public interface ImageStorage {
  String stage(byte[] bytes) throws IOException;

  byte[] readStage(String objectId) throws IOException;

  String promote(String stagedId, String finalId, String expectedSha) throws IOException;

  byte[] read(String objectId) throws IOException;

  java.io.InputStream open(String objectId) throws IOException;

  boolean ready();

  void cleanupUnreferenced(java.util.Set<String> referenced, long olderThanMillis)
      throws IOException;
}
