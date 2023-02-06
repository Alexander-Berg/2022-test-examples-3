package ru.yandex.market.checkout.checkouter.tasks.v2;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.delivery.track.DeliveryTrackerService;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.storage.track.registration.TrackRegistration;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryTrackCheckpointProvider;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.util.ClientHelper;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackMeta;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueParametersWithDelivery;

public class RegisterDeliveryTrackReturnTaskV2Test extends AbstractReturnTestBase {

    @Autowired
    DeliveryTrackerService deliveryTrackerService;
    @Autowired
    RegisterDeliveryTrackReturnTaskV2 registerDeliveryTrackReturnTaskV2;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;

    @Test
    public void notifyTracksTest() throws Exception {
        var trackCodeNumber = new AtomicLong(100);
        var seed = trackCodeNumber.getAndIncrement();
        var uniqueTrackCode = "TrackCode" + seed;
        var uniqueTrackerId = 111555L + seed;
        var meta = new DeliveryTrackMeta(
                uniqueTrackCode,
                new DeliveryService(DELIVERY_SERVICE_ID, "a", DeliveryServiceType.DELIVERY),
                Long.toString(uniqueTrackerId),
                EntityType.ORDER_RETURN,
                null
        ).setId(uniqueTrackerId);
        Mockito.doReturn(meta).when(deliveryTrackerService).registerDeliveryTrack(Mockito.any(TrackRegistration.class),
                Mockito.any(EntityType.class));
        Mockito.doReturn(Set.of(DELIVERY_SERVICE_ID)).when(deliveryTrackerService)
                .getSupportedByTrackerDeliveryServiceIds();

        Parameters params = defaultBlueParametersWithDelivery(DELIVERY_SERVICE_ID);
        params.getOrder().getItems().forEach(item -> item.setCount(10));
        Order order = orderCreateHelper.createOrder(params);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        Return request = prepareDefaultReturnRequest(order, DeliveryType.DELIVERY);
        Return ret = client.returns().initReturn(order.getId(), ClientRole.SYSTEM, order.getBuyer().getUid(), request);
        request = ReturnHelper.copy(ret);
        client.returns().resumeReturn(order.getId(), ret.getId(), ClientRole.REFEREE, ClientHelper.REFEREE_UID,
                request);
        client.returns().setReturnTrackCode(order.getId(), ret.getId(), ClientRole.SYSTEM, order.getBuyer().getUid(),
                uniqueTrackCode);
        ret = client.returns().getReturn(order.getId(), ret.getId(), false, ClientRole.SYSTEM, 123L);

        var result = registerDeliveryTrackReturnTaskV2.run(TaskRunType.ONCE);
        Assertions.assertEquals(TaskStageType.SUCCESS, result.getStage(), result.toString());
        notifyTracksHelper.notifyTracks(
                DeliveryTrackProvider.getDeliveryTrack(
                        String.valueOf(order.getId()),
                        uniqueTrackerId,
                        uniqueTrackCode,
                        DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(11, 1),
                        DeliveryTrackCheckpointProvider.deliveryTrackCheckpoint(12, 3)
                )
        );

        Return returnResp = client.returns().getReturn(order.getId(),
                ret.getId(), false, ClientRole.SYSTEM, 123L);
        assertThat(returnResp.getDelivery().getTrack().getCheckpoints(), hasSize(2));
    }
}
