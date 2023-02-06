package ru.yandex.market.logistics.nesu.admin;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponse;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointsFilter;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartnerResponseBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение настроек служб доставки сендера")
@DatabaseSetup("/controller/admin/sender-get-delivery-settings/before/setup.xml")
class SenderGetDeliverySettingsTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешное получение настроек")
    @JpaQueriesCount(2)
    void standardSetup() throws Exception {
        mockGetWarehouses(Set.of(10L));
        mockGetPartners(Set.of(10L), Set.of(1L));

        getSenderDeliverySettings()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-get-delivery-settings/response/default_settings.json"));

        verifyGetWarehouses(Set.of(10L));
        verifyGetPartners(Set.of(1L, 10L));
    }

    @Test
    @DisplayName("Неактивный сендер")
    @JpaQueriesCount(2)
    @DatabaseSetup(
        value = "/controller/admin/sender-get-delivery-settings/before/deleted_sender.xml",
        type = DatabaseOperation.UPDATE
    )
    void deletedSender() throws Exception {
        mockGetWarehouses(Set.of(10L));
        mockGetPartners(Set.of(10L), Set.of(1L));

        getSenderDeliverySettings()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-get-delivery-settings/response/default_settings.json"));

        verifyGetWarehouses(Set.of(10L));
        verifyGetPartners(Set.of(1L, 10L));
    }

    @Test
    @JpaQueriesCount(1)
    @DisplayName("Несуществующий сендер")
    void notExistingSender() throws Exception {
        getSenderDeliverySettings(2)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [2]"));
    }

    @Test
    @DisplayName("Отсутствие настроек")
    @DatabaseSetup("/controller/admin/sender-get-delivery-settings/before/no_settings.xml")
    void noSettings() throws Exception {
        getSenderDeliverySettings()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-get-delivery-settings/response/no_settings.json"));
    }

    @Test
    @DisplayName("Несколько настроек в разных регионах")
    @JpaQueriesCount(2)
    @DatabaseSetup(
        value = "/controller/admin/sender-get-delivery-settings/before/several_regions.xml",
        type = DatabaseOperation.INSERT
    )
    void severalRegions() throws Exception {
        mockGetWarehouses(Set.of(10L, 20L, 30L));
        mockGetPartners(Set.of(10L, 20L, 30L), Set.of(1L, 2L));

        getSenderDeliverySettings()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-get-delivery-settings/response/several_regions.json"));

        verifyGetWarehouses(Set.of(10L, 20L, 30L));
        verifyGetPartners(Set.of(1L, 2L, 10L, 20L, 30L));
    }

    @Test
    @DisplayName("Не проставлен партнёр у склада")
    void sortingCenterWithoutPartner() throws Exception {
        when(lmsClient.getLogisticsPoints(createWarehouseFilter(Set.of(10L))))
            .thenReturn(List.of(createLogisticsPointResponse(10L, null, "sc-warehouse-10", PointType.WAREHOUSE)));
        mockGetPartners(Set.of(), Set.of(1L));

        getSenderDeliverySettings()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-get-delivery-settings/response/no_sc_partner.json"));

        verifyGetWarehouses(Set.of(10L));
        verifyGetPartners(Set.of(1L));
    }

    @Test
    @DisplayName("Пустые имена складов и партнёров")
    void emptyNames() throws Exception {
        when(lmsClient.getLogisticsPoints(createWarehouseFilter(Set.of(10L))))
            .thenReturn(List.of(createLogisticsPointResponse(10L, 10L, "", PointType.WAREHOUSE)));
        when(lmsClient.searchPartners(createPartnerFilter(Set.of(1L, 10L))))
            .thenReturn(List.of(
                createPartnerResponseBuilder(10L, PartnerType.SORTING_CENTER, 10L).name(null).build(),
                createPartnerResponseBuilder(1L, PartnerType.DELIVERY, 1L).name(" ").build()
            ));

        getSenderDeliverySettings()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/sender-get-delivery-settings/response/empty_names.json"));

        verifyGetWarehouses(Set.of(10L));
        verifyGetPartners(Set.of(1L, 10L));
    }

    private void mockGetWarehouses(Set<Long> warehouseIds) {
        when(lmsClient.getLogisticsPoints(createWarehouseFilter(warehouseIds)))
            .thenReturn(
                warehouseIds.stream()
                    .map(id -> createLogisticsPointResponse(id, id, "sc-warehouse-" + id, PointType.WAREHOUSE))
                    .collect(Collectors.toList())
            );
    }

    private void verifyGetWarehouses(Set<Long> warehouseIds) {
        verify(lmsClient).getLogisticsPoints(createWarehouseFilter(warehouseIds));
    }

    @Nonnull
    private LogisticsPointFilter createWarehouseFilter(Set<Long> warehouseIds) {
        return createLogisticsPointsFilter(warehouseIds, null, PointType.WAREHOUSE, null);
    }

    private void mockGetPartners(Set<Long> sortingCenters, Set<Long> deliveryServices) {
        when(lmsClient.searchPartners(createPartnerFilter(Sets.union(sortingCenters, deliveryServices))))
            .thenReturn(
                Stream.concat(
                    sortingCenters.stream()
                        .map(
                            id -> createPartnerResponseBuilder(id, PartnerType.SORTING_CENTER, id)
                                .name("sc-partner-" + id)
                                .build()
                        ),
                    deliveryServices.stream()
                        .map(
                            id -> createPartnerResponseBuilder(id, PartnerType.DELIVERY, id)
                                .name("ds-partner-" + id)
                                .build()
                        )
                )
                    .collect(Collectors.toList())
            );
    }

    private void verifyGetPartners(Set<Long> partnerIds) {
        verify(lmsClient).searchPartners(createPartnerFilter(partnerIds));
    }

    @Nonnull
    private SearchPartnerFilter createPartnerFilter(Set<Long> partnerIds) {
        return SearchPartnerFilter.builder().setIds(partnerIds).build();
    }

    @Nonnull
    private ResultActions getSenderDeliverySettings() throws Exception {
        return getSenderDeliverySettings(1);
    }

    @Nonnull
    private ResultActions getSenderDeliverySettings(long senderId) throws Exception {
        return mockMvc.perform(get("/admin/senders/" + senderId + "/delivery-settings"));
    }
}
