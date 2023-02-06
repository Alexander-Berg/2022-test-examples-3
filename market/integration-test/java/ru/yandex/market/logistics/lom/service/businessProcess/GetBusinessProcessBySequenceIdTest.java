package ru.yandex.market.logistics.lom.service.businessProcess;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Получение бизнес-процесса по sequenceId")
@DatabaseSetup("/service/business_process_state/ydb/prepare_get_business_process.xml")
class GetBusinessProcessBySequenceIdTest extends AbstractBusinessProcessStateYdbServiceTest {

    @Test
    @DisplayName("Процесс найден в Postgres")
    void getBySequenceIdFromPostgres() {
        long processId = 1;
        insertProcessToYdb(processId);

        BusinessProcessState expectedProcessState = getExpectedProcessState(
            processId,
            1001L,
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            BusinessProcessStatus.ENQUEUED,
            PayloadFactory.createWaybillSegmentPayload(1, 2, "1001", 1)
        );
        BusinessProcessState actualProcessState = businessProcessStateService.getBySequenceId(1001L);

        assertProcessesAreEqual(actualProcessState, expectedProcessState);
    }

    @Test
    @DisplayName("Процесса нет в Postgres, найден в YDB")
    void getBySequenceIdFromYdb() {
        long processId = 11;
        insertProcessToYdb(processId);

        BusinessProcessState expectedProcessState = getExpectedYdbProcessState(processId);
        BusinessProcessState actualProcessState = businessProcessStateService.getBySequenceId(
            SEQUENCE_ID_FUNC.apply(processId)
        );

        assertProcessesAreEqual(actualProcessState, expectedProcessState);
    }

    @Test
    @DisplayName("Процесс не найден ни в Postgres, ни в YDB")
    void notFound() {
        softly.assertThatCode(
                () -> businessProcessStateService.getBySequenceId(123456789L)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [BUSINESS_PROCESS] with id [123456789]");
    }
}
