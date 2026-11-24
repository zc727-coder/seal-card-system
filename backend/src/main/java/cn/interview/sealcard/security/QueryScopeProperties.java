package cn.interview.sealcard.security;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 可信服务端配置，不接受HTTP请求头、Cookie角色字段或请求参数修改。 */
@Component
@ConfigurationProperties(prefix = "seal.access")
public class QueryScopeProperties {
  private Map<String, List<String>> queryScopes = new ConcurrentHashMap<>();

  public Map<String, List<String>> getQueryScopes() {
    return queryScopes;
  }

  public void setQueryScopes(Map<String, List<String>> queryScopes) {
    this.queryScopes = new ConcurrentHashMap<>(queryScopes);
  }
}
