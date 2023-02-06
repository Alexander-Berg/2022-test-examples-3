package ru.yandex.market.delivery.gruzin.client.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Accessors(chain = true)
@ConfigurationProperties("gruzin.api")
public class GruzinApiProperties {
    private String url;
}
