package ru.yandex.market.api.partner.controllers.util.request;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.text.StrSubstitutor;

import ru.yandex.market.api.partner.context.Format;

public class OrderRequestBuilder {
    private String url;
    private String urlBasePrefix;
    private Long uid;
    private Map<String, Object> substitutions = new HashMap<>();

    private OrderRequestBuilder() {
    }

    public OrderRequestBuilder url(@Nonnull String url) {
        this.url = url;
        return this;
    }

    public OrderRequestBuilder urlBasePrefix(@Nonnull String urlBasePrefix) {
        this.urlBasePrefix = urlBasePrefix;
        return this;
    }

    public OrderRequestBuilder campaignId(long campaignId) {
        substitutions.put("campaignId", campaignId);
        return this;
    }

    public OrderRequestBuilder orderId(@Nonnull Long orderId) {
        substitutions.put("orderId", orderId);
        return this;
    }

    public OrderRequestBuilder format(@Nonnull Format format) {
        substitutions.put("format", format.formatName());
        return this;
    }

    public OrderRequestBuilder uid(@Nullable Long uid) {
        this.uid = uid;
        return this;
    }

    public boolean hasUid() {
        return substitutions.containsKey("uid");
    }

    public Long getUid() {
        return uid;
    }

    @Nonnull
    public String build() {
        final StrSubstitutor substitute = new StrSubstitutor(substitutions, "{", "}");
        return urlBasePrefix + substitute.replace(url);
    }

    public static OrderRequestBuilder builder() {
        return new OrderRequestBuilder();
    }
}
