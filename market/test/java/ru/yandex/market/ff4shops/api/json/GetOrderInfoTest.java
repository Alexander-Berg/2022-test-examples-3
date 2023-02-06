package ru.yandex.market.ff4shops.api.json;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.model.CourierDto;
import ru.yandex.market.ff4shops.api.model.CourierWithDeadlineDTO;
import ru.yandex.market.ff4shops.api.model.CouriersWithDeadlinesResponse;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

@DisplayName("Информация о заказе")
@DbUnitDataSet(before = "GetOrderInfoTest.before.csv")
public class GetOrderInfoTest extends AbstractJsonControllerFunctionalTest {

    private ObjectMapper mapper = new ObjectMapper();

    {
        mapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Проверка что возвращается Map для курьера без дедлайна и для дедлайна без курьера")
    void getOrderInfo() throws IOException {
        List<Long> orderIds = List.of(94650671L, 94656852L, 94616251L);
        ResponseEntity<String> response = ordersRequest(orderIds);
        JsonNode jsonNode = mapper.readTree(response.getBody());
        Map<Long, CourierWithDeadlineDTO> mapResponse = mapper.readerFor(CouriersWithDeadlinesResponse.class)
                .<CouriersWithDeadlinesResponse>readValue(jsonNode.get("result")).getCouriersWithDeadlines();
        String pathToFile = "ru/yandex/market/ff4shops/api/json/getOrderInfoTest.exceptMap.json";
        Map<Long, CourierWithDeadlineDTO> mapExpect = mapper.readValue(IntegrationTestUtils.extractFileContent(pathToFile),
                                                        CouriersWithDeadlinesResponse.class).getCouriersWithDeadlines();
        comparingMaps(mapResponse, mapExpect);
    }

    @Test
    @DisplayName("Проверка что возвращается Map если в ответе инормация только по дедлайнам")
    void getOrderOnlyDeadlineInfo() throws IOException {
        List<Long> orderIds = List.of(94616251L);
        ResponseEntity<String> response = ordersRequest(orderIds);
        JsonNode jsonNode = mapper.readTree(response.getBody());
        Map<Long, CourierWithDeadlineDTO> mapResponse = mapper.readerFor(CouriersWithDeadlinesResponse.class)
                .<CouriersWithDeadlinesResponse>readValue(jsonNode.get("result")).getCouriersWithDeadlines();
        String pathToFile = "ru/yandex/market/ff4shops/api/json/getOrderInfoTest.onlyDeadline.json";
        Map<Long, CourierWithDeadlineDTO> mapExpect = mapper.readValue(IntegrationTestUtils.extractFileContent(pathToFile),
                CouriersWithDeadlinesResponse.class).getCouriersWithDeadlines();
        comparingMaps(mapResponse, mapExpect);
    }

    @Test
    @DisplayName("Возвращается пустой Map если информации не найдено")
    void getEmptyOrderInfo() throws IOException {
        List<Long> orderIds = List.of(0L);
        ResponseEntity<String> response = ordersRequest(orderIds);
        JsonNode jsonNode = mapper.readTree(response.getBody());
        Map<Long, CourierWithDeadlineDTO> mapResponse = mapper.readerFor(CouriersWithDeadlinesResponse.class)
                .<CouriersWithDeadlinesResponse>readValue(jsonNode.get("result")).getCouriersWithDeadlines();
        Assertions.assertNotNull(mapResponse);
        Assertions.assertEquals(mapResponse.size(), 0);
    }

    private void comparingMaps(Map<Long, CourierWithDeadlineDTO> mapResponse,
                               Map<Long, CourierWithDeadlineDTO> mapExpect) {
        mapExpect.values().forEach(orderInfo -> {
            CourierWithDeadlineDTO responseOrder = mapResponse.get(orderInfo.getOrderId());
            Assertions.assertNotNull(responseOrder);
            Assertions.assertEquals(orderInfo.getDeadline(), responseOrder.getDeadline());
            assertCourier(orderInfo.getCourier(), responseOrder.getCourier());
        });
    }

    private void assertCourier(CourierDto expectedCourier, CourierDto responseCourier) {
        if (expectedCourier == null) {
            Assertions.assertNull(responseCourier);
        } else {
            Assertions.assertEquals(expectedCourier.getFirstName(), responseCourier.getFirstName());
            Assertions.assertEquals(expectedCourier.getLastName(), responseCourier.getLastName());
            Assertions.assertEquals(expectedCourier.getMiddleName(), responseCourier.getMiddleName());
            Assertions.assertEquals(expectedCourier.getVehicleNumber(), responseCourier.getVehicleNumber());
            Assertions.assertEquals(expectedCourier.getVehicleDescription(), responseCourier.getVehicleDescription());
            Assertions.assertEquals(expectedCourier.getPhoneNumber(), responseCourier.getPhoneNumber());
            Assertions.assertEquals(expectedCourier.getPhoneExtension(), responseCourier.getPhoneExtension());
            Assertions.assertEquals(expectedCourier.getCourierType(), responseCourier.getCourierType());
            Assertions.assertEquals(expectedCourier.getUrl(), responseCourier.getUrl());
            Assertions.assertEquals(expectedCourier.getElectronicAcceptanceCertificateCode(),
                                    responseCourier.getElectronicAcceptanceCertificateCode());
            Assertions.assertEquals(expectedCourier.getElectronicAcceptCodeRequired(),
                                    responseCourier.getElectronicAcceptCodeRequired());
            Assertions.assertEquals(expectedCourier.getElectronicAcceptCodeStatus(),
                                    responseCourier.getElectronicAcceptCodeStatus());
        }
    }

    private ResponseEntity<String> ordersRequest(List<Long> orderIds) {
        String referenceUrl = FF4ShopsUrlBuilder.getOrderInfo(randomServerPort, orderIds);

        return FunctionalTestHelper.getForEntity(
                referenceUrl,
                FunctionalTestHelper.jsonHeaders()
        );
    }
}
