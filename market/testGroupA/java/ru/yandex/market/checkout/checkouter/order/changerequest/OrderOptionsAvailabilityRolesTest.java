package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
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
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.CALL_CENTER_OPERATOR;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.SYSTEM;
import static ru.yandex.market.checkout.checkouter.client.ClientRole.USER;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType.CARRIER;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

public class OrderOptionsAvailabilityRolesTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                new Object[]{
                        USER,
                        BuyerProvider.UID,
                        DeliveryCheckpointStatus.DELIVERY_AT_START,
                        List.of()
                },
                new Object[]{
                        CALL_CENTER_OPERATOR,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_AT_START,
                        List.of()
                },
                new Object[]{
                        SYSTEM,
                        1L,
                        DeliveryCheckpointStatus.DELIVERY_AT_START,
                        List.of()
                },
                new Object[]{
                        USER,
                        BuyerProvider.UID,
                        DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT,
                        List.of(new OptionAvailability(AvailableOptionType.SHOW_RUNNING_COURIER))
                },
                new Object[]{
                        CALL_CENTER_OPERATOR,
                        0L,
                        DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT,
                        List.of(new OptionAvailability(AvailableOptionType.SHOW_RUNNING_COURIER))
                },
                new Object[]{
                        SYSTEM,
                        1L,
                        DeliveryCheckpointStatus.DELIVERY_TRANSPORTATION_RECIPIENT,
                        List.of(new OptionAvailability(AvailableOptionType.SHOW_RUNNING_COURIER))
                },
                new Object[]{
                        USER,
                        BuyerProvider.UID,
                        DeliveryCheckpointStatus.DELIVERY_TRANSMITTED_TO_RECIPIENT,
                        List.of()
                }
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void roleAvailabilityTest(ClientRole requestRole, Long requestRoleId,
                                     DeliveryCheckpointStatus receivedCheckpoint,
                                     List<OptionAvailability> allAvailabilities) throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.DELIVERY)
                .withColor(BLUE)
                .withShopId(OrderProvider.SHOP_ID_WITH_SORTING_CENTER)
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);

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

        final List<OrderOptionAvailability> orderOptionsAvailabilities = client.getOrderOptionsAvailabilities(
                Set.of(order.getId()),
                requestRole,
                requestRoleId
        );

        assertThat(orderOptionsAvailabilities, hasSize(1));
        final OrderOptionAvailability optionAvailability = orderOptionsAvailabilities.get(0);
        assertThat(optionAvailability, hasProperty("orderId", is(order.getId())));
        assertThat(optionAvailability.getAvailableOptions(), containsInAnyOrder(allAvailabilities.toArray()));
    }
}
