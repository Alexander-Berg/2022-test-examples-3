package ru.yandex.market.logistics.nesu.controller.settings;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Получение доступных вариантов подключения в регионе")
@DatabaseSetup("/repository/settings/sender_regions_delivery_settings.xml")
class SettingsControllerGetSenderRegionDeliverySettingsTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @DisplayName(
        "Несколько вариантов с разными видами отгрузок (забор/самопривоз, через СЦ/напрямую, собственная доставка)"
    )
    @Test
    void getSenderRegionAvailableDeliveries() throws Exception {
        Set<Long> partnerIds = Set.of(1L, 2L, 3L, 4L, 6L);

        mockGetActiveWarehouses();
        mockSearchValidPartners(
            partnerIds,
            List.of(
                getPartnerResponse(1L, PartnerType.DELIVERY),
                getPartnerResponse(2L, PartnerType.SORTING_CENTER),
                getPartnerResponse(3L, PartnerType.DELIVERY),
                getPartnerResponse(6L, PartnerType.DELIVERY)
            )
        );
        mockSearchOwnDeliveryPartners();
        mockSearchPartnerRelations();

        getSenderRegionAvailableDeliveries(1, 20L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/settings/sender/region_delivery_available_result.json"));

        verifyGetActiveWarehouses();
        verifySearchPartnerRelations();
        verifySearchActivePartners(partnerIds);
        verifySearchOwnDeliveryPartners();
    }

    @DisplayName("Переданный магазин имеет тип не DAAS")
    @Test
    void getSenderRegionAvailableDeliveriesNotDaas() throws Exception {
        getSenderRegionAvailableDeliveries(2L, 1, 21L)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Получить доступные варианты подключения в регионе можно только для DAAS-магазинов."
            ));
    }

    @DisplayName("Партнёр СЦ недоступен для Яндекс доставки")
    @Test
    void getSenderRegionAvailableDeliveriesSortingCenterIsInactive() throws Exception {
        Set<Long> partnerIds = Set.of(1L, 2L, 3L, 4L, 6L);

        mockGetActiveWarehouses();
        mockSearchValidPartners(
            partnerIds,
            List.of(
                getPartnerResponse(1L, PartnerType.DELIVERY),
                getPartnerResponse(3L, PartnerType.DELIVERY),
                getPartnerResponse(6L, PartnerType.DELIVERY)
            )
        );
        mockSearchPartnerRelations();

        getSenderRegionAvailableDeliveries(1, 20L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Партнёр СЦ id=2 недоступен для Яндекс Доставки."));

        verifyGetActiveWarehouses();
        verifySearchPartnerRelations();
        verifySearchActivePartners(partnerIds);
    }

    @DisplayName("Нет конфигурации для указанного склада СЦ в регионе")
    @Test
    void getSenderRegionAvailableDeliveriesNoConfigurationForScWarehouse() throws Exception {
        getSenderRegionAvailableDeliveries(1, 21L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(
                "Не найдена конфигурация доступности склада id=21 доступная для DAAS-магазина id=1 хотя бы в одной "
                    + "из локаций [1, 3, 225, 10001, 10000]"
            ));
    }

    @DisplayName("Конфигурации для склада СЦ в регионе недоступна для указанного магазина")
    @Test
    void getSenderRegionAvailableDeliveriesNoAvailableConfigurationForScWarehouseForShop() throws Exception {
        getSenderRegionAvailableDeliveries(1, 22L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(
                "Не найдена конфигурация доступности склада id=22 доступная для DAAS-магазина id=1 хотя бы в одной "
                    + "из локаций [1, 3, 225, 10001, 10000]"
            ));
    }

    @DisplayName("Ошибка валидации входных параметров")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationErrorSource")
    void validationError(
        @SuppressWarnings("unused") String caseName,
        Integer locationId,
        Long sortingCenterWarehouseId,
        String errorField
    ) throws Exception {
        getSenderRegionAvailableDeliveries(locationId, sortingCenterWarehouseId)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(ValidationErrorData.fieldError(
                errorField,
                "must not be null",
                "deliverySettingsWarehouseFilter",
                "NotNull"
            )));
    }

    @Nonnull
    private static Stream<Arguments> validationErrorSource() {
        return Stream.of(
            Arguments.of(
                "Не указан регион",
                null,
                20L,
                "locationId"
            ),
            Arguments.of(
                "Не указан склад сортировочного центра",
                1,
                null,
                "sortingCenterWarehouseId"
            )
        );
    }

    private void mockGetActiveWarehouses() {
        when(lmsClient.getLogisticsPoints(getLogisticsPointFilter())).thenReturn(
            List.of(
                LmsFactory.createLogisticsPointResponse(10L, 1L, "Склад СД id=1", PointType.WAREHOUSE),
                LmsFactory.createLogisticsPointResponse(20L, 2L, "Склад СЦ id=2", PointType.WAREHOUSE),
                LmsFactory.createLogisticsPointResponse(30L, 3L, "Склад СД id=3", PointType.WAREHOUSE),
                LmsFactory.createLogisticsPointResponse(40L, 4L, "Склад неактивной СД id=4", PointType.WAREHOUSE)
            )
        );
    }

    private void mockSearchPartnerRelations() {
        when(lmsClient.searchPartnerRelation(getPartnerRelationFilter())).thenReturn(
            List.of(
                PartnerRelationEntityDto.newBuilder()
                    .fromPartnerId(2L)
                    .toPartnerId(1L)
                    .build(),
                PartnerRelationEntityDto.newBuilder()
                    .fromPartnerId(2L)
                    .toPartnerId(6L)
                    .build()
            )
        );
    }

    private void mockSearchValidPartners(Set<Long> partnerIds, List<PartnerResponse> partners) {
        when(lmsClient.searchPartners(getSearchPartnerFilter(partnerIds))).thenReturn(partners);
    }

    private PartnerResponse getPartnerResponse(long partnerId, PartnerType partnerType) {
        return LmsFactory.createPartnerResponseBuilder(partnerId, partnerType, partnerId * 100)
            .name(String.format("name_%d", partnerId))
            .readableName(String.format("readable name id=%d", partnerId))
            .build();
    }

    private void mockSearchOwnDeliveryPartners() {
        when(lmsClient.searchPartners(getSearchOwnDeliveryPartnerFilter())).thenReturn(
            List.of(
                LmsFactory.createPartnerResponseBuilder(5L, PartnerType.OWN_DELIVERY, 900L)
                    .name("OWN_DS")
                    .readableName("Собственная доставка")
                    .build()
            )
        );
    }

    @Nonnull
    private ResultActions getSenderRegionAvailableDeliveries(
        @Nullable Integer locationId,
        @Nullable Long sortingCenterWarehouseId
    ) throws Exception {
        return getSenderRegionAvailableDeliveries(1L, locationId, sortingCenterWarehouseId);
    }

    @Nonnull
    private ResultActions getSenderRegionAvailableDeliveries(
        @Nullable Long senderId,
        @Nullable Integer locationId,
        @Nullable Long sortingCenterWarehouseId
    ) throws Exception {
        return mockMvc.perform(
            get("/back-office/settings/sender/delivery/available")
                .param("userId", "123")
                .param("shopId", "1")
                .param("senderId", Objects.toString(senderId, null))
                .param("locationId", Objects.toString(locationId, null))
                .param("sortingCenterWarehouseId", Objects.toString(sortingCenterWarehouseId, null))
        );
    }

    private void verifyGetActiveWarehouses() {
        verify(lmsClient).getLogisticsPoints(getLogisticsPointFilter());
    }

    @Nonnull
    private LogisticsPointFilter getLogisticsPointFilter() {
        return LogisticsPointFilter.newBuilder()
            .active(true)
            .type(PointType.WAREHOUSE)
            .ids(Set.of(10L, 20L, 30L, 40L))
            .build();
    }

    private void verifySearchPartnerRelations() {
        verify(lmsClient).searchPartnerRelation(getPartnerRelationFilter());
    }

    @Nonnull
    private PartnerRelationFilter getPartnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnersIds(Set.of(2L))
            .enabled(true)
            .build();
    }

    private void verifySearchActivePartners(Set<Long> partnerIds) {
        verify(lmsClient).searchPartners(getSearchPartnerFilter(partnerIds));
    }

    @Nonnull
    private SearchPartnerFilter getSearchPartnerFilter(Set<Long> partnerIds) {
        return SearchPartnerFilter.builder()
            .setStatuses(EnumSet.of(PartnerStatus.ACTIVE, PartnerStatus.TESTING))
            .setPlatformClientStatuses(EnumSet.of(PartnerStatus.ACTIVE))
            .setPlatformClientIds(Set.of(PlatformClientId.YANDEX_DELIVERY.getId()))
            .setIds(partnerIds)
            .build();
    }

    private void verifySearchOwnDeliveryPartners() {
        verify(lmsClient).searchPartners(getSearchOwnDeliveryPartnerFilter());
    }

    @Nonnull
    private SearchPartnerFilter getSearchOwnDeliveryPartnerFilter() {
        return TestOwnDeliveryUtils.ownDeliveryFilter()
            .setMarketIds(Set.of(900L))
            .build();
    }
}
