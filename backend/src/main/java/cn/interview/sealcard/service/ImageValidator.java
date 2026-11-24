package cn.interview.sealcard.service;

import static cn.interview.sealcard.service.BusinessStore.*;

import cn.interview.sealcard.adapter.*;
import cn.interview.sealcard.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageValidator {
  private final int maxPages;
  private final long maxBytes, maxPixels;
  private final ImageStorage storage;

  public ImageValidator(
      @Value("${seal.max-pages}") int pages,
      @Value("${seal.max-file-bytes}") long bytes,
      @Value("${seal.max-image-pixels}") long pixels,
      ImageStorage storage) {
    maxPages = pages;
    maxBytes = bytes;
    maxPixels = pixels;
    this.storage = storage;
  }

  public List<Map<String, Object>> stage(List<MultipartFile> files) {
    if (files == null || files.isEmpty() || files.size() > maxPages)
      throw BusinessException.bad("须上传1至" + maxPages + "页影像");
    List<Map<String, Object>> out = new ArrayList<>();
    int page = 0;
    for (MultipartFile f : files) {
      if (f.isEmpty() || f.getSize() > maxBytes)
        throw new BusinessException(413, "FILE_TOO_LARGE", "每页影像须非空且不超过10MB");
      try {
        byte[] data = f.getBytes();
        String name =
            f.getOriginalFilename() == null ? "" : f.getOriginalFilename().toLowerCase(Locale.ROOT);
        String mime;
        if (data.length >= 8
            && (data[0] & 255) == 137
            && data[1] == 80
            && data[2] == 78
            && data[3] == 71
            && data[4] == 13
            && data[5] == 10
            && data[6] == 26
            && data[7] == 10) {
          mime = "image/png";
          if (!name.endsWith(".png")) throw invalid();
        } else if (data.length >= 3
            && (data[0] & 255) == 255
            && (data[1] & 255) == 216
            && (data[2] & 255) == 255) {
          mime = "image/jpeg";
          if (!name.endsWith(".jpg") && !name.endsWith(".jpeg")) throw invalid();
        } else throw invalid();
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
          Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
          if (!readers.hasNext()) throw invalid();
          ImageReader reader = readers.next();
          try {
            reader.setInput(in, true, true);
            int width = reader.getWidth(0), height = reader.getHeight(0);
            if (width < 32 || height < 32 || (long) width * height > maxPixels)
              throw new BusinessException(400, "IMAGE_DIMENSIONS", "影像尺寸过小或像素数超过演示上限");
            BufferedImage image = reader.read(0);
            if (image == null) throw invalid();
          } finally {
            reader.dispose();
          }
        }
        String staged;
        try {
          staged = storage.stage(data);
        } catch (IOException storageFailure) {
          throw new BusinessException(503, "IMAGE_STORAGE_UNAVAILABLE", "影像暂存失败，请稍后重试");
        }
        out.add(
            m(
                "pageNo",
                ++page,
                "stagedId",
                staged,
                "sha256",
                LocalPrivateImageStorage.hash(data),
                "mimeType",
                mime,
                "fileSize",
                data.length));
      } catch (BusinessException e) {
        throw e;
      } catch (IOException e) {
        throw invalid();
      }
    }
    return out;
  }

  private BusinessException invalid() {
    return new BusinessException(415, "INVALID_IMAGE", "只允许扩展名和实际内容一致且可解码的PNG/JPEG影像");
  }
}
