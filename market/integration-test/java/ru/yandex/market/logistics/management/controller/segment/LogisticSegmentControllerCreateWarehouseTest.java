package ru.yandex.market.logistics.management.controller.segment;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.CreateWarehouseSegmentRequest;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Создание логистического сегмента с типом 'Склад'")
@DatabaseSetup("/data/controller/logisticSegment/before/create_warehouse_segment_prepare_data.xml")
class LogisticSegmentControllerCreateWarehouseTest extends AbstractContextualTest {

    @Autowired
    FeatureProperties featureProperties;

    @Test
    @DisplayName("Не передан идентификатор логистической точки")
    void logisticPointIdIsNull() throws Exception {
        request(createWarehouseSegmentRequest(null))
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.validationErrorMatcher(
                "createWarehouseSegmentRequest",
                "logisticPointId",
                "NotNull",
                "must not be null"
            ));
    }

    @Test
    @DisplayName("Не найдена логистическая точка")
    void logisticPointNotFound() throws Exception {
        request(createWarehouseSegmentRequest(1000L))
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find logistics point with id=1000"));
    }

    @Test
    @DisplayName("Успешное создание логистического сегмента для активной СД с признаком дропоффа")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/pickup_point_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() throws Exception {
        request(createWarehouseSegmentRequest(10L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Успешное создание логистического сегмента для активной СД с признаком дропоффа и временем сортировки")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/pickup_point_segment_handling_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successHandlingTime() throws Exception {
        request(createWarehouseSegmentRequest(30L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName(
        "Успешное создание логистического сегмента для активной СД с признаком дропоффа и временем сортировки " +
            "(регион 10000)"
    )
    @DatabaseSetup("/data/controller/logisticSegment/before/partner_handling_time_region_10000.xml")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/pickup_point_segment_handling_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successHandlingTimeRegion10000() throws Exception {
        request(createWarehouseSegmentRequest(30L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName(
        "Успешное создание логистического сегмента для неактивной СД без указания партнёра возвратного склада"
    )
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/inactive_partner_pickup_point_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successPartnerIsInactive() throws Exception {
        request(createWarehouseSegmentRequest(20L, null, createCargoTypes()))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Не найден партнёр возвратного склада СЦ")
    void returnWarehousePartnerNotFound() throws Exception {
        request(createWarehouseSegmentRequest(10L, 2000L, createCargoTypes()))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Партнер с ID 2000 не найден"));
    }

    @Test
    @DisplayName("Партнёр возвратного склада не является СЦ")
    void returnWarehousePartnerIsNotSortingCenter() throws Exception {
        request(createWarehouseSegmentRequest(10L, 3000L, createCargoTypes()))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Возвратный партнер должен быть типа SORTING_CENTER, а не DELIVERY"));
    }

    @Test
    @DisplayName("Карго-типы не переданы, проставляем карго-типы партнера")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/pickup_point_segment_partner_cargo_types.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCargoTypesIsNull() throws Exception {
        request(createWarehouseSegmentRequest(10L, 1000L, null))
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions request(CreateWarehouseSegmentRequest request) throws Exception {
        return mockMvc.perform(TestUtil.request(
            HttpMethod.POST,
            "/externalApi/logistic-segments/warehouse",
            request
        ));
    }

    @Nonnull
    private CreateWarehouseSegmentRequest createWarehouseSegmentRequest(@Nullable Long logisticPointId) {
        return createWarehouseSegmentRequest(logisticPointId, 1000L, createCargoTypes());
    }

    @Nonnull
    private CreateWarehouseSegmentRequest createWarehouseSegmentRequest(
        @Nullable Long logisticPointId,
        @Nullable Long returnWarehousePartnerId,
        @Nullable Map<ServiceCodeName, Set<Integer>> cargoTypes
    ) {
        return CreateWarehouseSegmentRequest.builder()
            .logisticPointId(logisticPointId)
            .returnWarehousePartnerId(returnWarehousePartnerId)
            .cargoTypes(cargoTypes)
            .build();
    }

    @Nonnull
    private Map<ServiceCodeName, Set<Integer>> createCargoTypes() {
        return Map.of(
            ServiceCodeName.INBOUND, Set.of(301, 302, 303),
            ServiceCodeName.PROCESSING, Set.of(304, 305, 306)
        );
    }

    @Test
    @DisplayName("Успешное создание логистического сегмента для активной СД не dropoff")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/pickup_point_segment_zero_hanging_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successHandlingTimeNotDropoff() throws Exception {
        request(createWarehouseSegmentRequest(40L))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание сегмента для склада магазина GO")
    @ExpectedDatabase(
        value = "/data/controller/logisticSegment/after/go_shop_warehouse_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void goShopWarehouseSegment() throws Exception {
        CreateWarehouseSegmentRequest request = CreateWarehouseSegmentRequest.builder()
            .logisticPointId(50L)
            .build();
        request(request);
    }
}
