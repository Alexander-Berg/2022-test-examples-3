package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

/**
 * @author mmetlov
 */
public class GetOrdersCountTest extends GetOrdersTestBase {

    private final String urlTemplate = "/orders/count";

    @DisplayName("посчитать заказы по status/substatus")
    @Test
    public void statusAndSubstatusTest() throws Exception {

        // find order by status
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS, defaultOrder.getStatus().name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        // find order by substatus
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.SUBSTATUS, OrderSubstatus.PENDING_CANCELLED.toString())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        //There is no orders with this status was created, expect error
        long wrongId = Long.MAX_VALUE;
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS, OrderStatus.DELIVERED.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));

    }

    @DisplayName("посчитать заказы по fake")
    @Test
    public void fakeTest() throws Exception {
        // search for not fake orders. Expects to find first one.
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.FAKE, String.valueOf(false))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        //  search for fake orders. Expects to find second one.
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.FAKE, String.valueOf(true))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(2));
    }

    @DisplayName("посчитать заказы по paymentId")
    @Test
    public void paymentIdTest() throws Exception {
        int paymentId = globalOrder.getPaymentId().intValue();

        // search order with correct payment Id. Expects to find one order.
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.PAYMENT_ID, String.valueOf(paymentId))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        //Not existing payment Id, expect error
        long wrongId = Long.MAX_VALUE;
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.PAYMENT_ID, String.valueOf(wrongId))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));

    }

    @DisplayName("посчитать заказы по paymentType/paymentMethod")
    @Test
    public void paymentTypeMethodTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.PAYMENT_TYPE, String.valueOf(PaymentType.POSTPAID))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.PAYMENT_TYPE, String.valueOf(PaymentType.POSTPAID))
                        .param(CheckouterClientParams.PAYMENT_METHOD, String.valueOf(PaymentMethod.BANK_CARD))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.PAYMENT_TYPE, String.valueOf(PaymentType.PREPAID))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

    }

    @DisplayName("посчитать заказы по statusUpdateFromDate")
    @Test
    public void statusUpdateFromTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM,
                                DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        LocalDateTime minusDayDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM, DATE_FORMAT8.format(minusDayDate))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        LocalDateTime tooBigDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM, DATE_FORMAT8.format(tooBigDate))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по statusUpdateToDate")
    @Test
    public void statusUpdateToTest() throws Exception {
        //Whe set current day. There is no order would be found. Suppose this is an error and would be fixed in
        //MARKETCHECKOUT-618
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_TO,
                                DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));

        LocalDateTime plusDayDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(plusDayDate))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        LocalDateTime tooSmallDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(tooSmallDate))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по statusUpdateFromDate/statusUpdateToDate")
    @Test
    public void statusUpdateFromToTest() throws Exception {
        //Whe set current day. There is no order would be found. Suppose this is an error and would be fixed in
        //MARKETCHECKOUT-618
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM,
                                DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()))
                        .param(CheckouterClientParams.STATUS_UPDATE_TO,
                                DATE_FORMAT.format(defaultOrder.getStatusUpdateDate()))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));

        LocalDateTime minusDayDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).minusDays(1);
        LocalDateTime plusDayDate = LocalDateTime
                .ofInstant(defaultOrder.getStatusUpdateDate().toInstant(), ZoneId.systemDefault()).plusDays(1);

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM, DATE_FORMAT8.format(minusDayDate))
                        .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(plusDayDate))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM,
                                DATE_FORMAT8.format(minusDayDate.minusDays(2)))
                        .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(minusDayDate))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM, DATE_FORMAT8.format(plusDayDate))
                        .param(CheckouterClientParams.STATUS_UPDATE_TO, DATE_FORMAT8.format(plusDayDate.plusDays(2)))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));

    }

    @DisplayName("посчитать заказы по statusUpdateFromTimestamp")
    @Test
    public void statusUpdateFromTimestampTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                String.valueOf(minStatusUpdateTimestamp))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                String.valueOf(maxStatusUpdateTimestamp + 1000))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по statusUpdateToTimestamp")
    @Test
    public void statusUpdateToTimestampTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                String.valueOf(maxStatusUpdateTimestamp + 1000))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                String.valueOf(minStatusUpdateTimestamp))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по statusUpdateFromTimestamp/statusUpdateToTimestamp")
    @Test
    public void statusUpdateFromToTimestampTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                String.valueOf(minStatusUpdateTimestamp))
                        .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                String.valueOf(maxStatusUpdateTimestamp + 1000))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                String.valueOf(minStatusUpdateTimestamp))
                        .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                String.valueOf(maxStatusUpdateTimestamp))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(2));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.STATUS_UPDATE_FROM_TIMESTAMP,
                                String.valueOf(minStatusUpdateTimestamp))
                        .param(CheckouterClientParams.STATUS_UPDATE_TO_TIMESTAMP,
                                String.valueOf(minStatusUpdateTimestamp))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по shopOrderId")
    @Test
    public void shopOrderIdTest() throws Exception {
        final String wrongShopOrderId = "1000000";

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.SHOP_ORDER_ID, defaultOrder.getShopOrderId())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.SHOP_ORDER_ID, defaultOrder.getShopOrderId())
                        .param(CheckouterClientParams.SHOP_ORDER_ID, wrongShopOrderId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.SHOP_ORDER_ID, wrongShopOrderId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по acceptMethod")
    @Test
    public void acceptMethodTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.ACCEPT_METHOD, OrderAcceptMethod.WEB_INTERFACE.toString())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));
    }

    @DisplayName("посчитать заказы по lastStatusRole")
    @Test
    public void lastStatusRoleTest() throws Exception {
        //3 заказа в базе
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        /* TODO Но по истории в 90% случаев только 2 из них меняла роль SYSTEM :(
            Это связано с HISTORY_TABLE_JOIN_CONDITION = "H.ORDER_ID=O.ID and H.FROM_DT=O.STATUS_UPDATED_AT", полагаю
        */
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.LAST_STATUS_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.LAST_STATUS_ROLE, ClientRole.REFEREE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по noAuth")
    @Test
    public void noAuthTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(false))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(3));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(true))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по userGroup")
    @Test
    public void userGroupTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.USER_GROUP, UserGroup.DEFAULT.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(2));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.USER_GROUP, UserGroup.ABO.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по context")
    @Test
    public void contextsTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CONTEXT, Context.MARKET.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(2));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CONTEXT, Context.MARKET.name())
                        .param(CheckouterClientParams.CONTEXT, Context.PINGER.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(2));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CONTEXT, Context.PINGER.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("посчитать заказы по notes")
    @Test
    public void notesTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.NOTES, "astra")
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.NOTES, "per aspera ad astra")
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.NOTES, "veritas")
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(0));
    }

    @DisplayName("GET /orders/*: получение заказов по global")
    @Test
    public void globalTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(Names.Order.GLOBAL, String.valueOf(true))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        mockMvc.perform(
                MockMvcRequestBuilders.get(urlTemplate)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(Names.Order.GLOBAL, String.valueOf(false))
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(2));
    }
}
