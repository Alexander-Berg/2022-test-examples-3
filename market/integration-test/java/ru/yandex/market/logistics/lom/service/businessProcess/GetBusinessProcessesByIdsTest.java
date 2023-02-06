package ru.yandex.market.logistics.lom.service.businessProcess;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.enums.BusinessProcessStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;

@ParametersAreNonnullByDefault
@DisplayName("Получение бизнес-процессов по списку идентификаторов")
@DatabaseSetup("/service/business_process_state/ydb/prepare_get_business_process.xml")
class GetBusinessProcessesByIdsTest extends AbstractBusinessProcessStateYdbServiceTest {

    @Test
    @DisplayName("Все процессы из postgres")
    void allProcessesFromPostgres() {
        getByIdsAndAssertResult(
            Set.of(1L, 2L),
            List.of(
                getExpectedProcessState(
                    1L,
                    1001L,
                    QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
                    BusinessProcessStatus.ENQUEUED,
                    PayloadFactory.createWaybillSegmentPayload(1, 2, "1001", 1)
                ),
                getExpectedProcessState(
                    2L,
                    1002L,
                    QueueType.CREATE_ORDER_EXTERNAL,
                    BusinessProcessStatus.QUEUE_TASK_ERROR,
                    PayloadFactory.createWaybillSegmentPayload(2, 3, "1002", 1)
                )
            )
        );
    }

    @Test
    @DisplayName("Часть процессов из postgres, часть из YDB. Данные Postgres приоритетнее")
    void fromPostgresAndFromYdb() {
        insertProcessesToYdb(1L, 3L);
        getByIdsAndAssertResult(
            Set.of(1L, 3L),
            List.of(
                //postgres
                getExpectedProcessState(
                    1L,
                    1001L,
                    QueueType.FULFILLMENT_CREATE_ORDER_EXTERNAL,
                    BusinessProcessStatus.ENQUEUED,
                    PayloadFactory.createWaybillSegmentPayload(1, 2, "1001", 1)
                ),
                //YDB
                getExpectedYdbProcessState(3L)
            )
        );
    }

    @Test
    @DisplayName("Все процессы из YDB")
    void allFromYdb() {
        insertProcessesToYdb(3L, 4L, 10L);
        getByIdsAndAssertResult(
            Set.of(3L, 4L, 10L),
            List.of(
                getExpectedYdbProcessState(3L),
                getExpectedYdbProcessState(4L),
                getExpectedYdbProcessState(10L)
            )
        );
    }

    @Test
    @DisplayName("Нет ни в Postgres, ни в YDB")
    void notFound() {
        getByIdsAndAssertResult(Set.of(123L, 1234L, 12345L, 1234567L, 12345678L), List.of());
    }

    private void getByIdsAndAssertResult(Set<Long> ids, List<BusinessProcessState> expected) {
        assertProcessesAreEqual(
            businessProcessStateService.findBusinessProcessStates(ids),
            expected
        );
    }
}
