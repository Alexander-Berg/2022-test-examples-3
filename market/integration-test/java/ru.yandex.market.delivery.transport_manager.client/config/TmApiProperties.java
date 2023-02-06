package ru.yandex.market.delivery.transport_manager.client.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@Accessors(chain = true)
@ConfigurationProperties("tm.api")
public class TmApiProperties {
    private String url;
}
