package ru.yandex.market.delivery.deliveryintegrationtests.wms.client.request;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@Builder
public class DatacreatorGetItemRequest {
    String storer;
    String manufacturerSku;
    String lot;
    String loc;
    String serialNumber;
}
