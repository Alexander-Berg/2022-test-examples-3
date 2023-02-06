package ru.yandex.market.logistics.nesu.base.partner;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter.TransportationSearchFilterBuilder;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics4shops.client.api.OutboundApi;
import ru.yandex.market.logistics4shops.client.model.Outbound;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DatabaseSetup("/repository/partner-shipment/common.xml")
abstract class AbstractPartnerShipmentTest extends AbstractContextualTest {
    protected static final MediaType XLSX_MIME_TYPE =
        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    protected static final long SHOP_ID = 10;
    protected static final long SECOND_SHOP_ID = 20;

    protected static final String ORDER_ID = "100";

    protected static final long CHECKOUTER_ORDER_ID = 100L;

    protected static final byte[] BYTES = new byte[10];

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected TransportManagerClient transportManagerClient;

    @Autowired
    protected OutboundApi outboundApi;

    @Autowired
    protected LomClient lomClient;

    @Autowired
    protected CheckouterAPI checkouterAPI;

    @Autowired
    protected WwClient wwClient;

    @Autowired
    protected MdsS3Client mdsS3Client;

    protected void mockTransportationSearch(
        TransportationSearchFilterBuilder transportationFilter,
        List<TransportationSearchDto> result
    ) {
        when(transportManagerClient.searchTransportations(
            transportationFilter.build(),
            new PageRequest(0, Integer.MAX_VALUE)
        )).thenReturn(new Page<TransportationSearchDto>().setData(result));
    }

    protected void mockExpressPartner(long expressPartnerId) {
        when(lmsClient.searchPartners(SearchPartnerFilter.builder().setPartnerSubTypeIds(Set.of(34L)).build()))
            .thenReturn(List.of(LmsFactory.createPartner(expressPartnerId, PartnerType.DELIVERY)));
    }

    protected void mockWarehouseFrom() {
        when(lmsClient.getLogisticsPoint(TMFactory.WAREHOUSE_FROM)).thenReturn(Optional.of(
            warehouse(TMFactory.WAREHOUSE_FROM, "Какой-то склад", "Один адрес")
        ));
    }

    protected void mockWarehouses() {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(TMFactory.WAREHOUSE_FROM, TMFactory.WAREHOUSE_TO))
                .build()
        )).thenReturn(List.of(
            warehouse(TMFactory.WAREHOUSE_FROM, TMFactory.PARTNER_ID, "Какой-то склад", "Один адрес"),
            warehouse(TMFactory.WAREHOUSE_TO, "Какой-то другой склад", "Другой адрес")
        ));
    }

    @Nonnull
    protected LogisticsPointResponse warehouse(long id, String name, String address) {
        return warehouse(id, null, name, address);
    }

    @Nonnull
    protected LogisticsPointResponse warehouse(long id, @Nullable Long partnerId, String name, String address) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .name(name)
            .type(PointType.WAREHOUSE)
            .partnerId(partnerId)
            .address(
                Address.newBuilder()
                    .locationId(213)
                    .addressString(address)
                    .shortAddressString(address + " (короткий)")
                    .build()
            )
            .build();
    }

    protected void mockPartnerRelation() {
        mockPartnerRelation(TMFactory.PARTNER_ID);
    }

    protected void mockPartnerRelation(long partnerId) {
        doReturn(List.of(
            PartnerRelationEntityDto.newBuilder()
                .enabled(true)
                .cutoffs(Set.of(CutoffResponse.newBuilder().cutoffTime(LocalTime.of(15, 0)).build()))
                .fromPartnerId(partnerId)
                .build()
        ))
            .when(lmsClient)
            .searchPartnerRelation(
                PartnerRelationFilter.newBuilder()
                    .fromPartnerId(partnerId)
                    .build()
            );
    }

    protected void mockPartnerRelations(Set<Long> partnerIds) {
        doReturn(
            partnerIds.stream()
                .map(partnerId -> PartnerRelationEntityDto.newBuilder()
                    .enabled(true)
                    .cutoffs(Set.of(CutoffResponse.newBuilder().cutoffTime(LocalTime.of(15, 0)).build()))
                    .fromPartnerId(partnerId)
                    .build()
                )
                .collect(Collectors.toList())
        )
            .when(lmsClient)
            .searchPartnerRelation(
                PartnerRelationFilter.newBuilder()
                    .fromPartnersIds(partnerIds)
                    .build()
            );
    }

    protected void mockHandlingTime(int days) {
        mockHandlingTime(days, TMFactory.PARTNER_ID);
    }

    protected void mockHandlingTime(int days, long partnerId) {
        doReturn(Duration.ofDays(days))
            .when(lmsClient)
            .getWarehouseHandlingDuration(partnerId);
    }

    protected void mockTransportationUnit() {
        Outbound outbound = L4ShopsFactory.outbound(List.of(ORDER_ID));
        outbound.setExternalId("external-id");
        when(outboundApi.searchOutbounds(safeRefEq(L4ShopsFactory.outboundIds(1))))
            .thenReturn(new OutboundsListDto().outbounds(List.of(outbound)));
    }

    protected void mockSearchOrders(List<OrderDto> orders) {
        when(lomClient.searchOrders(
            OrderSearchFilter.builder()
                .platformClientId(1L)
                .externalIds(Set.of(ORDER_ID))
                .build(),
            Pageable.unpaged()
        )).thenReturn(new PageResult<OrderDto>().setData(orders));
    }
}
