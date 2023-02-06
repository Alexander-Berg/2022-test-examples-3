package ru.yandex.market.loyalty.core.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author <a href="mailto:maratik@yandex-team.ru">Marat Bukharov</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JugglerEventView {
    @JsonProperty("service")
    String service;
    @JsonProperty("status")
    String status;


    public String getService() {
        return service;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "JugglerEventView{" +
                "service='" + service + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
