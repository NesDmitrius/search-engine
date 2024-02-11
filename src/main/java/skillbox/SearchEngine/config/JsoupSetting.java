package skillbox.SearchEngine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Data
@Component
@ConfigurationProperties(prefix = "jsoup-setting")
public class JsoupSetting {

    private String userAgent;

    private String referrer;

}
