package ru.yandex.market.logistics.nesu.service.dropoff;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentSequenceFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;

import static org.mockito.Mockito.verify;

@DisplayName("Тест на выполнение подзадачи отключения дропоффа. Подготовить данные для коммуникации с партнерами.")
class PrepareDataForCommunicationExecutorTest extends AbstractDisablingSubtaskTest {

    @Test
    @DatabaseSetup(
        value = "/service/dropoff/before/prepare_data_for_communication_subtask.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/dropoff/after/prepare_data_for_communication_subtask.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешное выполнение подзадачи: подготовка данных для коммуникации с партнерами")
    void successExecution() {
        processWithAddress("test_sc", "test_dropoff");
    }

    @Test
    @DatabaseSetup("/service/dropoff/before/prepare_data_for_communication_subtask.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/error_prepare_data_subtask_no_edges.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Выполнение с ошибкой. Дропофф не связан ни с одним СЦ.")
    void noEdgeFromDropoffToSc() {
        mockDropships();
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        verify(lmsClient).searchLogisticSegmentsSequence(expectedDropshipDropoffSegmentFilter());
        verify(lmsClient).searchLogisticSegmentsSequence(expectedDropoffScSegmentFilter());
    }

    @Test
    @DatabaseSetup(
        value = "/service/dropoff/before/prepare_data_for_communication_subtask.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/service/dropoff/after/prepare_data_for_communication_subtask_no_affected_shops.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Нет затронутых магазинов.")
    void noAffectedShops() {
        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));
        verify(lmsClient).searchLogisticSegmentsSequence(expectedDropshipDropoffSegmentFilter());
    }

    @Test
    @DatabaseSetup("/service/dropoff/before/prepare_data_for_communication_subtask.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/error_prepare_data_subtask_no_sc_address.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Выполнение с ошибкой. Не сущуствует СЦ с такой лог. точкой.")
    void noSuchSc() {
        processWithAddress(null, null);
    }

    @Test
    @DatabaseSetup("/service/dropoff/before/prepare_data_for_communication_subtask.xml")
    @ExpectedDatabase(
        value = "/service/dropoff/after/error_prepare_data_subtask_no_dropoff_address.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Выполнение с ошибкой. Не сущуствует адреса у дропоффа.")
    void noSuchDropoffAddress() {
        processWithAddress("test_sc", null);
    }

    private void processWithAddress(@Nullable String scAddress, @Nullable String dropoffAddress) {
        mockDropships();
        mockSc();
        mockScDropoffAddress(scAddress, dropoffAddress);

        disableDropoffSubtaskProcessor.processPayload(getDisableDropoffPayload(100, 1));

        verify(lmsClient).searchLogisticSegmentsSequence(expectedDropshipDropoffSegmentFilter());
        verify(lmsClient).searchLogisticSegmentsSequence(expectedDropoffScSegmentFilter());
        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder()
            .ids(Set.of(SC_LOGISTIC_POINT_ID_54, DROPOFF_LOGISTIC_POINT_ID_321))
            .build()
        );
    }

    @Nonnull
    private LogisticSegmentSequenceFilter expectedDropshipDropoffSegmentFilter() {
        return LogisticSegmentSequenceFilter.builder()
            .segmentSequence(
                List.of(
                    logisticSegmentSequenceFilterFactory.createBaseLogisticSegmentFilter(LogisticSegmentType.WAREHOUSE),
                    logisticSegmentSequenceFilterFactory.createBaseLogisticSegmentFilter(
                        LogisticSegmentType.MOVEMENT,
                        ActivityStatus.ACTIVE
                    ),
                    logisticSegmentSequenceFilterFactory.createBaseLogisticSegmentFilter(
                        Set.of(AbstractDisablingSubtaskTest.DROPOFF_LOGISTIC_POINT_ID_321),
                        LogisticSegmentType.WAREHOUSE
                    )
                )
            )
            .build();
    }

    @Nonnull
    private LogisticSegmentSequenceFilter expectedDropoffScSegmentFilter() {
        return LogisticSegmentSequenceFilter.builder()
            .segmentSequence(
                List.of(
                    logisticSegmentSequenceFilterFactory.createBaseLogisticSegmentFilter(
                        Set.of(DROPOFF_LOGISTIC_POINT_ID_321),
                        LogisticSegmentType.WAREHOUSE,
                        ActivityStatus.ACTIVE
                    ),
                    logisticSegmentSequenceFilterFactory.createBaseLogisticSegmentFilter(
                        LogisticSegmentType.MOVEMENT,
                        ActivityStatus.ACTIVE
                    ),
                    logisticSegmentSequenceFilterFactory.createBaseLogisticSegmentFilter(
                        LogisticSegmentType.WAREHOUSE,
                        ActivityStatus.ACTIVE
                    )
                )
            )
            .build();
    }
}
