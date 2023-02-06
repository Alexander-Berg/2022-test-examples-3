package ru.yandex.market.logistics.nesu.controller.partner;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.datacamp.client.DataCampClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.model.LmsFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractGetAvailableShipmentOptionsTest extends AbstractContextualTest {
    protected static final long SHOP_PARTNER_ID = 1000L;
    protected static final long SHOP_BUSINESS_ID = 42;
    protected static final long TO_PARTNER_ID = 2000L;
    protected static final List<Integer> EXPRESS_AVAILABLE_HANDLING_TIME_MINUTES = List.of(10, 20, 30, 40, 50, 60);

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected DataCampClient dataCampClient;

    @Autowired
    protected FeatureProperties featureProperties;

    @AfterEach
    void tearDownAbstractGetAvailableShipmentOptionsTest() {
        verifyNoMoreInteractions(lmsClient);
        verifyNoMoreInteractions(dataCampClient);
        featureProperties.setExpressDefaultHandlingTimeMinutes(30);
        featureProperties.setExpressAvailableHandlingTimesMinutes(EXPRESS_AVAILABLE_HANDLING_TIME_MINUTES);
    }

    @Test
    @DisplayName("Отсутствует идентификатор магазина")
    void noShopId() throws Exception {
        mockMvc.perform(
                createRequestBuilder()
                    .param("partnerType", "DROPSHIP")
                    .param("warehouseId", "1")
                    .param("userId", "2")
            )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "shopId",
                "Failed to convert value of type 'null' to required type 'long'",
                "shopIdHolder",
                "typeMismatch"
            )));
    }

    @Test
    @DisplayName("Отсутствует идентификатор пользователя")
    void noUserId() throws Exception {
        mockMvc.perform(
                createRequestBuilder()
                    .param("partnerType", "DROPSHIP")
                    .param("warehouseId", "1")
                    .param("shopId", "2")
            )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "userId",
                "Failed to convert value of type 'null' to required type 'long'",
                "shopIdHolder",
                "typeMismatch"
            )));
    }

    @Test
    @DisplayName("Несуществующий магазин")
    void nonExistentShop() throws Exception {
        getAvailableShipmentOptions(100500, 1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [100500]"));
    }

    @Test
    @DisplayName("Отключенный магазин")
    void disabledShop() throws Exception {
        getAvailableShipmentOptions(5, 1)
            .andExpect(status().isForbidden())
            .andExpect(noContent());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ValueSource(longs = {2L, 4L})
    @DisplayName("Некорректная роль магазина")
    void wrongShopType(long shopId) throws Exception {
        getAvailableShipmentOptions(shopId, 1)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Shop must have role [DROPSHIP, SUPPLIER] for this operation"));
    }

    @Test
    @DisplayName("Бизнес-склад не существует")
    void nonExistentWarehouse() throws Exception {
        long businessWarehouseId = SHOP_PARTNER_ID;
        getAvailableShipmentOptions(1, businessWarehouseId)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_WAREHOUSE] with ids [1000]"));
        verify(lmsClient).getBusinessWarehouseForPartner(eq(businessWarehouseId));
        verifyDataCampClient(SHOP_BUSINESS_ID, 1, 1);
    }

    @Test
    @DisplayName("Бизнес-склад не принадлежит магазину")
    void warehouseNotAvailable() throws Exception {
        long businessWarehouseId = 5;
        when(lmsClient.getBusinessWarehouseForPartner(businessWarehouseId))
            .thenReturn(Optional.of(businessWarehouse(businessWarehouseId).build()));
        getAvailableShipmentOptions(1, businessWarehouseId)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [BUSINESS_WAREHOUSE] with ids [5]"));
    }

    @Nonnull
    protected abstract ResultActions getAvailableShipmentOptions(long shopId, long warehouseId) throws Exception;

    protected abstract MockHttpServletRequestBuilder createRequestBuilder();

    @Nonnull
    protected AutoCloseable mockGetWarehouses(Set<Long> warehouseIds, int locationId) {
        when(lmsClient.getLogisticsPoints(warehousesFilter(warehouseIds)))
            .thenReturn(
                warehouseIds.stream()
                    .map(id -> warehouse(id, locationId, TO_PARTNER_ID).build())
                    .collect(Collectors.toList())
            );
        return () -> verifyGetWarehouses(warehouseIds);
    }

    protected void verifyGetWarehouses(Set<Long> warehouseIds) {
        verify(lmsClient).getLogisticsPoints(warehousesFilter(warehouseIds));
    }

    @Nonnull
    protected AutoCloseable mockPartnerTo(long toPartnerId) {
        SearchPartnerFilter filter = SearchPartnerFilter.builder().setIds(Set.of(toPartnerId)).build();
        when(lmsClient.searchPartners(filter))
            .thenReturn(List.of(LmsFactory.createPartner(toPartnerId, PartnerType.DELIVERY)));
        return () -> verify(lmsClient).searchPartners(filter);
    }

    @Nonnull
    protected AutoCloseable mockPartnerTo() {
        SearchPartnerFilter filter = SearchPartnerFilter.builder().setIds(Set.of(TO_PARTNER_ID)).build();
        when(lmsClient.searchPartners(filter))
            .thenReturn(List.of(LmsFactory.createPartner(TO_PARTNER_ID, PartnerType.DELIVERY)));
        return () -> verify(lmsClient).searchPartners(filter);
    }

    @Nonnull
    protected LogisticsPointResponse.LogisticsPointResponseBuilder warehouse(
        long warehouseId,
        int locationId,
        Long partnerId
    ) {
        return LmsFactory.createLogisticsPointResponseBuilder(
                warehouseId,
                partnerId,
                "Warehouse " + warehouseId,
                PointType.WAREHOUSE
            )
            .businessId(SHOP_BUSINESS_ID)
            .address(LmsFactory.createAddressDto(locationId));
    }

    @Nonnull
    protected AutoCloseable mockGetActiveRelation(Long toPartnerWarehouseId) {
        when(lmsClient.searchPartnerRelation(relationFilter()))
            .thenReturn(List.of(
                PartnerRelationEntityDto.newBuilder()
                    .enabled(true)
                    .toPartnerId(TO_PARTNER_ID)
                    .fromPartnerId(SHOP_PARTNER_ID)
                    .toPartnerLogisticsPointId(toPartnerWarehouseId)
                    .shipmentType(ShipmentType.WITHDRAW)
                    .build(),
                PartnerRelationEntityDto.newBuilder()
                    .build()
            ));
        return this::verifyGetActiveRelation;
    }

    protected void verifyGetActiveRelation() {
        verify(lmsClient).searchPartnerRelation(relationFilter());
    }

    @Nonnull
    private PartnerRelationFilter relationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(SHOP_PARTNER_ID)
            .build();
    }

    protected void verifyGetPartners(Set<Long> partnerIds) {
        verify(lmsClient).searchPartners(
            SearchPartnerFilter.builder().setIds(partnerIds).build()
        );
    }

    protected void verifyDataCampClient(long businessId, long shopId, int times) {
        verify(dataCampClient, times(times)).getCargoTypes(businessId, shopId);
    }

    @Nonnull
    protected AutoCloseable mockGetPartnerWarehouse(long warehouseId) {
        when(lmsClient.getLogisticsPoints(partnerWarehouseFilter()))
            .thenReturn(List.of(warehouse(warehouseId, 225, TO_PARTNER_ID).build()));

        return () -> verify(lmsClient).getLogisticsPoints(partnerWarehouseFilter());
    }

    @Nonnull
    protected AutoCloseable mockGetBusinessWarehouse(long businessWarehouseId, int locationId) {
        when(lmsClient.getBusinessWarehouseForPartner(businessWarehouseId)).thenReturn(Optional.of(
            businessWarehouse(businessWarehouseId).address(LmsFactory.createAddressDto(locationId)).build()
        ));
        return () -> verify(lmsClient).getBusinessWarehouseForPartner(eq(businessWarehouseId));
    }

    @Nonnull
    protected PartnerResponse shipmentPartner(long id, boolean isDropoff) {
        return LmsFactory.createPartnerResponseBuilder(id, PartnerType.DELIVERY, 1L)
            .params(List.of(new PartnerExternalParam(
                PartnerExternalParamType.IS_DROPOFF.name(),
                null,
                String.valueOf(isDropoff)
            )))
            .build();
    }

    @Nonnull
    protected AutoCloseable mockGetPartnersExpress(
        long expressPartnerId,
        long dropshipId,
        List<PartnerExternalParam> params
    ) {
        when(lmsClient.searchPartners(
            SearchPartnerFilter.builder().setIds(Set.of(TO_PARTNER_ID, SHOP_PARTNER_ID)).build()
        )).thenReturn(
            List.of(
                PartnerResponse.newBuilder()
                    .id(expressPartnerId)
                    .marketId(1L)
                    .partnerType(PartnerType.DELIVERY)
                    .subtype(PartnerSubtypeResponse.newBuilder().id(34L).build())
                    .status(PartnerStatus.ACTIVE)
                    .build(),
                PartnerResponse.newBuilder()
                    .id(dropshipId)
                    .marketId(1L)
                    .partnerType(PartnerType.DROPSHIP)
                    .params(params)
                    .status(PartnerStatus.ACTIVE)
                    .build()
            )
        );

        return () -> verify(lmsClient).searchPartners(
            SearchPartnerFilter.builder().setIds(Set.of(TO_PARTNER_ID, SHOP_PARTNER_ID)).build()
        );
    }

    @Nonnull
    protected LogisticsPointFilter warehousesFilter(Set<Long> warehouseIds) {
        return LogisticsPointFilter.newBuilder()
            .ids(warehouseIds)
            .active(true)
            .build();
    }

    @Nonnull
    private LogisticsPointFilter partnerWarehouseFilter() {
        return LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .partnerIds(Set.of(TO_PARTNER_ID))
            .active(true)
            .build();
    }

    @Nonnull
    protected BusinessWarehouseResponse.Builder businessWarehouse(long partnerId) {
        return BusinessWarehouseResponse.newBuilder()
            .partnerId(partnerId)
            .logisticsPointId(partnerId + 10_000_000_000L)
            .marketId(200L)
            .businessId(42L);
    }
}
