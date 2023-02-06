package ru.yandex.market.checkout.checkouter.delivery.dropship;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.checkout.checkouter.order.OrderServiceTestHelper.FF_SHOP_ID;

public class DropshipOrderPayTest extends AbstractWebTestBase {

    @Autowired
    private DropshipDeliveryHelper dropshipDeliveryHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private ShopService shopService;

    @Test
    public void deliveryShouldHaveBlueFulfilmentInn() throws Exception {
        Order order = dropshipDeliveryHelper.createDropshipOrder();
        orderPayHelper.payForOrder(order);

        List<ServeEvent> createBasketRequests = trustMockConfigurer.servedEvents().stream()
                .filter(se -> se.getRequest().getUrl().endsWith("/payments?show_trust_payment_id=true")
                        && se.getRequest().getMethod().equals(RequestMethod.POST))
                .collect(Collectors.toList());

        assertThat(createBasketRequests, Matchers.hasSize(1));
        LoggedRequest trustRequest = createBasketRequests.get(0).getRequest();
        List<String> inns = JsonPath.compile("$.orders[*].fiscal_inn").read(trustRequest.getBodyAsString());
        // проверяем, что есть два разных ИНН.
        assertThat(inns.stream().distinct().count(), Matchers.equalTo(2L));
        // и что хотя бы один из этих ИНН - дефолтовый фулфилментовый маркета.
        assertThat(inns, Matchers.hasItem(shopService.getMeta(FF_SHOP_ID).getInn()));
    }
}
