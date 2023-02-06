package ru.yandex.market.checkout.checkouter.order.cancellationrequest;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.NotificationResultStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.notification.NotificationResults;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.USER_CHANGED_MIND;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;
import static ru.yandex.market.checkout.util.balance.checkers.CheckBasketParams.buildPostAuth;


public class CancellationRequestDeliveryPickupTest extends AbstractWebTestBase {

    private static final String NOTES = "notes";

    private static final ClientInfo USER_CLIENT_INFO = new ClientInfo(ClientRole.USER, BuyerProvider.UID);

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    /**
     * Здесь проверяем только общий механизм отмены заказа при получение чекпоинта 70 (RETURN_ARRIVED)
     * Детальная проверка связок clientInfo + status + subStatus в
     * {@link CancellationRequestDeliveryPickupByStatusesTest}
     */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void createCancellationRequest(boolean isCreateByOrderEditApi) throws Exception {
        ClientInfo clientInfo = USER_CLIENT_INFO;
        OrderStatus status = OrderStatus.DELIVERY;
        OrderSubstatus substatus = USER_CHANGED_MIND;

        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.PICKUP)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        order = orderStatusHelper.proceedOrderToStatus(order, status);
        CancellationRequest cancellationRequest = new CancellationRequest(substatus, NOTES);
        Order response;
        if (isCreateByOrderEditApi) {
            cancellationRequestHelper.createCancellationRequestByEditApi(
                    order.getId(), cancellationRequest, clientInfo);
            response = orderGetHelper.getOrder(order.getId(), clientInfo);
        } else {
            response = cancellationRequestHelper.createCancellationRequest(
                    order.getId(), cancellationRequest, clientInfo);
        }
        assertEquals(substatus, response.getCancellationRequest().getSubstatus());
        Assertions.assertEquals(NOTES, response.getCancellationRequest().getNotes());

        Order orderFromDB = orderService.getOrder(order.getId());
        assertEquals(substatus, orderFromDB.getCancellationRequest().getSubstatus());
        Assertions.assertEquals(NOTES, orderFromDB.getCancellationRequest().getNotes());

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        trustMockConfigurer.resetAll();
        trustMockConfigurer.mockCheckBasket(buildPostAuth());
        trustMockConfigurer.mockStatusBasket(buildPostAuth(), null);
        trustMockConfigurer.mockCreateRefund(null);
        trustMockConfigurer.mockDoRefund();

        NotificationResults results =
                notifyTracksHelper.notifyTracksForResult(DeliveryTrackProvider.getDeliveryTrack(
                        MockTrackerHelper.TRACKER_ID, 70));

        assertThat(results.getResults().get(0).getStatus(), CoreMatchers.is(NotificationResultStatus.OK));

        orderFromDB = orderService.getOrder(order.getId());
        assertEquals(CANCELLED, orderFromDB.getStatus());
        assertEquals(substatus, orderFromDB.getSubstatus());
    }
}
