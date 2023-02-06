package ru.yandex.market.delivery.deliveryintegrationtests.wms.dto;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class Item {
    String sku;
    String serialNumber;
    long vendorId;
    @NonNull String article;
    String name;
    boolean shelfLife;
    String expDate;
    String creationDate;
    String toExpireDaysQuantity;
    int checkImei;
    int checkSn;
    @Builder.Default
    int checkCis = -1;
    @Builder.Default
    Set<AnomalyType> anomalyTypes = Collections.emptySet();
    @Builder.Default
    int quantity = 1;
    @Builder.Default
    Map<String, String> instances = Collections.emptyMap();
    @Builder.Default
    boolean removableIfAbsent = false;
    @Builder.Default
    boolean hasDuplicates = false;
}
