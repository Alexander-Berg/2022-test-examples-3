package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.DeliveryCheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.BUSINESS;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.CALL_CENTER_OPERATOR;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SHOP;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.USER;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType.CARRIER;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;


public class OrderEditPossibilityRolesTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;

    @SuppressWarnings("checkstyle:MethodLength")
    public static Stream<Arguments> parameterizedTestData() {

        return Arrays.asList(
                new Object[]{
                        USER,
                        BuyerProvider.UID,
                        DeliveryCheckpointStatus.DELIVERY_AT_START,
                        true,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        CALL_CENTER_OPERATOR,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_LOADED,
                        true,
                        true,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        CALL_CENTER_OPERATOR,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_AT_START,
                        true,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        USER,
                        BuyerProvider.UID,
                        DeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT,
                        true,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        CALL_CENTER_OPERATOR,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT,
                        true,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        SYSTEM,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_AT_START,
                        true,
                        true,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        SYSTEM,
                        BuyerProvider.UID,
                        DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT,
                        true,
                        true,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        USER,
                        BuyerProvider.UID,
                        DeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT,
                        false,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        SHOP,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT,
                        true,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        SHOP,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_AT_START,
                        true,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        BUSINESS,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_ARRIVED_PICKUP_POINT,
                        true,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                },
                new Object[]{
                        BUSINESS,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_AT_START,
                        true,
                        false,
                        List.of(
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_DATES, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_PHONE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.RECIPIENT, MethodOfChange.PARTNER_SITE,
                                        ChangeRequestAvailability.ENABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_COURIER,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE_PICKUP,
                                        MethodOfChange.PARTNER_API, ChangeRequestAvailability.DISABLED),
                                new EditPossibility(ChangeRequestType.DELIVERY_LAST_MILE, MethodOfChange.PARTNER_API,
                                        ChangeRequestAvailability.DISABLED))
                }
        ).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void rolePossibilityTest(ClientRole requestRole, Long requestRoleId,
                                    DeliveryCheckpointStatus receivedCheckpoint, boolean deliveryDatesPossible,
                                    boolean recipientPossible, List<EditPossibility> allPossibilities)
            throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(CARRIER);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        DeliveryCheckpointStatus dcs;
        Iterator<DeliveryCheckpointStatus> iterator = Arrays.asList(DeliveryCheckpointStatus.values()).iterator();
        long trackerCheckpointId = 0;
        do {
            dcs = iterator.next();

            notifyTracksHelper.notifyTracks(
                    DeliveryTrackProvider.getDeliveryTrack(
                            MockTrackerHelper.TRACKER_ID,
                            dcs.getId(),
                            trackerCheckpointId++
                    )
            );
        } while (iterator.hasNext() && dcs.getId() < receivedCheckpoint.getId());

        List<OrderEditPossibility> orderEditPossibilityList = client.getOrderEditPossibilities(
                Set.of(order.getId()),
                requestRole,
                requestRoleId,
                List.of(Color.BLUE)
        );

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        OrderEditPossibility orderEditPossibility = orderEditPossibilityList.get(0);
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        EditPossibilityWrapper editPossibilityWrapper =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());

        assertEquals(recipientPossible, editPossibilityWrapper.isPossible(ChangeRequestType.RECIPIENT));
        assertEquals(deliveryDatesPossible, editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapper.isPossible(ChangeRequestType.DELIVERY_ADDRESS));

        assertThat(orderEditPossibility.getEditPossibilities(), containsInAnyOrder(allPossibilities.toArray()));

        orderEditPossibilityList = client.getOrderEditPossibilities(
                Collections.singleton(order.getId()), SYSTEM, 0L, Collections.singletonList(Color.BLUE));

        assertThat(orderEditPossibilityList, Matchers.hasSize(1));
        orderEditPossibility = orderEditPossibilityList.get(0);
        EditPossibilityWrapper editPossibilityWrapperBySystem =
                EditPossibilityWrapper.build(orderEditPossibility.getEditPossibilities());
        assertThat(orderEditPossibility, hasProperty("orderId", is(order.getId())));
        assertTrue(editPossibilityWrapperBySystem.isPossible(ChangeRequestType.RECIPIENT));
        assertTrue(editPossibilityWrapperBySystem.isPossible(ChangeRequestType.DELIVERY_DATES));
        assertFalse(editPossibilityWrapperBySystem.isPossible(ChangeRequestType.DELIVERY_ADDRESS));
    }
}
