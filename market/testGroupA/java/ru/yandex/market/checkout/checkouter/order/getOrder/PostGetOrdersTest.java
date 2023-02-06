package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.json.helper.EntityHelper.VENDOR_ID;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherOrdersNotFound;

/**
 * @author mmetlov
 * Тест на бОльшую чась параметров GET /orders/*
 * при добавлении нового параметра НЕ ДОПИСЫВАЙТЕ тест сюда, а создайте отдельный класс
 * @see GetOrdersByExcludeABOTestBase
 */
public class PostGetOrdersTest extends GetOrdersTestBase {

    private final String urlTemplate = "/get-orders";

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: Получeние заказa по id")
    @Test
    public void idTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"orderIds\": [%d, %d]}",
                                defaultOrder.getId(), modifiedOrder.getId()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]").value(containsInAnyOrder(
                        hasEntry("id", modifiedOrder.getId().intValue()),
                        hasEntry("id", defaultOrder.getId().intValue()))));

        long wrongId = Long.MAX_VALUE;
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"orderIds\": [%d]}", wrongId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: пейджер")
    @Test
    public void pagerTest() throws Exception {
        final int pageSize = 1;
        //PAGE_SIZE has settled, default page
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{" +
                                "\"rgbs\":[\"BLUE\",\"WHITE\"]," +
                                "\"pageInfo\": {" +
                                "\"pageSize\": %d" +
                                "}}", pageSize))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*]", hasSize(1)));


        //PAGE_SIZE has settled, first page
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{" +
                                "\"rgbs\":[\"BLUE\",\"WHITE\"]," +
                                "\"pageInfo\": {" +
                                "\"pageSize\": %d," +
                                "\"currentPage\": 1" +
                                "}}", pageSize))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*]", hasSize(1)));

        //PAGE_SIZE has settled, second page
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{" +
                                "\"rgbs\":[\"BLUE\",\"WHITE\"]," +
                                "\"pageInfo\": {" +
                                "\"pageSize\": %d," +
                                "\"currentPage\": 2" +
                                "}}", pageSize))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*]", hasSize(1)));

        //PAGE_SIZE has settled, too big page PAGE
        mockMvc.perform(

                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{" +
                                "\"rgbs\":[\"BLUE\",\"WHITE\"]," +
                                "\"pageInfo\": {" +
                                "\"pageSize\": %d," +
                                "\"currentPage\": 4" +
                                "}}", pageSize))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pager.page").value(3));

    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказа по статусу/подстатусу")
    @Test
    public void statusAndSubstatusTest() throws Exception {

        // find order by status
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statuses\": [\"%s\"]}",
                                defaultOrder.getStatus().name()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].status").value(defaultOrder.getStatus().name()));

        // find order by substatus
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"substatuses\": [\"%s\"]}",
                                OrderSubstatus.PENDING_CANCELLED))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].substatus").value(OrderSubstatus.PENDING_CANCELLED.name()));

        //There is no orders with this status was created, expect error
        long wrongId = Long.MAX_VALUE;
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statuses\": [\"%s\"]}",
                                OrderStatus.DELIVERED))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());

    }


    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение fake заказов")
    @Test
    public void fakeTest() throws Exception {
        // search for not fake orders. Expects to find first one.
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"fake\": %b}", false))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].fake").value(false))
                .andExpect(jsonPath("$.orders[*]", hasSize(1)));

        //  search for fake orders. Expects to find second one.
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"fake\": %b}", true))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].fake").value(true))
                .andExpect(jsonPath("$.orders[*]", hasSize(2)));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по paymentId")
    @Test
    public void paymentIdTest() throws Exception {
        int paymentId = globalOrder.getPaymentId().intValue();

        // search order with correct payment Id. Expects to find one order.
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"paymentId\": %d}", paymentId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(globalOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].paymentId").value(paymentId))
                .andExpect(jsonPath("$.orders[*]", hasSize(1)));

        //Not existing payment Id, expect error
        long wrongId = Long.MAX_VALUE;
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"paymentId\": %d}", wrongId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());

    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по paymentType")
    @Test
    public void paymentTypeMethodTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"paymentType\": \"%s\"}",
                                PaymentType.PREPAID))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].paymentType").value(PaymentType.PREPAID.toString()));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по statusUpdateFromDate")
    @Test
    public void statusUpdateFromTest() throws Exception {
        /* TODO
        дата сериализуется в +3 часа и тест не будет работать с 0 до 3 часов

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"statusUpdateFromDate\": \"%s\"}",
                                DATE_FORMAT.format(defaultOrder.getStatusUpdateDate())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*]", hasSize(3)));*/

        LocalDateTime minusDayDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromDate\": \"%s\"}",
                                DATE_FORMAT8.format(minusDayDate)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*]", hasSize(3)));

        LocalDateTime tooBigDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromDate\": \"%s\"}",
                                DATE_FORMAT8.format(tooBigDate)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по statusUpdateToDate")
    @Test
    public void statusUpdateToTest() throws Exception {
        //Whe set current day. There is no order would be found. Suppose this is an error and would be fixed in
        // MARKETCHECKOUT-618
        /* из-за MARKETCHECKOUT-4654  не работает с 00:00 до 03:00
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"statusUpdateToDate\": \"%s\"}",
                                DATE_FORMAT.format(defaultOrder.getStatusUpdateDate())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
                */

        LocalDateTime plusDayDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateToDate\": \"%s\"}",
                                DATE_FORMAT8.format(plusDayDate)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*]", hasSize(3)));

        LocalDateTime tooSmallDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateToDate\": \"%s\"}",
                                DATE_FORMAT8.format(tooSmallDate)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по statusUpdateFromDate, statusUpdateToDate")
    @Test
    public void statusUpdateFromToTest() throws Exception {
        //Whe set current day. There is no order would be found. Suppose this is an error and would be fixed in
        // MARKETCHECKOUT-618
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromDate\": \"%s\", " +
                                        "\"statusUpdateToDate\": \"%s\"}",
                                DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()),
                                DATE_FORMAT.format(defaultOrder.getStatusUpdateDate())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());

        LocalDateTime minusDayDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
        LocalDateTime plusDayDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromDate\": \"%s\", " +
                                        "\"statusUpdateToDate\": \"%s\"}",
                                DATE_FORMAT8.format(minusDayDate),
                                DATE_FORMAT8.format(plusDayDate)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*]", hasSize(3)));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromDate\": \"%s\", " +
                                        "\"statusUpdateToDate\": \"%s\"}",
                                DATE_FORMAT8.format(minusDayDate.minusDays(2)),
                                DATE_FORMAT8.format(minusDayDate)))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"statusUpdateFromDate\": \"%s\", \"statusUpdateToDate\": \"%s\"}",
                                DATE_FORMAT8.format(plusDayDate),
                                DATE_FORMAT8.format(plusDayDate.plusDays(2))))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());

    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по statusUpdateFromTimestamp")
    @Test
    public void statusUpdateFromTimestampTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromTimestamp\": %d}",
                                minStatusUpdateTimestamp))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*]", hasSize(3)));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromTimestamp\": %d}",
                                maxStatusUpdateTimestamp + 1000))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по statusUpdateToTimestamp")
    @Test
    public void statusUpdateToTimestampTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateToTimestamp\": %d}",
                                maxStatusUpdateTimestamp + 1000))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]").value(containsInAnyOrder(hasEntry("id",
                        defaultOrder.getId().intValue()),
                        hasEntry("id", modifiedOrder.getId().intValue()),
                        hasEntry("id", globalOrder.getId().intValue()))));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateToTimestamp\": %d}",
                                minStatusUpdateTimestamp))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по statusUpdateFromTimestamp и statusUpdateToTimestamp")
    @Test
    public void statusUpdateFromToTimestampTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromTimestamp\": %d, " +
                                        "\"statusUpdateToTimestamp\": %d}", minStatusUpdateTimestamp,
                                maxStatusUpdateTimestamp + 1000))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]").value(containsInAnyOrder(hasEntry("id",
                        defaultOrder.getId().intValue()),
                        hasEntry("id", modifiedOrder.getId().intValue()),
                        hasEntry("id", globalOrder.getId().intValue()))));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromTimestamp\": %d, " +
                                "\"statusUpdateToTimestamp\": %d}", minStatusUpdateTimestamp, maxStatusUpdateTimestamp))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*]", hasSize(2)));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"statusUpdateFromTimestamp\": %d, " +
                                "\"statusUpdateToTimestamp\": %d}", minStatusUpdateTimestamp, minStatusUpdateTimestamp))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по shopOrderId")
    @Test
    public void shopOrderIdTest() throws Exception {
        final String wrongShopOrderId = "1000000";

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"shopOrderIds\": [\"%s\"]}",
                                defaultOrder.getShopOrderId()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].shopOrderId").value(defaultOrder.getShopOrderId()));

        mockMvc.perform(

                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"shopOrderIds\": [%s, %s]}",
                                defaultOrder.getShopOrderId(), wrongShopOrderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].shopOrderId").value(defaultOrder.getShopOrderId()));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"shopOrderIds\": [%s]}",
                                wrongShopOrderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по acceptMethod")
    @Test
    public void acceptMethodTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"acceptMethod\": \"%s\"}",
                                defaultOrder.getAcceptMethod()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].acceptMethod").value(modifiedOrder.getAcceptMethod().toString()));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по lastStatusRole")
    @Test
    public void lastStatusRoleTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"lastStatusRole\": \"%s\"}",
                                ClientRole.SYSTEM))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"lastStatusRole\": \"%s\"}",
                                ClientRole.REFEREE))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по noAuth")
    @Test
    public void noAuthTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"noAuth\": \"%s\"}",
                                false))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[1].noAuth").value(false));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"noAuth\": \"%s\"}",
                                true))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по userGroup")
    @Test
    public void userGroupTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"userGroups\": [\"%s\"]}",
                                UserGroup.DEFAULT))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"userGroups\": [\"%s\"]}",
                                UserGroup.ABO))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по context")
    @Test
    public void contextsTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"contexts\": [\"%s\"]}",
                                Context.MARKET))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].context").value(Context.MARKET.name()));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"contexts\": [\"%s\", \"%s\"]}",
                                Context.MARKET, Context.PINGER))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[0].context").value(Context.MARKET.name()))
                .andExpect(jsonPath("$.orders[*]").value(not(contains(hasEntry("context",
                        not(Context.MARKET.name()))))));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"contexts\": [\"%s\"]}",
                                Context.PINGER))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по notes")
    @Test
    public void notesTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"notes\": \"%s\"}", "astra"))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"notes\": \"%s\"}", "per aspera ad " +
                                "astra"))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"notes\": \"%s\"}", "veritas"))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по global")
    @Test
    public void globalTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"global\": \"%s\"}",
                                true))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(globalOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*].id", hasSize(1)));

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"global\": \"%s\"}",
                                false))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(modifiedOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[1].id").value(defaultOrder.getId().intValue()))
                .andExpect(jsonPath("$.orders[*].id", hasSize(2)));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: получение заказов по SKU/SSKU")
    @Test
    public void getBySkuAndSskuTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"mSku\": \"%s\"}", "100307940934"))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"sSku\": \"%s\"}", "100307940934"))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherOrdersNotFound());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: В заказе возвращается vendorId")
    @Test
    public void vendorIdTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"BLUE\",\"WHITE\"],\"orderIds\": [%d]}",
                                defaultOrder.getId()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].items[0].vendorId").value(VENDOR_ID));
    }
}
