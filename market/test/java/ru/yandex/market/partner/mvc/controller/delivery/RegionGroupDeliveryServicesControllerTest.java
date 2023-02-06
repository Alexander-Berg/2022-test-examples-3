package ru.yandex.market.partner.mvc.controller.delivery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.cpa.CPADataPusher;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ActionDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.DeliveryModifierDto;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.modifier.ValueModificationRuleDto;
import ru.yandex.market.partner.mvc.controller.delivery.model.DeliveryServiceRequestDTO;
import ru.yandex.market.partner.mvc.controller.delivery.model.DeliveryServiceStrategyDTO;
import ru.yandex.market.partner.mvc.controller.delivery.model.ModifyDeliveryServicesRequestDTO;
import ru.yandex.market.partner.mvc.controller.delivery.model.SelectedDeliveryServiceDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Тесты для {@link RegionGroupDeliveryServicesController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RegionGroupDeliveryServicesControllerTest extends FunctionalTest {

    private static final long REGION_GROUP_ID = 1L;
    private static final long REGION_GROUP_ID_2 = 1002L;

    private static final long DELIVERY_SERVICE_ID = 1001L;
    private static final long DELIVERY_SERVICE_ID_2 = 1003L;
    private static final long OLD_DELIVERY_SERVICE_ID = 1002L;
    private static final long DATASOURCE_ID = 2001L;
    private static final long DATASOURCE_ID_2 = 774L;

    @Autowired
    CPADataPusher cpaDataPusher;

    @Autowired
    private WireMockServer tarifficatorWireMockServer;

    @BeforeEach
    void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());
    }

    @Test
    @DisplayName("Получение списка всех СД для магазина")
    void testGetAllDSForShopFromTarifficator() {
        mockTarifficatorShopResponse(DATASOURCE_ID);

        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl +
                "region-group/shop-delivery-services?shopId=2001&_user_id=123456");
        JsonTestUtil.assertEquals(response, "[1001, 1002, 1003]");
    }

    @Test
    @DisplayName("Получение списка СД региональной группы")
    @DbUnitDataSet(before = "csv/RegionGroupDeliveryServicesController.get_rgds.before.csv")
    void testGetDSofRGFromTarifficator() {
        mockTarifficatorRGResponse(DATASOURCE_ID_2, REGION_GROUP_ID_2, 1);

        ResponseEntity<String> response = FunctionalTestHelper.get(getRGDSUrl(REGION_GROUP_ID_2, DATASOURCE_ID_2));
        JsonTestUtil.assertEquals(response, getClass(),
                "json/RegionGroupDeliveryServicesController.get_rgds_tariff.json");
    }

    // Тест на фикс бага. Удалить в рамках https://st.yandex-team.ru/MBI-79051
    @Test
    @DisplayName("Получение списка СД региональной группы, пустые deliveryModifiers")
    @DbUnitDataSet(before = "csv/RegionGroupDeliveryServicesController.get_rgds.before.csv")
    void testGetDSofRGFromTarifficatorEmptyDeliveryModifiers() {
        mockTarifficatorRGResponse(DATASOURCE_ID_2, REGION_GROUP_ID_2, 2);

        ResponseEntity<String> response = FunctionalTestHelper.get(getRGDSUrl(REGION_GROUP_ID_2, DATASOURCE_ID_2));
        JsonTestUtil.assertEquals(response, getClass(),
                "json/RegionGroupDeliveryServicesController.get_rgds_tariff_2.json");
    }

    @Test
    @DisplayName("Обновление СД в региональной группе с правильными стратегиями ПВЗ и курьерки")
    @DbUnitDataSet(before = "csv/RegionGroupDeliveryServicesController.before.csv")
    void testUpdRGDSGoodStrategies() {
        mockTarifficatorSelectDeliveryServiceCall(DATASOURCE_ID, REGION_GROUP_ID, DELIVERY_SERVICE_ID,
                "region/tarifficator/selectDeliveryServiceRequest.json");

        ResponseEntity<String> response = FunctionalTestHelper.post(
                getUpdRGDSUrl(REGION_GROUP_ID, DELIVERY_SERVICE_ID, DATASOURCE_ID),
                new DeliveryServiceRequestDTO(
                        DeliveryServiceStrategyDTO.UNKNOWN_COST_TIME,
                        DeliveryServiceStrategyDTO.AUTO_CALCULATED)
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Обновление СД в региональной группе (батчевая версия). Создание, обновление, удаление")
    @DbUnitDataSet(before = "csv/RegionGroupDeliveryServicesController.before.csv")
    void testUpdRGDSCreateAndUpdateBatch() throws JsonProcessingException {
        mockTarifficatorModifyDeliveryServicesCall(DATASOURCE_ID, REGION_GROUP_ID,
                "region/tarifficator/modifyDeliveryServicesRequest.json");

        var update = List.of(
                // создастся
                new SelectedDeliveryServiceDTO(DELIVERY_SERVICE_ID, DeliveryServiceStrategyDTO.UNKNOWN_COST_TIME,
                        DeliveryServiceStrategyDTO.AUTO_CALCULATED),
                // обновится
                new SelectedDeliveryServiceDTO(DELIVERY_SERVICE_ID_2, DeliveryServiceStrategyDTO.AUTO_CALCULATED,
                        DeliveryServiceStrategyDTO.UNKNOWN_COST_TIME));
        var delete = Set.of(OLD_DELIVERY_SERVICE_ID);
        String body = OBJECT_MAPPER.writeValueAsString(new ModifyDeliveryServicesRequestDTO(update, delete));
        FunctionalTestHelper.post(getUpdRGDSBatchUrl(REGION_GROUP_ID, DATASOURCE_ID), body);

        verifyNoInteractions(cpaDataPusher);
    }

    @Test
    @DisplayName("Обновление СД в региональной группе. Изменение модификаторов")
    @DbUnitDataSet(before = "csv/RegionGroupDeliveryServicesController.before.csv")
    void testUpdRGDSUpdModifiers() throws JsonProcessingException {
        mockTarifficatorSelectDeliveryServiceCall(DATASOURCE_ID, REGION_GROUP_ID, OLD_DELIVERY_SERVICE_ID,
                "region/tarifficator/selectDeliveryServiceWithModifiersRequest.json");

        DeliveryServiceRequestDTO data = new DeliveryServiceRequestDTO(DeliveryServiceStrategyDTO.UNKNOWN_COST_TIME,
                DeliveryServiceStrategyDTO.AUTO_CALCULATED);
        List<DeliveryModifierDto> modifiers = List.of(
                new DeliveryModifierDto.Builder()
                        .withAction(new ActionDto.Builder()
                                .withTimeModificationRule(new ValueModificationRuleDto.Builder()
                                        .withOperation(ValueModificationRuleDto.OperationEnum.ADD)
                                        .withParameter(BigDecimal.valueOf(2))
                                        .build())
                                .build())
                        .withId(100L)
                        .withTimestamp(1583325200238L)
                        .build()
        );
        data.setCourierDeliveryModifiers(modifiers);

        String s = OBJECT_MAPPER.writeValueAsString(data);

        FunctionalTestHelper.post(getUpdRGDSUrl(REGION_GROUP_ID, OLD_DELIVERY_SERVICE_ID, DATASOURCE_ID), s);

        verifyNoInteractions(cpaDataPusher);
    }

    @Test
    @DisplayName("Удаление СД в региональной группе. Связь существует")
    @DbUnitDataSet(before = "csv/RegionGroupDeliveryServicesController.before.csv")
    void testDeleteRGDSExists() {
        mockTarifficatorDeleteDeliveryServiceCall(DATASOURCE_ID, REGION_GROUP_ID, OLD_DELIVERY_SERVICE_ID);

        FunctionalTestHelper.delete(getUpdRGDSUrl(REGION_GROUP_ID, OLD_DELIVERY_SERVICE_ID, DATASOURCE_ID));

        verifyNoInteractions(cpaDataPusher);
    }

    private String getRGDSUrl(long regionGroupId, long datasourceId) {
        return baseUrl + "region-group/" + regionGroupId + "/delivery-service?_user_id=123456&shopId=" + datasourceId;
    }

    private String getUpdRGDSUrl(long regionGroupId, long deliveryServiceId, long datasourceId) {
        return baseUrl + "region-group/" + regionGroupId + "/delivery-service/" + deliveryServiceId
                + "?datasource_id=" + datasourceId;
    }

    private String getUpdRGDSBatchUrl(long regionGroupId, long datasourceId) {
        return baseUrl + "region-group/" + regionGroupId + "/delivery-services"
                + "?datasource_id=" + datasourceId;
    }

    private void mockTarifficatorShopResponse(long shopId) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(
                        getString(
                                this.getClass(),
                                String.format("region/tarifficator/testGetShopDeliveryServicesTarifficator_%s.json",
                                        shopId)
                        )
                );

        tarifficatorWireMockServer.stubFor(
                get("/v2/shops/" + shopId + "/delivery-services?_user_id=123456")
                        .willReturn(response));
    }

    private void mockTarifficatorRGResponse(long shopId, long regionGroupId, int id) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(
                        getString(
                                this.getClass(),
                                String.format("region/tarifficator/" +
                                        "testGetSelectedDeliveryServicesTarifficator_%s_%s.json", shopId, id)
                        )
                );

        tarifficatorWireMockServer.stubFor(
                get("/v2/shops/" + shopId + "/region-groups/" + regionGroupId + "/delivery-services?_user_id=123456")
                        .willReturn(response));
    }

    private void mockTarifficatorSelectDeliveryServiceCall(
            long datasourceId,
            long regionGroupId,
            long deliveryServiceId,
            String requestBodyPath
    ) {
        tarifficatorWireMockServer.stubFor(
                put("/v2/shops/" + datasourceId + "/region-groups/" + regionGroupId + "/delivery-services/" +
                        deliveryServiceId + "?_user_id=0")
                        .withRequestBody(equalToJson(getString(this.getClass(), requestBodyPath)))
                        .willReturn(aResponse().withStatus(200)));
    }

    private void mockTarifficatorModifyDeliveryServicesCall(
            long datasourceId,
            long regionGroupId,
            String requestBodyPath
    ) {
        tarifficatorWireMockServer.stubFor(
                post("/v2/shops/" + datasourceId + "/region-groups/" + regionGroupId + "/delivery-services?_user_id=0")
                        .withRequestBody(equalToJson(getString(this.getClass(), requestBodyPath)))
                        .willReturn(aResponse().withStatus(200)));
    }

    private void mockTarifficatorDeleteDeliveryServiceCall(long datasourceId,
                                                           long regionGroupId,
                                                           long oldDeliveryServiceId) {
        tarifficatorWireMockServer.stubFor(
                delete("/v2/shops/" + datasourceId + "/region-groups/" + regionGroupId + "/delivery-services/" +
                        oldDeliveryServiceId + "?_user_id=0")
                        .willReturn(aResponse().withStatus(200)));
    }
}
