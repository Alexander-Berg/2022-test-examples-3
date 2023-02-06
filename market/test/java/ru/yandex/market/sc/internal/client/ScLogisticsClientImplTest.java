package ru.yandex.market.sc.internal.client;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockserver.model.MediaType;

import ru.yandex.market.sc.internal.model.DropOffDto;
import ru.yandex.market.sc.internal.model.InventoryItemDto;
import ru.yandex.market.sc.internal.model.InventoryItemPlaceDto;
import ru.yandex.market.sc.internal.model.InventoryItemPlaceStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class ScLogisticsClientImplTest extends ClientTest {

    @Test
    void createDropOff() {
        var dto = DropOffDto.builder()
                .deliveryPartnerId(758319)
                .address("г. Москва, ул. Профсоюзная, д.128")
                .apiToken("3cefef9b51354dc9a4a7f0fe4754c5c1534b754f087e4038a3f826563b24d76c")
                .campaignId("358591")
                .partnerName("ИП Курочкин Иван Федорович")
                .dropOffName("ПВЗ Колотушкина на улице Пушкина")
                .courierDeliveryServiceId(258317)
                .build();

        String json = "{" +
                "\"deliveryPartnerId\":" + dto.getDeliveryPartnerId() + "," +
                "\"address\":\"" + dto.getAddress() + "\"," +
                "\"apiToken\":\"" + dto.getApiToken() + "\"," +
                "\"campaignId\":\"" + dto.getCampaignId() + "\"," +
                "\"partnerName\":\"" + dto.getPartnerName() + "\"," +
                "\"dropOffName\":\"" + dto.getDropOffName() + "\"," +
                "\"courierDeliveryServiceId\": " + dto.getCourierDeliveryServiceId() + "" +
                "}";
        mockServer
                .when(
                        request()
                                .withMethod("POST")
                                .withPath("/v1/logistics/sorting-centers/drop-off")
                                .withContentType(MediaType.APPLICATION_JSON)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(json, StandardCharsets.UTF_8)
                );


        var actual = scLogisticsClient.createSortingCenterAsDropOff(dto);

        assertThat(actual).isEqualTo(dto);
    }

    @Test
    void findInventoryItemByBarcode() {
        var externalId = "123";
        var placeCode1 = "AAA";
        var placeCode2 = "BBB";
        var declaredCost = BigDecimal.valueOf(2.55);
        long courierId = 123;

        var dto =
                InventoryItemDto.builder()
                        .externalId(externalId)
                        .declaredCost(declaredCost)
                        .courierId(courierId)
                        .placeCodes(List.of(placeCode1, placeCode2))
                        .places(List.of(
                                new InventoryItemPlaceDto(placeCode1, InventoryItemPlaceStatus.ACCEPTED),
                                new InventoryItemPlaceDto(placeCode2, InventoryItemPlaceStatus.CAN_ACCEPT))
                        )
                        .build();
        var json = "{" +
                "\"externalId\": \"" + externalId + "\"," +
                "\"declaredCost\": \"" + declaredCost + "\"," +
                "\"courierId\": " + courierId + "," +
                "\"placeCodes\": [\"" + placeCode1 + "\", \"" + placeCode2 + "\"]," +
                "\"places\": [{" +
                "\"barcode\": \"" + placeCode1 + "\", " + "\"status\":\"" + InventoryItemPlaceStatus.ACCEPTED + "\"}," +
                "{\"barcode\": \"" + placeCode2 + "\", " + "\"status\":\"" + InventoryItemPlaceStatus.CAN_ACCEPT +
                "\"}]" +
                "}";

        var sortCenterId = 1;

        mockServer
                .when(request()
                        .withMethod("GET")
                        .withPath("/v1/logistics/sorting-centers/" + sortCenterId + "/find-inventory-item-by-barcode")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(json, StandardCharsets.UTF_8)
                );

        var actual = scLogisticsClient.findInventoryItemByBarcode(sortCenterId, List.of(placeCode1));
        assertThat(actual).isEqualTo(dto);
    }

    @Test
    void acceptPlacesByBarcodes() {
        var externalId = "123";
        var placeCode1 = "AAA";
        var placeCode2 = "BBB";
        var declaredCost = BigDecimal.valueOf(2.55);
        long courierId = 123;

        var dto = List.of(
                InventoryItemDto.builder()
                        .externalId(externalId)
                        .declaredCost(declaredCost)
                        .courierId(courierId)
                        .placeCodes(List.of(placeCode1, placeCode2))
                        .places(List.of(
                                new InventoryItemPlaceDto(placeCode1, InventoryItemPlaceStatus.ACCEPTED),
                                new InventoryItemPlaceDto(placeCode2, InventoryItemPlaceStatus.ACCEPTED))
                        )
                        .build()
        );
        var json = "[{" +
                "\"externalId\": \"" + externalId + "\"," +
                "\"declaredCost\": \"" + declaredCost + "\"," +
                "\"courierId\": " + courierId + "," +
                "\"placeCodes\": [\"" + placeCode1 + "\", \"" + placeCode2 + "\"]," +
                "\"places\": [{" +
                "\"barcode\": \"" + placeCode1 + "\", " + "\"status\":\"" + InventoryItemPlaceStatus.ACCEPTED + "\"}," +
                "{\"barcode\": \"" + placeCode2 + "\", " + "\"status\":\"" + InventoryItemPlaceStatus.ACCEPTED +
                "\"}]" +
                "}]";
        var sortCenterId = 1;

        mockServer
                .when(request()
                        .withMethod("POST")
                        .withPath("/v1/logistics/sorting-centers/" + sortCenterId + "/cargo/accept-by-barcodes")
                )
                .respond(response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(json, StandardCharsets.UTF_8)
                );

        var actual = scLogisticsClient.acceptPlacesByBarcodes(sortCenterId, List.of(placeCode1));
        assertThat(actual).isEqualTo(dto);
    }

}
