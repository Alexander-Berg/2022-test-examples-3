package ru.yandex.travel.api.config.common;

import java.time.Duration;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import ru.yandex.travel.orders.client.FailureDetectorProperties;
import ru.yandex.travel.orders.client.GrpcChannelSupplierMode;
import ru.yandex.travel.orders.client.GrpcChannelSupplierProperties;
import ru.yandex.travel.orders.client.yp.YpDiscoveryProperties;

@ConfigurationProperties(value = "test-context", ignoreUnknownFields = false)
@Getter
@Setter
@Validated
public class TestContextConfigurationProperties {
    private boolean enabled;
    @Valid
    private SearcherClientProperties searcher;

    private String urlPrefix;

    @Valid
    @Getter
    @Setter
    public static class SearcherClientProperties implements GrpcChannelSupplierProperties {
        @Valid
        @NotNull
        private GrpcChannelSupplierMode mode;

        private List<String> targets;

        @Valid
        private YpDiscoveryProperties yp;

        @Valid
        private FailureDetectorProperties failureDetection;

        @NotNull
        private Duration timeout;

        @Positive
        private int maxMessageSize;
    }
}
