package ru.yandex.market.antifraud.orders.storage.entity.configuration;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import ru.yandex.market.antifraud.orders.service.exceptions.InvalidRequestException;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

/**
 * @author dzvyagin
 */
@Value
@Builder
@JsonDeserialize(builder = TestUidsMockConfig.TestUidsMockConfigBuilder.class)
public class TestUidsMockConfig {

    public Set<String> endpoints;
    public Boolean enabled;

    public static TestUidsMockConfig disabled() {
        return TestUidsMockConfig.builder()
                .enabled(false)
                .endpoints(Set.of())
                .build();
    }

    public static void validateJson(JsonNode json) {
        try {
            TestUidsMockConfig value = AntifraudJsonUtil.fromJson(json, TestUidsMockConfig.class);
            if (value == null) {
                throw new InvalidRequestException("Not valid class " + TestUidsMockConfig.class.getName() +": " + json);
            }
            if (value.enabled == null) {
                throw new InvalidRequestException("Enabled property is empty: " + json);
            }
            if (value.endpoints == null) {
                throw new InvalidRequestException("Endpoints property is empty: " + json);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
