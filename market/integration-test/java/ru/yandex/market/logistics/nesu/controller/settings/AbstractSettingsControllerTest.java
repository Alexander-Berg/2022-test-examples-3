package ru.yandex.market.logistics.nesu.controller.settings;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartner;

abstract class AbstractSettingsControllerTest extends AbstractContextualTest {

    @Autowired
    protected LMSClient lmsClient;

    @BeforeEach
    void prepare() {
        SearchPartnerFilter filterDeliveryService = LmsFactory.createPartnerFilter(
            null,
            Set.of(PartnerStatus.ACTIVE),
            Set.of(PartnerType.DELIVERY, PartnerType.OWN_DELIVERY)
        );
        PartnerResponse firstValid = createPartner(1L, PartnerType.DELIVERY);
        PartnerResponse secondValid = createPartner(2L, PartnerType.DELIVERY);
        PartnerResponse thirdValid = createPartner(8L, PartnerType.DELIVERY);
        when(lmsClient.searchPartners(filterDeliveryService))
            .thenAnswer(invocation -> List.of(firstValid, secondValid, thirdValid));
        when(lmsClient.getPartner(4L))
            .thenAnswer(invocation -> Optional.of(createPartner(4L, PartnerType.DELIVERY)));

        when(lmsClient.getPartner(3L))
            .thenAnswer(invocation -> Optional.of(PartnerResponse.newBuilder()
                .id(3L)
                .partnerType(PartnerType.FULFILLMENT)
                .logoUrl("http://test_logo_url/" + 3)
                .build()
            ));

        SearchPartnerFilter filterSortingCenters = LmsFactory.createPartnerFilter(PartnerType.SORTING_CENTER);
        when(lmsClient.searchPartners(filterSortingCenters))
            .thenAnswer(invocation -> List.of(
                createPartner(10L, PartnerType.SORTING_CENTER),
                createPartner(11L, PartnerType.SORTING_CENTER)
            ));

        Set<Long> presentDeliveryIds = Set.of(1L, 2L, 3L, 4L, 8L, 10L, 11L);
        when(lmsClient.getPartner(longThat(argument -> !presentDeliveryIds.contains(argument))))
            .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
    }

    protected void mockAvailableWarehousesAndPartners() {
        when(
            lmsClient.getLogisticsPoints(
                LogisticsPointFilter.newBuilder()
                    .active(true)
                    .type(PointType.WAREHOUSE)
                    .ids(any())
                    .build()
            )
        ).thenAnswer(invocation ->
            invocation.<LogisticsPointFilter>getArgument(0).getIds().stream()
                .map(id -> LmsFactory.createLogisticsPointResponse(id, id / 10, "warehouse " + id, PointType.WAREHOUSE))
                .collect(Collectors.toList())
        );

        when(
            lmsClient.searchPartners(
                SearchPartnerFilter.builder()
                    .setStatuses(Set.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
                    .setPlatformClientStatuses(Set.of(PartnerStatus.ACTIVE))
                    .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
                    .setIds(Set.of(1L, 2L, 8L, 10L, 11L))
                    .build()
            )
        ).thenReturn(
            List.of(
                LmsFactory.createPartner(1L, PartnerType.DELIVERY),
                LmsFactory.createPartner(2L, PartnerType.DELIVERY),
                LmsFactory.createPartner(8L, PartnerType.DELIVERY),
                LmsFactory.createPartner(10L, PartnerType.SORTING_CENTER),
                LmsFactory.createPartner(11L, PartnerType.SORTING_CENTER)
            )
        );

        when(
            lmsClient.searchPartners(
                SearchPartnerFilter.builder()
                    .setStatuses(Set.of(PartnerStatus.ACTIVE))
                    .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
                    .setMarketIds(Set.of(200L))
                    .setTypes(Set.of(PartnerType.OWN_DELIVERY))
                    .build()
            )
        ).thenReturn(
            List.of(LmsFactory.createPartner(100L, PartnerType.OWN_DELIVERY))
        );
    }

    protected void mockSortingCenterRelation() {
        when(lmsClient.searchPartnerRelation(
            PartnerRelationFilter.newBuilder()
                .fromPartnersIds(Set.of(10L))
                .enabled(true)
                .build()
        )).thenReturn(List.of(
            PartnerRelationEntityDto.newBuilder()
                .toPartnerId(1L)
                .shipmentType(ShipmentType.WITHDRAW)
                .build()
        ));
    }
}
