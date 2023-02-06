package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.EnumSet;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.mock.http.MockHttpInputMessage;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.common.rest.Pager;

/**
 * Created by berest on 24.08.16.
 */
public class OrderSearchRequestJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void testSerialization() throws IOException, JSONException {
        OrderSearchRequest request = new OrderSearchRequest();
        request.buyerEmailSubstring = "somebody";
        request.buyerEmail = "somebody@something.somewhere";
        request.statuses = EnumSet.of(OrderStatus.PROCESSING, OrderStatus.PENDING, OrderStatus.PICKUP);
        request.userId = 12312L;
        request.shopId = 774L;
        request.acceptMethod = OrderAcceptMethod.DEFAULT;
        request.buyerPhone = "+504346248";
        request.contexts = Arrays.asList(Context.MARKET, Context.SELF_CHECK);
        request.excludeABO = false;
        request.fake = false;
        request.lastStatusRole = ClientRole.SHOP;
        request.noAuth = false;
        request.pageInfo = new Pager(null, 30, 50, null, null, null);
        String json = write(request);
        System.out.println(json);

        String expected = "" +
                "{\"orderIds\":null," +
                "\"userId\":12312," +
                "\"shopId\":774," +
                "\"statuses\":[\"PROCESSING\",\"PICKUP\",\"PENDING\"]," +
                "\"substatuses\":null," +
                "\"fake\":false," +
                "\"contexts\":[\"MARKET\",\"SELF_CHECK\"]," +
                "\"fromDate\":null," +
                "\"toDate\":null," +
                "\"pageInfo\":{\"total\":null,\"from\":30,\"to\":50,\"pageSize\":null,\"pagesCount\":null}," +
                "\"paymentId\":null,\"notStatuses\":null,\"notSubstatuses\":null," +
                "\"excludeABO\":false,\"paymentType\":null,\"paymentMethod\":null,\"statusUpdateFromDate\":null," +
                "\"statusUpdateToDate\":null,\"statusUpdateFromTimestamp\":null," +
                "\"statusUpdateToTimestamp\":null,\"buyerPhone\":\"+504346248\",\"buyerEmail\":\"somebody@something" +
                ".somewhere\"," +
                "\"buyerEmailSubstring\":\"somebody\",\"shopOrderIds\":null,\"acceptMethod\":\"PUSH_API\"," +
                "\"lastStatusRole\":\"SHOP\",\"noAuth\":false,\"userGroups\":null}";
        JSONAssert.assertEquals(expected, json, false);
    }

    @Test
    public void shouldParseDateAsMidnight() throws IOException {
        String json = "{\"statusUpdateToDate\":\"30-11-2017\"}";
        OrderSearchRequest request = (OrderSearchRequest) converter.read(
                OrderSearchRequest.class, new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8))
        );

        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(request.getStatusUpdateToDate().toInstant(), ZoneId.of(
                "Europe/Moscow"));
        Assertions.assertEquals(0, zonedDateTime.getHour());
    }
}
