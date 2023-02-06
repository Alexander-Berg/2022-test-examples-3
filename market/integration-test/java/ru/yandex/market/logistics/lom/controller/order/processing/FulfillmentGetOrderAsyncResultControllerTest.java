package ru.yandex.market.logistics.lom.controller.order.processing;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemPlace;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Order;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.async.GetOrderErrorDto;
import ru.yandex.market.logistics.lom.model.async.GetOrderSuccessDto;
import ru.yandex.market.logistics.lom.service.async.AsyncResultService;
import ru.yandex.market.logistics.lom.service.async.ProcessItemNotFoundChangeRequestAsyncResultService;
import ru.yandex.market.logistics.lom.service.async.ProcessOrderChangedByPartnerRequestAsyncResultService;
import ru.yandex.market.logistics.lom.service.async.ProcessOrderPlacesChangedAsyncResultService;
import ru.yandex.market.logistics.lom.service.async.ProcessOrderReadyToShipAsyncResultService;

import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createFfOrder;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createPlace;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwFulfillmentEntitiesUtils.createUnitId;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тест асинхронного получения заказа по FF API")
public class FulfillmentGetOrderAsyncResultControllerTest extends AbstractContextualTest {
    @Autowired
    private ProcessOrderChangedByPartnerRequestAsyncResultService orderChangedByPartnerRequestAsyncResultService;
    @Autowired
    private ProcessItemNotFoundChangeRequestAsyncResultService itemNotFoundChangeRequestAsyncResultService;
    @Autowired
    private ProcessOrderReadyToShipAsyncResultService orderReadyToShipAsyncResultService;
    @Autowired
    private ProcessOrderPlacesChangedAsyncResultService orderPlacesChangedAsyncResultService;

    @Test
    @DisplayName("Успешное асинхронное получение заказа c заявкой PROCESS_ORDER_ITEM_NOT_FOUND_REQUEST")
    @DatabaseSetup("/controller/order/getOrder/items_not_found.xml")
    void asyncResultSuccessItemNotFound() throws Exception {
        checkGetOrderSuccess(itemNotFoundChangeRequestAsyncResultService);
    }

    @Test
    @DisplayName("Успешное асинхронное получение заказа c заявкой PROCESS_ORDER_CHANGED_BY_PARTNER_REQUEST")
    @DatabaseSetup("/controller/order/getOrder/changed_by_partner.xml")
    void asyncResultSuccessChangeByPartner() throws Exception {
        checkGetOrderSuccess(orderChangedByPartnerRequestAsyncResultService);
    }

    @Test
    @DisplayName("Успешное асинхронное получение заказа c заявкой PROCESS_ORDER_READY_TO_SHIP")
    @DatabaseSetup("/controller/order/getOrder/ready_to_ship.xml")
    void asyncResultSuccessReadyToShip() throws Exception {
        checkGetOrderSuccess(orderReadyToShipAsyncResultService);
    }

    @Test
    @DisplayName("Успешное асинхронное получение заказа c заявкой PROCESS_PLACES_CHANGED")
    @DatabaseSetup("/controller/order/getOrder/places_changed.xml")
    void asyncResultSuccessPlacesChanged() throws Exception {
        checkGetOrderSuccess(orderPlacesChangedAsyncResultService);
    }

    @Test
    @DisplayName("Успешная обработка ошибки получения заказа")
    @DatabaseSetup("/controller/order/getOrder/ready_to_ship.xml")
    void asyncResultSuccessProcessingChangeRequestFlow() throws Exception {
        GetOrderErrorDto getOrderErrorDto = new GetOrderErrorDto(11L, "2-LOinttest-1", 1L, 2, "Error", false);
        mockMvc.perform(request(HttpMethod.POST, "/orders/ff/get/error", getOrderErrorDto))
            .andExpect(status().isOk())
            .andExpect(noContent());

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=ERROR\t" +
                    "format=plain\t" +
                    "payload=Error getting order from partner. Code: 2, " +
                    "message: Error\trequest_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                    + "entity_types=order,partner\tentity_values=order:2-LOinttest-1,partner:1"
            );
    }

    private void checkGetOrderSuccess(AsyncResultService<GetOrderSuccessDto, GetOrderErrorDto> service)
        throws Exception {
        Order order = createFfOrder().setPlaces(List.of(
            createPlace(1, 1).setItemPlaces(List.of(new ItemPlace(createUnitId(1), 5))).build()
        ))
            .build();

        GetOrderSuccessDto orderSuccessDto = new GetOrderSuccessDto(11L, 1L, order);
        mockMvc.perform(request(HttpMethod.POST, "/orders/ff/get/success", orderSuccessDto))
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

}
