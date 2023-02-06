package ru.yandex.market.checkout.checkouter.events;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MoveOrderResponse;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.AuthHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.MUID;
import static ru.yandex.market.checkout.checkouter.event.HistoryEventType.ORDER_UID_UPDATED;
import static ru.yandex.market.checkout.checkouter.order.MoveOrderStatus.SUCCESS;

public class MoveOrdersHistoryEventsTest extends AbstractWebTestBase {

    private static final String MOVE_URL_TEMPLATE = "/move-orders";

    @Autowired
    private AuthHelper authHelper;

    @Resource(name = "checkouterAnnotationObjectMapper")
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_COIN_BINDING_ON_MOVE_ORDER, true);
    }

    @Test
    public void shouldCreateUidUpdatedEvent() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        Buyer to = BuyerProvider.getBuyer();
        Order order = orderCreateHelper.createOrder(new Parameters(BuyerProvider.getDefaultBuyer(from)));

        assertThat(order, hasProperty("uid", is(from)));

        List<MoveOrderResponse> response = mapper.readValue(mockMvc.perform(
                post(MOVE_URL_TEMPLATE)
                        .content(String.format("{\"orders\": [%d]}", order.getId()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", to.getUid().toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .param("rgb", Color.GREEN.toString())
                        .cookie(new Cookie(MUID, responseCookie))
        )
                .andReturn().getResponse().getContentAsString(), new TypeReference<List<MoveOrderResponse>>() {
        });

        assertThat(response, hasItem(hasProperty("status", is(SUCCESS))));

        PagedEvents events = client.orderHistoryEvents().getOrderHistoryEvents(order.getId(), ClientRole.SYSTEM, null,
                0, 10);

        assertThat(events.getItems().stream()
                .filter(e -> e.getType() == ORDER_UID_UPDATED)
                .map(OrderHistoryEvent::getOrderAfter)
                .collect(toList()), hasItem(
                hasProperty("uid", is(to.getUid()))
        ));
    }

}
