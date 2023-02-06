package ru.yandex.market.logistics.lom.service.businessProcess;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateStatusHistoryYdb;
import ru.yandex.market.logistics.lom.entity.ydb.BusinessProcessStateYdb;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPartnerIdPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@ParametersAreNonnullByDefault
@DisplayName("Сохранение состояний бизнес-процессов в YDB")
@DatabaseSetup("/service/business_process_state/ydb/prepare.xml")
class SaveBusinessProcessesTest extends AbstractBusinessProcessStateYdbServiceTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Создание нового состояния бизнес-процесса в YDB")
    @MethodSource
    void create(
        @SuppressWarnings("unused") String caseName,
        Long businessProcessStateId,
        UnaryOperator<BusinessProcessStateYdb> businessProcessStateYdbUpdater
    ) {
        BusinessProcessState businessProcessState = businessProcessStateService.getBusinessProcessState(
            businessProcessStateId
        );

        OrderIdPartnerIdPayload payload = PayloadFactory.createOrderIdPartnerIdPayload(1L, 2L, 1L);
        payload.setSequenceId(businessProcessState.getSequenceId());

        BusinessProcessStateYdb businessProcessStateYdb = businessProcessStateYdbUpdater.apply(
            getBusinessProcessStateYdb(payload, businessProcessStateId)
        );
        businessProcessStateService.saveToYdb(businessProcessState);
        assertYdbContainsBusinessProcessWithEntities(businessProcessStateYdb);
    }

    @DisplayName("Сохранение списка бизнес-процессов в YDB")
    @Test
    void saveList() {
        OrderIdPartnerIdPayload firstPayload = PayloadFactory.createOrderIdPartnerIdPayload(1L, 2L, 1L);
        firstPayload.setSequenceId(1001L);

        OrderIdPartnerIdPayload secondPayload = PayloadFactory.createOrderIdPartnerIdPayload(2L, 3L, 1L);
        secondPayload.setSequenceId(1002L);

        List<BusinessProcessState> businessProcesses = businessProcessStateService.findBusinessProcessStates(
            Set.of(1L, 2L)
        );

        List<BusinessProcessStateYdb> businessProcessesYdb = List.of(
            getBusinessProcessStateYdb(firstPayload, 1L),
            getBusinessProcessStateYdb(secondPayload, 2L)
        );

        businessProcessStateService.saveToYdb(businessProcesses);
        assertYdbContainsBusinessProcessWithEntities(businessProcessesYdb);
    }

    @Nonnull
    private static Stream<Arguments> create() {
        return Stream.<Triple<String, Long, UnaryOperator<BusinessProcessStateYdb>>>of(
                Triple.of(
                    "Бизнес-процесс со всеми заполненными полями",
                    1L,
                    UnaryOperator.identity()
                ),
                Triple.of(
                    "Бизнес-процесс без родительского бизнес-процесса",
                    3L,
                    businessProcessStateYdb -> businessProcessStateYdb.setParentId(null)
                ),
                Triple.of(
                    "Бизнес-процесс без связанных сущностей",
                    4L,
                    businessProcessStateYdb -> businessProcessStateYdb.setEntityIds(List.of())
                )
            )
            .map(t -> Arguments.of(t.getLeft(), t.getMiddle(), t.getRight()));
    }

    @DisplayName("В YDB сразу сохраняются только процессы в успешном терминальном статусе")
    @EnumSource(BusinessProcessStatus.class)
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void processSavedToYdbOnlyInTerminalSuccessStatus(BusinessProcessStatus status) {
        saveProcessAndCheckYdbSaving(
            businessProcess(status),
            BusinessProcessStatus.TERMINAL_SUCCESS_STATUSES.contains(status)
        );
    }

    private void saveProcessAndCheckYdbSaving(
        BusinessProcessState businessProcess,
        boolean savedToYdb
    ) {
        Long processId = businessProcessStateService.save(businessProcess).getId();
        BusinessProcessState savedProcess = businessProcessStateService.getBusinessProcessState(processId);

        assertProcessStatusHistory(statusHistoryYdb(savedProcess.getStatus()));
        if (!savedToYdb) {
            assertYdbNotContainsProcesses();
            softly.assertThat(savedProcess.getSaveToYtOnly()).isNull();
            return;
        }

        assertYdbContainsBusinessProcessWithEntities(
            businessProcessStateYdb(savedProcess.getStatus())
        );
        softly.assertThat(savedProcess.getSaveToYtOnly()).isTrue();
    }

    @Nonnull
    private BusinessProcessState businessProcess(BusinessProcessStatus status) {
        return new BusinessProcessState()
            .setId(1111111L)
            .setStatus(status)
            .setQueueType(QueueType.COMMIT_ORDER)
            .setSequenceId(1111111L)
            .setSaveToYtOnly(null);
    }

    @Nonnull
    private BusinessProcessStateYdb businessProcessStateYdb(BusinessProcessStatus status) {
        return new BusinessProcessStateYdb()
            .setId(1111111L)
            .setSequenceId(1111111L)
            .setCreated(clock.instant())
            .setUpdated(clock.instant())
            .setStatus(status)
            .setQueueType(QueueType.COMMIT_ORDER)
            .setPayload("");
    }

    @Nonnull
    private BusinessProcessStateStatusHistoryYdb statusHistoryYdb(BusinessProcessStatus status) {
        return new BusinessProcessStateStatusHistoryYdb()
            .setId(1111111L)
            .setSequenceId(1111111L)
            .setCreated(clock.instant())
            .setStatus(status);
    }
}
