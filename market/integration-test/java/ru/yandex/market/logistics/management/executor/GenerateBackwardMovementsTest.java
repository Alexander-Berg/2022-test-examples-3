package ru.yandex.market.logistics.management.executor;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.entity.InternalLogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.facade.LogisticSegmentFacade;
import ru.yandex.market.logistics.management.service.client.LogisticsPointService;
import ru.yandex.market.logistics.management.service.graph.LogisticEdgeService;
import ru.yandex.market.logistics.management.service.graph.LogisticSegmentEntityService;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Генерация BACKWARD_MOVEMENT сегментов для существующих MOVEMENT сегментов")
@DatabaseSetup("/data/executor/generate-backward-movements/before/bmv_generate_setup.xml")
class GenerateBackwardMovementsTest extends AbstractContextualAspectValidationTest {
    @Autowired
    private LogisticSegmentFacade logisticSegmentFacade;

    @Autowired
    private LogisticSegmentEntityService logisticSegmentEntityService;

    @Autowired
    private LogisticEdgeService logisticEdgeService;

    @Autowired
    private LogisticsPointService logisticsPointService;

    @Autowired
    private TestableClock clock;

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(logisticEdgeService, logisticSegmentEntityService, logisticsPointService);
    }

    @Test
    @DisplayName("Нет активных перемещещний")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_no_active_movements.xml",
        type = DatabaseOperation.INSERT
    )
    void noActiveMovements() {
        logisticSegmentFacade.generateBackwardMovementSegments();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
    }

    @Test
    @DisplayName("Для дропоффа указана настройка RETURN_SORTING_CENTER_ID / DO-MV-SC")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_is_dropoff_has_return_sc_id.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/generate-backward-movements/after/bmv_generate_is_dropoff_has_return_sc_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void fromDropoffHasReturnScId() {
        logisticSegmentFacade.generateBackwardMovementSegments();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
        verify(logisticSegmentEntityService).getByIdOrThrow(101L);
        verify(logisticSegmentEntityService).findPreviousWarehouses(101L);
        verify(logisticSegmentEntityService).findNextWarehouses(101L);
        verify(logisticsPointService).logisticsPointIsActiveDropoff(any());
        verify(logisticSegmentEntityService).getNextLogisticSegmentIds(104L);
        verify(logisticSegmentEntityService).getPreviousLogisticSegmentIds(102L);
        verify(logisticEdgeService, times(2)).createOrUpdate(any(), any());
    }

    @Test
    @DisplayName("Для дропоффа указана настройка RETURN_SORTING_CENTER_ID / WH-MV-DO")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_to_dropoff_has_return_sc_id.xml",
        type = DatabaseOperation.INSERT
    )
    void toDropoffHasReturnScId() {
        logisticSegmentFacade.generateBackwardMovementSegments();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
        verify(logisticSegmentEntityService).getByIdOrThrow(1L);
        verify(logisticSegmentEntityService).findPreviousWarehouses(1L);
        verify(logisticSegmentEntityService).findNextWarehouses(1L);
        verify(logisticsPointService).logisticsPointIsActiveDropoff(any());
    }

    @Test
    @DisplayName("Успешно создаются BACKWARD_MOVEMENT сегменты")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_success.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/generate-backward-movements/after/bmv_generate_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        logisticSegmentFacade.generateBackwardMovementSegments();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
        verify(logisticSegmentEntityService).getByIdOrThrow(101L);
        verify(logisticSegmentEntityService).findPreviousWarehouses(101L);
        verify(logisticSegmentEntityService).findNextWarehouses(101L);
        verify(logisticsPointService).logisticsPointIsActiveDropoff(any());
        verify(logisticSegmentEntityService, times(2)).getNextLogisticSegmentIds(104L);
        verify(logisticSegmentEntityService, times(2)).getNextLogisticSegmentIds(105L);
        verify(logisticSegmentEntityService, times(2)).getPreviousLogisticSegmentIds(102L);
        verify(logisticSegmentEntityService, times(2)).getPreviousLogisticSegmentIds(103L);
        verify(logisticEdgeService, times(16)).createOrUpdate(any(), any());
    }

    @Test
    @DisplayName("BACKWARD_MOVEMENT сегмент уже существует")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_success_already_exist.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_success_already_exist.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successAlreadyExist() {
        clock.setFixed(Instant.parse("2022-04-19T13:00:00Z"), ZoneOffset.UTC);

        logisticSegmentFacade.generateBackwardMovementSegments();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
    }

    @Test
    @DisplayName("BACKWARD_MOVEMENT сегмент уже существует, маппинг пустой, должна обновиться привязка к связке")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_success_already_exist.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_empty_mapping.xml",
        type = DatabaseOperation.DELETE_ALL
    )
    @ExpectedDatabase(
        value = "/data/executor/generate-backward-movements/after/bmv_generate_success_already_exist.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successAlreadyExistMappingIsEmpty() {
        logisticSegmentFacade.generateBackwardMovementSegments();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
        verify(logisticSegmentEntityService).getByIdOrThrow(101L);
        verify(logisticSegmentEntityService).findPreviousWarehouses(101L);
        verify(logisticSegmentEntityService).findNextWarehouses(101L);
        verify(logisticsPointService).logisticsPointIsActiveDropoff(any());
        verify(logisticSegmentEntityService).getNextLogisticSegmentIds(104L);
        verify(logisticSegmentEntityService).getPreviousLogisticSegmentIds(102L);
        verify(logisticSegmentEntityService).getPage(
            Pageable.unpaged(),
            InternalLogisticSegmentFilter.builder()
                .ids(Set.of(1L))
                .partnerIds(Set.of(203L))
                .types(Set.of(LogisticSegmentType.BACKWARD_MOVEMENT))
                .build()
        );
        verify(logisticEdgeService, times(4)).createOrUpdate(any(), any());
    }

    @Test
    @DisplayName("BACKWARD_MOVEMENT сегмент уже существует, но устарел")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_success_already_exist.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/generate-backward-movements/after/bmv_generate_success_already_exist.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successAlreadyExistButExpired() {
        Date lastUpdatedDate = Date.from(Instant.parse("2022-04-19T12:30:00Z"));
        clock.setFixed(Instant.parse("2022-04-19T13:31:00Z"), ZoneOffset.UTC);
        assertMappingWithUpdated(1, lastUpdatedDate);
        logisticSegmentFacade.generateBackwardMovementSegments();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
        verify(logisticSegmentEntityService).getByIdOrThrow(101L);
        verify(logisticSegmentEntityService).findPreviousWarehouses(101L);
        verify(logisticSegmentEntityService).findNextWarehouses(101L);
        verify(logisticSegmentEntityService).getNextLogisticSegmentIds(104L);
        verify(logisticSegmentEntityService).getPreviousLogisticSegmentIds(102L);
        verify(logisticSegmentEntityService).getPage(any(Pageable.class), any(InternalLogisticSegmentFilter.class));

        verify(logisticsPointService).logisticsPointIsActiveDropoff(any());
        verify(logisticEdgeService, times(4)).createOrUpdate(any(), any());
        assertMappingWithUpdated(0, lastUpdatedDate);
    }

    @Test
    @DisplayName("BACKWARD_MOVEMENT сегмент уже существует, но устарел, к прямому MOVEMENT оторвали еджи")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_success_already_exist.xml",
        type = DatabaseOperation.INSERT
    )
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/deleted_edges.xml",
        type = DatabaseOperation.DELETE
    )
    void successAlreadyExistButExpiredAndMovementEdgesDeleted() {
        Date lastUpdatedDate = Date.from(Instant.parse("2022-04-19T12:30:00Z"));
        clock.setFixed(Instant.parse("2022-04-19T13:31:00Z"), ZoneOffset.UTC);
        assertMappingWithUpdated(1, lastUpdatedDate);
        softly.assertThatCode(() -> logisticSegmentFacade.generateBackwardMovementSegments())
            .doesNotThrowAnyException();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
        verify(logisticSegmentEntityService).getByIdOrThrow(101L);
        verify(logisticSegmentEntityService).findPreviousWarehouses(101L);
        verify(logisticSegmentEntityService).findNextWarehouses(101L);
    }

    @Test
    @DisplayName("BACKWARD_MOVEMENT сегмент уже существует, но устарел. Не дропофф")
    @DatabaseSetup(
        value = "/data/executor/generate-backward-movements/before/bmv_generate_success_already_exist.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/executor/generate-backward-movements/" +
            "after/bmv_generate_success_already_exist_and_is_not_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successAlreadyExistButExpiredAndIsNotDropoff() {
        doReturn(false).when(logisticsPointService).logisticsPointIsActiveDropoff(any());
        Date lastUpdatedDate = Date.from(Instant.parse("2022-04-19T12:30:00Z"));
        clock.setFixed(Instant.parse("2022-04-19T13:31:00Z"), ZoneOffset.UTC);
        assertMappingWithUpdated(1, lastUpdatedDate);
        logisticSegmentFacade.generateBackwardMovementSegments();

        verify(logisticSegmentEntityService).findActiveMovementIdsBetweenWarehouses();
        verify(logisticSegmentEntityService).getByIdOrThrow(101L);
        verify(logisticSegmentEntityService).findPreviousWarehouses(101L);
        verify(logisticSegmentEntityService).findNextWarehouses(101L);
        verify(logisticSegmentEntityService).getNextLogisticSegmentIds(104L);
        verify(logisticSegmentEntityService).getPreviousLogisticSegmentIds(102L);
        verify(logisticSegmentEntityService).getPage(any(Pageable.class), any(InternalLogisticSegmentFilter.class));

        verify(logisticsPointService).logisticsPointIsActiveDropoff(any());
        verify(logisticEdgeService, times(2)).createOrUpdate(any(), any());
        assertMappingWithUpdated(0, lastUpdatedDate);
    }

    private void assertMappingWithUpdated(int expectedCount, Date... date) {
        softly.assertThat(
                jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM movement_backward_movement_mapping WHERE updated = ?",
                    date,
                    Integer.class
                )
            )
            .as(String.format("Amount of movement mapping with updated '%s' should be %d", date[0], expectedCount))
            .isEqualTo(expectedCount);
    }
}
