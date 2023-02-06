package ru.yandex.market.logistics.nesu.controller.shipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.lom.model.dto.CourierDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.ShipmentSearchDto;
import ru.yandex.market.logistics.lom.model.dto.TimeIntervalDto;
import ru.yandex.market.logistics.lom.model.enums.ShipmentApplicationStatus;
import ru.yandex.market.logistics.lom.model.enums.ShipmentType;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponse;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartner;

@ParametersAreNonnullByDefault
public final class ShipmentTestUtils {

    private ShipmentTestUtils() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    static ShipmentSearchDto createShipment(
        long partnerIdTo,
        @Nullable CourierDto courier,
        boolean hasAcceptanceCertificate
    ) {
        return createShipmentBuilder()
            .partnerIdTo(partnerIdTo)
            .courier(courier)
            .hasAcceptanceCertificate(hasAcceptanceCertificate)
            .build();
    }

    @Nonnull
    public static ShipmentSearchDto createShipment(long partnerIdTo, @Nullable CourierDto courier) {
        return createShipment(null, partnerIdTo, courier);
    }

    @Nonnull
    public static ShipmentSearchDto createShipment(@Nullable Long id, long partnerIdTo, @Nullable CourierDto courier) {
        return createShipment(id, partnerIdTo, courier, null, null);
    }

    @Nonnull
    public static ShipmentSearchDto createShipment(
        @Nullable Long id,
        long partnerIdTo,
        @Nullable CourierDto courier,
        @Nullable PageResult<OrderDto> orders,
        @Nullable BigDecimal ordersTotalCost
    ) {
        return createShipmentBuilder()
            .id(id)
            .partnerIdTo(partnerIdTo)
            .ordersTotalCost(ordersTotalCost)
            .courier(courier)
            .orders(orders)
            .build();
    }

    private static ShipmentSearchDto.ShipmentSearchDtoBuilder createShipmentBuilder() {
        LocalDate date = LocalDate.of(2019, 5, 30);
        return ShipmentSearchDto.builder()
            .marketIdFrom(1L)
            .shipmentDate(date)
            .shipmentType(ShipmentType.IMPORT)
            .warehouseFrom(1L)
            .warehouseTo(2L)
            .marketIdTo(2L)
            .korobyteDto(new KorobyteDto(4, 10, 20, new BigDecimal(50)))
            .interval(new TimeIntervalDto(LocalTime.of(10, 0), LocalTime.of(15, 0)))
            .status(ShipmentApplicationStatus.DELIVERY_SERVICE_PROCESSING)
            .cost(new BigDecimal(300))
            .comment("test_comment")
            .applicationId(5L)
            .created(date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    public static void mockGetPartnersById(long partnerId, LMSClient lmsClient) {
        when(lmsClient.searchPartners(refEq(
            SearchPartnerFilter.builder()
                .setIds(Set.of(partnerId))
                .build()
        )))
            .thenReturn(List.of(createPartner(partnerId, partnerId, PartnerType.DELIVERY)));
    }

    public static void mockGetPartnersByIds(Set<Long> ids, LMSClient lmsClient) {
        when(lmsClient.searchPartners(refEq(
            SearchPartnerFilter.builder()
                .setIds(ids)
                .build()
        )))
            .thenReturn(
                ids.stream()
                    .map(id -> createPartner(id, id, PartnerType.DELIVERY))
                    .collect(Collectors.toList())
            );
    }

    public static void mockGetLogisticsPoints(Set<Long> ids, LMSClient client) {
        mockGetLogisticsPoints(ids, client, PointType.WAREHOUSE);
    }

    public static void mockGetLogisticsPoints(Set<Long> ids, LMSClient client, @Nullable PointType pointType) {
        List<LogisticsPointResponse> response = ids.stream()
            .map(id -> createLogisticsPointResponse(id, id, "warehouseName", pointType))
            .collect(Collectors.toList());
        mockGetLogisticsPoints(ids, response, client, pointType);
    }

    public static void mockGetLogisticsPoints(Set<Long> ids, List<LogisticsPointResponse> response, LMSClient client) {
        mockGetLogisticsPoints(ids, response, client, PointType.WAREHOUSE);
    }

    public static void mockGetLogisticsPoints(
        Set<Long> ids,
        List<LogisticsPointResponse> response,
        LMSClient client,
        @Nullable PointType pointType
    ) {
        when(client.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder().ids(ids).type(pointType).build())))
            .thenReturn(response);
    }
}
