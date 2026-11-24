package cn.interview.sealcard.service;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {
  String value();

  boolean success() default true;
}
