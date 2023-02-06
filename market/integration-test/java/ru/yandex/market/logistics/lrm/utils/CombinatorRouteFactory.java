package ru.yandex.market.logistics.lrm.utils;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteRequest.ReturnRoutePoint;
import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteResponse;

import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@UtilityClass
@ParametersAreNonnullByDefault
public class CombinatorRouteFactory {

    @Nonnull
    public ReturnRoutePoint.Builder requestFromPoint(long partnerId, long logisticPointId) {
        return requestRoutePoint(partnerId, logisticPointId);
    }

    @Nonnull
    public ReturnRoutePoint.Builder requestToPoint(long partnerId) {
        return ReturnRoutePoint.newBuilder().setPartnerId(partnerId);
    }

    @Nonnull
    public ReturnRoutePoint.Builder requestRoutePoint(long partnerId, long logisticPointId) {
        return ReturnRoutePoint.newBuilder()
            .setPartnerId(partnerId)
            .setLogisticPointId(logisticPointId);
    }

    @Nonnull
    public ReturnRouteResponse routeResponse(ReturnRouteResponse.Point... points) {
        return ReturnRouteResponse
            .newBuilder()
            .addAllPoints(List.of(points))
            .build();
    }

    @Nonnull
    public ReturnRouteResponse.Point routeFfPoint(long segmentId, String partnerName) {
        return buildPoint(
            partnerName,
            PartnerType.FULFILLMENT,
            LogisticSegmentType.WAREHOUSE,
            segmentId
        );
    }

    @Nonnull
    public ReturnRouteResponse.Point routeScPoint(long segmentId, String partnerName) {
        return buildPoint(
            partnerName,
            PartnerType.SORTING_CENTER,
            LogisticSegmentType.WAREHOUSE,
            segmentId
        );
    }

    @Nonnull
    public ReturnRouteResponse.Point buildPoint(
        String partnerName,
        PartnerType partnerType,
        LogisticSegmentType segmentType,
        long segmentId
    ) {
        return ReturnRouteResponse.Point.newBuilder()
            .setPartnerName(partnerName)
            .setPartnerType(partnerType.name())
            .setSegmentType(segmentType.name())
            .setSegmentId(segmentId)
            .build();
    }
}
