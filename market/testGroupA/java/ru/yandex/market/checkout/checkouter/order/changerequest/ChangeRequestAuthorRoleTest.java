package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.recipient.RecipientEditRequest;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.RecipientProvider;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.USER;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PROCESSING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;


public class ChangeRequestAuthorRoleTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{USER, BuyerProvider.UID},
                new Object[]{SYSTEM, 0L}
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldSaveAuthorRole(ClientRole requestRole, Long requestUserId) {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, PROCESSING);

        RecipientEditRequest recipientEditRequest = new RecipientEditRequest();
        recipientEditRequest.setPerson(RecipientProvider.getDefaultRecipient().getPerson());
        recipientEditRequest.setPhone(RecipientProvider.getDefaultRecipient().getPhone());

        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setRecipientEditRequest(recipientEditRequest);

        List<ChangeRequest> changeRequests = client.editOrder(order.getId(), requestRole, requestUserId,
                singletonList(BLUE), orderEditRequest);
        assertNotNull(changeRequests);
        MatcherAssert.assertThat(changeRequests, hasSize(1));

        ChangeRequest changeRequest = changeRequests.iterator().next();

        assertEquals(requestRole, changeRequest.getRole());

        // Проверяем, что в выдаче тоже проросло
        Order orderAfter = client.getOrder(order.getId(), ClientRole.SYSTEM, 0L,
                Sets.newHashSet(OptionalOrderPart.CHANGE_REQUEST));
        assertEquals(requestRole, orderAfter.getChangeRequests().iterator().next().getRole());
    }
}
