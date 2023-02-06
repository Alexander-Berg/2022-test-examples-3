package ru.yandex.market.partner.mvc.controller.delivery.region;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.delivery.region_blacklist.dao.DeliveryRegionBlacklistYtDao;
import ru.yandex.market.core.delivery.region_blacklist.model.DeliveryRegionBlacklist;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

public class DeliveryRegionControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private DeliveryRegionBlacklistYtDao deliveryRegionBlacklistYtDao;

    @Autowired
    private LMSClient lmsClient;

    private static Stream<Arguments> getRegionalDeliverySettingsData() {
        return Stream.of(
                Arguments.of(775L, List.of(), "DeliveryRegionController_testExcludedRegionsAreAbsent.response.json"),
                Arguments.of(
                        774L,
                        List.of(
                                DeliveryRegionBlacklist.builder()
                                        .setPartnerId(774L)
                                        .setUpdatedAt(new Date().toInstant())
                                        .setRegions(List.of(59L, 10645L))
                                        .setWarehouseId(1L)
                                        .build()
                        ),
                        "DeliveryRegionController_testGetRegionalDeliverySettings.response.json"
                )
        );
    }

    @ParameterizedTest(name = "{index}: {2}")
    @DbUnitDataSet(before = "csv/DeliveryRegionController_testGetRegionalDeliverySettings.before.csv")
    @MethodSource("getRegionalDeliverySettingsData")
    void testGetRegionalDeliverySettings(long shopId, List<DeliveryRegionBlacklist> blacklists, String responsePath) {
        int warehouseLocationId = 59;

        Mockito.when(deliveryRegionBlacklistYtDao.getPartnerDeliveryRegionBlacklists(anyLong()))
                .thenReturn(blacklists);

        Optional<BusinessWarehouseResponse> getBusinessWarehouseForPartnerResponse =
                Optional.of(BusinessWarehouseResponse.newBuilder()
                        .address(Address.newBuilder().locationId(warehouseLocationId).build())
                        .build());
        when(lmsClient.getBusinessWarehouseForPartner(any())).thenReturn(getBusinessWarehouseForPartnerResponse);

        ResponseEntity<String> response = FunctionalTestHelper.get(getUrl(shopId));
        JsonTestUtil.assertEquals(response, this.getClass(), responsePath);

        Mockito.verify(deliveryRegionBlacklistYtDao).getPartnerDeliveryRegionBlacklists(shopId);
        Mockito.verifyNoMoreInteractions(deliveryRegionBlacklistYtDao);
    }

    @Test
    @DbUnitDataSet(before = "csv/DeliveryRegionController_testPutDeliveryRegionSettings.before.csv")
    void testPutDeliveryRegionSettings() {
        long shopId = 775L;

        ResponseEntity<String> response = FunctionalTestHelper.put(getUrl(shopId),
                JsonTestUtil.getJsonHttpEntity("{" +
                        "\"regions\": [" +
                        "{\"regionId\": 3, \"hasDelivery\": true}," +
                        "{\"regionId\": 52, \"hasDelivery\": false}," +
                        "{\"regionId\": 59, \"hasDelivery\": false}," +
                        "{\"regionId\": 73, \"hasDelivery\": true}" +
                        "]}"));

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        Mockito.verify(deliveryRegionBlacklistYtDao)
                .setPartnerDeliveryRegionBlacklist(argThat(blacklist ->
                        blacklist.getPartnerId() == 775 &&
                                blacklist.getWarehouseId() == 17 &&
                                blacklist.getRegions().size() == 2 &&
                                new HashSet<>(blacklist.getRegions()).equals(Set.of(52L, 59L))));
        Mockito.verifyNoMoreInteractions(deliveryRegionBlacklistYtDao);
    }

    @Test
    @DbUnitDataSet(before = "csv/DeliveryRegionController_testPutDeliveryRegionSettings.before.csv")
    void testDeleteRegionsToExclude() {
        long shopId = 775L;

        ResponseEntity<String> response = FunctionalTestHelper.put(getUrl(shopId),
                JsonTestUtil.getJsonHttpEntity("{" +
                        "\"regions\": [" +
                        "{\"regionId\": 3, \"hasDelivery\": true}," +
                        "{\"regionId\": 52, \"hasDelivery\": true}," +
                        "{\"regionId\": 59, \"hasDelivery\": true}," +
                        "{\"regionId\": 73, \"hasDelivery\": true}" +
                        "]}"));

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());

        Mockito.verify(deliveryRegionBlacklistYtDao).deleteDeliveryRegionBlacklist(shopId, 17);
        Mockito.verifyNoMoreInteractions(deliveryRegionBlacklistYtDao);
    }

    private static Stream<Arguments> badRequestData() {
        return Stream.of(
                Arguments.of(
                        "{" +
                                "\"regions\": [" +
                                "{\"regionId\": 3, \"hasDelivery\": false}," +
                                "{\"regionId\": 1, \"hasDelivery\": false}" +
                                "]}",
                        "Could not exclude non federal districts"
                ),
                Arguments.of(
                        "{" +
                                "\"settings\": {}," +
                                "\"regions\": [" +
                                "{\"regionId\": 3, \"hasDelivery\": false}," +
                                "{\"regionId\": 52, \"hasDelivery\": false}," +
                                "{\"regionId\": 59, \"hasDelivery\": false}," +
                                "{\"regionId\": 73, \"hasDelivery\": false}" +
                                "]}",
                        "Could not exclude all federal districts"
                )
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("badRequestData")
    @DbUnitDataSet(before = "csv/DeliveryRegionController_testPutDeliveryRegionSettings.before.csv")
    void testInvalidData(String request, String expectedErrorMessage) {
        long shopId = 775L;

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(getUrl(shopId),
                        JsonTestUtil.getJsonHttpEntity(request))
        );
        Assertions.assertEquals(exception.getStatusCode(), HttpStatus.BAD_REQUEST);
        String errorMessage = JsonTestUtil.parseJson(exception.getResponseBodyAsString()).getAsJsonObject()
                .get("errors").getAsJsonArray().get(0).getAsJsonObject()
                .get("message").getAsString();
        Assertions.assertEquals(expectedErrorMessage, errorMessage);
        Mockito.verifyNoInteractions(deliveryRegionBlacklistYtDao);
    }

    private String getUrl(long shopId) {
        return String.format("%s/delivery/region/settings?shopId=%d&format=json", baseUrl, shopId);
    }

}
