package ru.yandex.market.checkout.checkouter.delivery;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;


public class HideCheckpointForUserTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    private static Object[] checkpointStatusCase(int status, boolean isHiddenForUser) {
        return new Object[]{status, isHiddenForUser};
    }

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                checkpointStatusCase(0, false),
                checkpointStatusCase(1, false),
                checkpointStatusCase(2, true),
                checkpointStatusCase(10, false),
                checkpointStatusCase(20, false),
                checkpointStatusCase(30, false),
                checkpointStatusCase(38, true),
                checkpointStatusCase(39, true),
                checkpointStatusCase(40, false),
                checkpointStatusCase(45, false),
                checkpointStatusCase(46, false),
                checkpointStatusCase(47, false),
                checkpointStatusCase(48, false),
                checkpointStatusCase(49, true),
                checkpointStatusCase(50, false),
                checkpointStatusCase(60, true),
                checkpointStatusCase(70, true),
                checkpointStatusCase(80, true),
                checkpointStatusCase(100, true),
                checkpointStatusCase(101, true),
                checkpointStatusCase(102, true),
                checkpointStatusCase(105, true),
                checkpointStatusCase(110, true),
                checkpointStatusCase(120, true),
                checkpointStatusCase(130, true),
                checkpointStatusCase(160, true),
                checkpointStatusCase(170, true),
                checkpointStatusCase(175, true),
                checkpointStatusCase(177, true),
                checkpointStatusCase(180, true),
                checkpointStatusCase(195, true),
                checkpointStatusCase(197, true),
                checkpointStatusCase(199, true),
                checkpointStatusCase(403, false),
                checkpointStatusCase(404, true),
                checkpointStatusCase(410, false),
                checkpointStatusCase(-1, true)
        ).collect(Collectors.toList()).stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void hideDeliveryTransmittedToRecipient(int checkpointStatus, boolean isHiddenForUser) throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(DeliveryServiceType.CARRIER);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID,
                checkpointStatus));

        order = orderService.getOrder(order.getId());
        assertThat(
                order.getDelivery()
                        .getParcels().iterator().next()
                        .getTracks().iterator().next()
                        .getCheckpoints(),
                contains(
                        hasProperty("deliveryCheckpointStatus", is(checkpointStatus))
                )
        );

        order = orderGetHelper.getOrder(order.getId(), new ClientInfo(ClientRole.USER, BuyerProvider.UID));
        if (isHiddenForUser) {
            assertThat(order.getDelivery().getParcels().get(0).getTracks().get(0).getCheckpoints(), is(empty()));
        } else {
            assertThat(order.getDelivery().getParcels().get(0).getTracks().get(0).getCheckpoints(), hasSize(1));
        }
    }

}
