package cn.interview.sealcard.configuration;

import java.lang.reflect.Field;
import java.util.*;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.builders.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.*;

/** Swagger2/Knife4j文档，仅描述实际Controller端点。 */
@Configuration
public class Knife4jConfig {
  @Bean
  public Docket docket() {
    return new Docket(DocumentationType.SWAGGER_2)
        .groupName("seal-card-v1")
        .apiInfo(
            new ApiInfoBuilder()
                .title("印鉴卡管理系统 API")
                .description("模拟银行环境；角色机构鉴权、版本复核及受控影像调阅。Cookie会话登录后可调试。平台主数据为显式演示适配器。")
                .version("1.0")
                .build())
        .useDefaultResponseMessages(false)
        .select()
        .apis(RequestHandlerSelectors.basePackage("cn.interview.sealcard.controller"))
        .paths(PathSelectors.regex("/api/v1/.*"))
        .build();
  }
  /** Springfox3不能解析Boot2.7 Actuator的PathPattern映射；只从文档扫描中过滤，不修改实际路由。 */
  @Bean
  public static BeanPostProcessor springfoxCompatibility() {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String name) {
        if (bean instanceof WebMvcRequestHandlerProvider) {
          Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
          if (field != null) {
            field.setAccessible(true);
            try {
              @SuppressWarnings("unchecked")
              List<RequestMappingInfoHandlerMapping> mappings =
                  (List<RequestMappingInfoHandlerMapping>) field.get(bean);
              mappings.removeIf(mapping -> mapping.getPatternParser() != null);
            } catch (IllegalAccessException e) {
              throw new IllegalStateException("Swagger mapping initialization failed", e);
            }
          }
        }
        return bean;
      }
    };
  }
}
