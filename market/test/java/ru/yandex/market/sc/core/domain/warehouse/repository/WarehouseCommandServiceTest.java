package ru.yandex.market.sc.core.domain.warehouse.repository;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageRequest;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.DistributionCenterWarehouseRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@EmbeddedDbTest
class WarehouseCommandServiceTest {

    @MockBean
    LMSClient lmsClient;
    @Autowired
    WarehouseCommandService warehouseCommandService;
    @Autowired
    DistributionCenterWarehouseRepository distributionCenterWarehouseRepository;
    @Autowired
    TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void updateSCWarehouse() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "true");
        Warehouse warehouse = testFactory.storedWarehouse("111112");
        when(lmsClient.searchPartnerRelation(
                PartnerRelationFilter.newBuilder()
                        .enabled(true)
                        .fromPartnersIds(Set.of(sortingCenter.getId()))
                        .build(),
                new PageRequest(0, 1000)
        )).thenReturn(
                new PageResult<PartnerRelationEntityDto>().setData(List.of(PartnerRelationEntityDto.newBuilder()
                        .fromPartnerId(sortingCenter.getId())
                        .toPartnerId(Long.parseLong(Objects.requireNonNull(warehouse.getPartnerId())))
                        .build()))
                        .setPage(0)
                        .setSize(1)
                        .setTotalElements(1)
                        .setTotalPages(1)
        );

        when(lmsClient.getLogisticsPoints(
                LogisticsPointFilter.newBuilder()
                        .partnerIds(Set.of(Long.parseLong(warehouse.getPartnerId())))
                        .active(true)
                        .build()
        ))
                .thenReturn(List.of(LogisticsPointResponse.newBuilder()
                        .active(true)
                        .partnerId(Long.parseLong(warehouse.getPartnerId()))
                        .id(Long.parseLong(Objects.requireNonNull(warehouse.getYandexId())))
                        .build()));

        assertThat(distributionCenterWarehouseRepository.findAll()).hasSize(0);
        warehouseCommandService.updateWarehousesForSortingCenters();
        assertThat(distributionCenterWarehouseRepository.findAll()).hasSize(1);
    }
}
