package ru.yandex.market.logistics.logistics4shops.external.controller;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.factory.CheckouterFactory;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Получение данных отправки")
@DatabaseSetup("/external/controller/getOutbound/prepare.xml")
class ExternalGetOutboundTest extends AbstractIntegrationTest {
    private static final String URL = "external/outbounds/getOutbound";
    private static final Long[] ORDER_IDS = {100100L, 100102L, 100103L};
    private static final Pager DEFAULT_PAGER = Pager.atPage(1, 50).setTotal(Integer.MAX_VALUE);

    @Autowired
    private CheckouterAPI checkouterAPI;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(checkouterAPI);
    }

    @Test
    @DisplayName("Отправка существует в БД")
    void getOutbound() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutbound/request/success.xml",
            "external/controller/getOutbound/response/success.xml"
        );
    }

    @Test
    @DisplayName("Заказы в отправке")
    void getOutboundOrderIds() {
        mockCheckouterOrderBoxes();

        ArgumentCaptor<OrderSearchRequest> searchRequestCaptor = ArgumentCaptor.forClass(OrderSearchRequest.class);

        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutbound/request/successWithOrders.xml",
            "external/controller/getOutbound/response/successWithOrders.xml"
        );

        verify(checkouterAPI).getOrders(
            safeRefEq(checkouterFactory.systemUserInfo()),
            searchRequestCaptor.capture()
        );

        OrderSearchRequest searchRequestArgument = searchRequestCaptor.getValue();

        softly.assertThat(searchRequestArgument.pageInfo).isEqualTo(DEFAULT_PAGER);
        softly.assertThat(searchRequestArgument.rgbs).isEqualTo(Set.of(Color.BLUE, Color.WHITE));
        softly.assertThat(searchRequestArgument.orderIds).containsExactlyInAnyOrder(ORDER_IDS);
    }

    @Test
    @DisplayName("Отправка отсутствует в БД")
    void outboundNotFound() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutbound/request/notFound.xml",
            "external/controller/getOutbound/response/notFound.xml"
        );
    }

    @Test
    @DisplayName("Идентификатор не число")
    void invalidId() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOutbound/request/non_numeric_yandex_id.xml",
            "external/controller/getOutbound/response/non_numeric_yandex_id.xml"
        );

        verify(checkouterAPI).getOrders(
            safeRefEq(checkouterFactory.systemUserInfo()),
            safeRefEq(OrderSearchRequest.builder()
                .withOrderIds(List.of(100104L).toArray(new Long[0]))
                .withPageInfo(DEFAULT_PAGER)
                .withRgbs(Color.BLUE, Color.WHITE)
                .build()
            )
        );
    }

    private void mockCheckouterOrderBoxes() {
        Pager pager = Pager.atPage(1, 50).setTotal(2);
        var order = CheckouterFactory.createOrder(100102L, List.of("box1-1", "box2-1"));
        when(checkouterAPI.getOrders(
            any(RequestClientInfo.class),
            any(OrderSearchRequest.class)
        )).thenReturn(new PagedOrders(List.of(order), pager));
    }
}
