package ru.yandex.market.checkout.helpers;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.cashback.model.OrdersCashbackInfoResponse;
import ru.yandex.market.checkout.checkouter.cashback.model.details.OrdersCashbackDetailsResponse;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.checkout.checkouter.json.Names.Order.ORDER_IDS;

@WebTestHelper
public class CashbackDetailsHelper extends MockMvcAware {

    public CashbackDetailsHelper(WebApplicationContext webApplicationContext,
                                 TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public OrdersCashbackDetailsResponse getOrderCashbackDetails(Collection<Order> orders,
                                                                 long userId) throws Exception {
        return getOrderCashbackDetails(orders.stream().map(Order::getId).collect(Collectors.toSet()), userId);
    }

    public OrdersCashbackDetailsResponse getOrderCashbackDetails(Set<Long> orderIds, long userId) throws Exception {
        MockHttpServletRequestBuilder builder = get("/users/{userId}/cashback", userId);
        builder.param(ORDER_IDS, orderIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        builder.param("mergeOption", "UNKNOWN");
        return performApiRequest(builder, OrdersCashbackDetailsResponse.class);
    }

    public OrdersCashbackInfoResponse getOrdersCashbackInfo(Set<Long> orderIds,
                                                            long userId) throws Exception {
        MockHttpServletRequestBuilder builder = get("/users/{userId}/cashbackInfo", userId);
        builder.param(ORDER_IDS, orderIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        return performApiRequest(builder, OrdersCashbackInfoResponse.class);
    }

}
