package ru.yandex.market.tpl.core.domain.routing.api;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

import lombok.Builder;

import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.external.routing.api.DimensionsClass;
import ru.yandex.market.tpl.core.external.routing.api.RoutingAddress;
import ru.yandex.market.tpl.core.external.routing.api.RoutingGeoPoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;

@Builder
public class RoutingRequestItemTestBuilder {

    @Builder.Default
    RoutingRequestItemType type = RoutingRequestItemType.CLIENT;
    @Builder.Default
    String taskId = "task-id";
    @Builder.Default
    int subTaskCount = 0;
    @Builder.Default
    String depotId = "depot-id";
    @Builder.Default
    RoutingAddress address = new RoutingAddress(
            "address", RoutingGeoPoint.ofLatLon(BigDecimal.ONE, BigDecimal.ONE)
    );
    @Builder.Default
    RelativeTimeInterval interval = new RelativeTimeInterval(
            LocalTime.of(1, 0), LocalTime.of(3, 0)
    );
    @Builder.Default
    String ref = "empty-reg";
    @Builder.Default
    Set<String> requiredTags = Set.of();
    @Builder.Default
    Set<String> optionalTags = Set.of();
    @Builder.Default
    BigDecimal volume = BigDecimal.ONE;
    @Builder.Default
    boolean excludedFromLocationGroups = false;
    @Builder.Default
    DimensionsClass dimensionsClass = DimensionsClass.REGULAR_CARGO;
    @Builder.Default
    Integer regionId = 1;
    @Builder.Default
    long additionalTimeForSurvey = 0;
    @Builder.Default
    long fashionOrdersCount = 0;
    @Builder.Default
    boolean isUserShiftRoutingRequest = false;
    @Builder.Default
    boolean logisticRequest = false;

    public RoutingRequestItem get() {
        return new RoutingRequestItem(
                type,
                taskId,
                subTaskCount,
                depotId,
                address,
                interval,
                ref,
                requiredTags,
                optionalTags,
                volume,
                excludedFromLocationGroups,
                dimensionsClass,
                regionId,
                additionalTimeForSurvey,
                fashionOrdersCount,
                isUserShiftRoutingRequest,
                logisticRequest
        );
    }

}
