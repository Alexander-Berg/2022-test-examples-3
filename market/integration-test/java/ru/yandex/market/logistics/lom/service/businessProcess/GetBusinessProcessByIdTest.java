package ru.yandex.market.logistics.lom.service.businessProcess;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@DisplayName("Получение бизнес-процесса по идентификатору")
@DatabaseSetup("/service/business_process_state/ydb/prepare_get_business_process.xml")
class GetBusinessProcessByIdTest extends AbstractBusinessProcessStateYdbServiceTest {

    @Test
    @DisplayName("Процесс найден в postgres")
    void getByIdFromPostgres() {
        long processId = 1;
        insertProcessToYdb(processId);

        BusinessProcessState expectedProcessState = getExpectedProcessState(
            1L,
            1001L,
            QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
            BusinessProcessStatus.ENQUEUED,
            PayloadFactory.createWaybillSegmentPayload(1, 2, "1001", 1)
        );
        BusinessProcessState actualProcessState = businessProcessStateService.getBusinessProcessState(processId);

        assertProcessesAreEqual(actualProcessState, expectedProcessState);
    }

    @Test
    @DisplayName("Процесса нет в postgres, найден в YDB")
    void getByIdFromYdb() {
        long processId = 11;
        insertProcessToYdb(processId);

        BusinessProcessState expectedProcessState = getExpectedYdbProcessState(processId);
        BusinessProcessState actualProcessState = businessProcessStateService.getBusinessProcessState(processId);

        assertProcessesAreEqual(actualProcessState, expectedProcessState);
    }

    @Test
    @DisplayName("Процесс не найден ни в postgres, ни в YDB")
    void processNotFound() {
        softly.assertThat(businessProcessStateService.findBusinessProcessState(123456789L))
            .isEmpty();

        softly.assertThatCode(
                () -> businessProcessStateService.getBusinessProcessState(123456789L)
            )
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [BUSINESS_PROCESS] with id [123456789]");
    }
}
