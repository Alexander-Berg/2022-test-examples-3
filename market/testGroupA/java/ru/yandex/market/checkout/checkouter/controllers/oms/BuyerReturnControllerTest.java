package ru.yandex.market.checkout.checkouter.controllers.oms;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.jackson.CheckouterDateFormats;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.pay.StorageReturnService;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus;
import ru.yandex.market.checkout.checkouter.returns.ReturnItemType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.util.DateUtils;
import ru.yandex.market.checkout.helpers.OrderStatusHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.util.report.ReportConfigurer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

class BuyerReturnControllerTest extends AbstractWebTestBase {

    @Autowired
    private ReturnHelper returnHelper;
    @Autowired
    private ReportConfigurer reportConfigurer;
    @Autowired
    private StorageReturnService storageReturnService;
    @Autowired
    private Clock clock;
    @Autowired
    private OrderStatusHelper orderStatusHelper;

    @BeforeEach
    void setUp() throws IOException {
        reportConfigurer.mockOutlets();
    }

    @Test
    void assertBuyerReturnListJsonResponse() throws Exception {
        final Long userId = 123L;
        final Instant createDate = Instant.parse("2021-11-26T14:27:00.00Z");
        Order order = insertOrder(userId);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return insertedReturn = insertReturnByOrder(order, createDate);
        MvcResult result = mockMvc.perform(
                get("/returns/by-uid/{userId}", userId))
                .andExpect(status().isOk())
                .andReturn();
        String response = result.getResponse().getContentAsString();
        String expectedJson = getExpectedBuyerReturnListJsonResponse(insertedReturn, order);
        JSONAssert.assertEquals(expectedJson, response, JSONCompareMode.STRICT);
    }

    private String getExpectedBuyerReturnListJsonResponse(Return expectedReturn, Order expectedOrder) {
        // Очень плохо, что мы заранее генерируем JSON, который строится по нашим же моделям
        String expectedJson = readResourceFile("/json/GetPagedUserReturnViewModelsResultExpectedTemplate.json");
        //неизвестно сколько до этого момента уже было вставлено записей, поэтому в шаблон вставляю актуальные id
        expectedJson = expectedJson.replace("{returnId}", expectedReturn.getId().toString());
        expectedJson = expectedJson.replace("{orderId}", expectedReturn.getOrderId().toString());
        expectedJson = expectedJson.replace("{deliveryId}", expectedReturn.getDelivery().getId().toString());
        expectedJson = expectedJson.replace("{returnItemId}",
                expectedReturn.getItems().get(0).getId().toString());
        expectedJson = expectedJson.replace("{orderItemId}",
                expectedReturn.getItems().get(0).getItemId().toString());
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(CheckouterDateFormats.DEFAULT).withZone(clock.getZone());
        expectedJson = expectedJson.replace(
                "{returnStatusUpdateDate}", formatter.format(expectedReturn.getStatusUpdatedAt()));
        expectedJson = expectedJson.replace(
                "{orderCreateDate}", formatter.format(DateUtils.dateToLocalDateTime(
                        expectedOrder.getCreationDate(),
                        clock)));
        expectedJson = expectedJson.replace(
                "{returnDeliveryStatusUpdateDate}", formatter.format(DateUtils.dateToLocalDateTime(
                        expectedReturn.getDelivery().getStatusUpdatedDate(),
                        clock)));
        return expectedJson;
    }

    @Test
    void buyerReturnListWithTwoResultAndHasNext() throws Exception {
        final Long userId = 357547548L;
        Order order1 = insertOrder(userId);
        Order order2 = insertOrder(userId);
        Order order3 = insertOrder(userId);
        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.DELIVERED);
        orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.DELIVERED);
        orderStatusHelper.proceedOrderToStatus(order3, OrderStatus.DELIVERED);
        returnHelper.initReturn(order1.getId(), ReturnProvider.generateReturn(order1));
        returnHelper.initReturn(order2.getId(), ReturnProvider.generateReturn(order2));
        returnHelper.initReturn(order3.getId(), ReturnProvider.generateReturn(order3));
        mockMvc.perform(
                        get("/returns/by-uid/{userId}", userId)
                                .param(CheckouterClientParams.LIMIT, "2"))
                .andExpect(status().isOk())
                .andDo(log())
                .andExpect(jsonPath("$.returns.length()").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }

    private Order insertOrder(Long userId) {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.getBuyer().setUid(userId);
        return orderCreateHelper.createOrder(parameters);
    }

    private Return insertReturnByOrder(Order order, Instant createDate) {
        Return newReturn = ReturnProvider.generateReturn(order);
        newReturn.setStatus(ReturnStatus.STARTED_BY_USER);
        newReturn.setDelivery(initReturnDelivery());
        newReturn.getDelivery().setStatusUpdatedAt(createDate);
        newReturn.setApplicationUrl("https://market-checkouter.net/return-application-1325348.pdf");
        newReturn.setCreatedAt(createDate);
        newReturn.getItems().stream()
                .filter(item -> item.getType() == ReturnItemType.ORDER_ITEM)
                .forEach(item -> item.setReturnReason("Все сломалось, все пропало. Верните деньги!"));
        newReturn = returnHelper.insertReturn(newReturn);
        String postTrackCode = "iAmTrackCode :D";
        storageReturnService.setTrackCodeToReturn(order.getId(), newReturn.getId(), ClientInfo.SYSTEM, postTrackCode);
        return newReturn;
    }

    private ReturnDelivery initReturnDelivery() {
        ReturnDelivery delivery = new ReturnDelivery();
        delivery.setDeliveryServiceId(100501L);
        delivery.setType(DeliveryType.POST);
        delivery.setStatus(ReturnDeliveryStatus.CREATED);
        delivery.setOutletId(741259L);
        return delivery;
    }

    private String readResourceFile(String filePath) {
        try {
            return IOUtils.readInputStream(getClass().getResourceAsStream(filePath));
        } catch (IOException e) {
            throw new RuntimeException("exception while reading file: " + filePath, e);
        }
    }
}
