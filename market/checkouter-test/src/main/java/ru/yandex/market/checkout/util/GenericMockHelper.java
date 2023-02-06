package ru.yandex.market.checkout.util;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.Lists;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;

/**
 * @author mkasumov
 */
public abstract class GenericMockHelper {

    private GenericMockHelper() {
    }

    @Nonnull
    public static List<ServeEvent> servedEvents(WireMockServer wireMockServer) {
        return Lists.reverse(wireMockServer.getAllServeEvents());
    }

    public static MockHttpServletRequestBuilder withUserRole(MockHttpServletRequestBuilder requestBuilder, Order o) {
        return requestBuilder
                .param("clientRole", ClientRole.USER.name())
                .param("clientId", o.getBuyer().getUid().toString());
    }

    public static MockHttpServletRequestBuilder withShopRole(MockHttpServletRequestBuilder requestBuilder, Order o) {
        return requestBuilder
                .param("clientRole", ClientRole.SHOP.name())
                .param("clientId", o.getShopId().toString())
                .param("shopId", o.getShopId().toString());
    }

    public static MockHttpServletRequestBuilder withRefereeRole(MockHttpServletRequestBuilder requestBuilder) {
        return requestBuilder
                .param("clientRole", ClientRole.REFEREE.name())
                .param("clientId", String.valueOf(ClientHelper.REFEREE_UID));
    }
}
