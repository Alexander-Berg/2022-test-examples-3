package ru.yandex.market.tpl.integration.tests.facade;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingCancelOrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingOrderCancelReason;
import ru.yandex.market.tpl.api.model.tracking.TrackingRescheduleDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.integration.tests.client.PublicApiClient;
import ru.yandex.market.tpl.integration.tests.client.RecipientApiClient;

@Component
@RequiredArgsConstructor
public class RecipientApiFacade extends BaseFacade {
    private final PublicApiFacade publicApiFacade;
    private final RecipientApiClient recipientApi;
    private final PublicApiClient publicApi;

    public TrackingDto getInternalTracking() {
        OrderDeliveryTaskDto taskDto = publicApiFacade.getDeliveryTask();
        String externalOrderId = taskDto.getOrder().getExternalOrderId();
        String trackingId = recipientApi.getTrackingLinkByOrder(externalOrderId);
        getContext().setTrackingId(trackingId);
        return recipientApi.getTrackingInfo(trackingId);
    }

    public TrackingDto getTrackingByRoutePointId(long routePointId) {
        RoutePointDto routePoint = publicApi.getRoutePoint(routePointId);
        OrderDeliveryTaskDto taskDto = (OrderDeliveryTaskDto) routePoint.getTasks().iterator().next();
        String externalOrderId = taskDto.getOrder().getExternalOrderId();
        String trackingId = recipientApi.getTrackingLinkByOrder(externalOrderId);
        getContext().setTrackingId(trackingId);
        return recipientApi.getTrackingInfo(trackingId);
    }

    public TrackingDto cancelOrderInTracking() {
        TrackingCancelOrderDto cancelOrderDto = new TrackingCancelOrderDto();
        cancelOrderDto.setCancelReasonType(TrackingOrderCancelReason.CHANGED_MIND);
        return recipientApi.cancelTrackingOrder(getContext().getTrackingId(), cancelOrderDto);
    }

    public TrackingDto reschedule() {
        var dates = recipientApi.getRescheduleDatesForOrder(getContext().getTrackingId());
        var rescheduleDate = dates.entrySet().iterator().next();
        var rescheduleTimeInterval = LocalTimeInterval.valueOf(rescheduleDate.getValue().iterator().next()
                .replaceAll("\\s", ""));
        var rescheduleInterval = rescheduleTimeInterval.toInterval(LocalDate.parse(rescheduleDate.getKey()),
                DateTimeUtil.DEFAULT_ZONE_ID);
        var trackingRescheduleDto = new TrackingRescheduleDto();
        trackingRescheduleDto.setDeliveryIntervalFrom(rescheduleInterval.getStart());
        trackingRescheduleDto.setDeliveryIntervalTo(rescheduleInterval.getEnd());
        return recipientApi.reschedule(getContext().getTrackingId(), trackingRescheduleDto);
    }
}
