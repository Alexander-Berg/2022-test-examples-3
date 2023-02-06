package ru.yandex.market.logistics.util.client.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ResourceId {

    private final String yandexId;

    private final String partnerId;

    @JsonCreator
    public ResourceId(@JsonProperty("yandexId") String yandexId,
                      @JsonProperty("partnerId") String partnerId) {
        this.yandexId = yandexId;
        this.partnerId = partnerId;
    }

    public String getYandexId() {
        return yandexId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    @Override
    public String toString() {
        return "ResourceId{" +
            "yandexId='" + yandexId + '\'' +
            ", partnerId='" + partnerId + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceId that = (ResourceId) o;
        return Objects.equals(yandexId, that.yandexId) &&
            Objects.equals(partnerId, that.partnerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(yandexId, partnerId);
    }
}
