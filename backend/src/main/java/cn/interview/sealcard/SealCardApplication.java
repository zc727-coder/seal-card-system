package cn.interview.sealcard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SealCardApplication {
  public static void main(String[] args) {
    SpringApplication.run(SealCardApplication.class, args);
  }
}
