package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.dbqueue.monitoring.NumberOfRetriesInterval;
import ru.yandex.market.ff.model.dbqueue.DbQueueState;
import ru.yandex.market.ff.model.enums.DbQueueType;

public class DbQueueRepositoryTest extends IntegrationTest {

    @Autowired
    private DbQueueRepository dbQueueRepository;

    @Test
    @DatabaseSetup("classpath:repository/db-queue-repository/before.xml")
    @SuppressWarnings("checkstyle:MethodLength")
    void getStateByQueueWorksCorrect() {
        Map<DbQueueType, DbQueueState> stateByQueue = dbQueueRepository.getStateByQueue();
        assertions.assertThat(stateByQueue.size()).isEqualTo(56);

        DbQueueState state = stateByQueue.get(DbQueueType.VALIDATE_SUPPLY_REQUEST);
        assertCorrectQueueState(2, 1, 1, 0, state);

        state = stateByQueue.get(DbQueueType.VALIDATE_COMMON_REQUEST);
        assertCorrectQueueState(1, 0, 0, 1, state);

        state = stateByQueue.get(DbQueueType.UPDATE_REQUEST);
        assertCorrectQueueState(1, 0, 1, 0, state);

        state = stateByQueue.get(DbQueueType.VALIDATE_SHADOW_SUPPLY_REQUEST);
        assertCorrectQueueState(2, 1, 1, 0, state);

        state = stateByQueue.get(DbQueueType.BUILD_ACT_REQUEST);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.PUSH_RIGHT_VERDICT_MEASUREMENT);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.START_RETURN_PROCESSING);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.PUBLISH_REQUEST_STATUS_CHANGE);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.CREATE_UTILIZATION_OUTBOUNDS);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_REQUEST_DETAILS);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_REQUEST_DETAILS_ERROR);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_REQUEST_DETAILS_ON_SUCCESS_ERROR);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_REQUEST_DETAILS_FF_ERROR);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_REQUEST_DETAILS_FF_ON_SUCCESS_ERROR);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_REQUEST_DETAILS_SC_RETURNS_ERROR);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_REQUEST_DETAILS_SC_RETURNS_ON_SUCCESS_ERROR);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.CREATE_TRANSFER_FROM_PREPARED);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_INVENTORYING_REQUEST);
        assertCorrectQueueState(1, 0, 1, 0, state);

        state = stateByQueue.get(DbQueueType.REGISTER_STATUS_TRACKING);
        assertCorrectQueueState(1, 0, 1, 0, state);

        state = stateByQueue.get(DbQueueType.GET_INVENTORYING_PER_SUPPLIER_REQUEST);
        assertCorrectQueueState(1, 0, 1, 0, state);

        state = stateByQueue.get(DbQueueType.VALIDATE_SHADOW_WITHDRAW_REQUEST);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.ENRICH_REQUEST_ITEM);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.VALIDATE_UPDATING_REQUEST);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.VALIDATE_CIS);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.FINISH_UPDATING_REQUEST);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.PUT_FF_INBOUND_REGISTRY);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.PUT_FF_OUTBOUND_REGISTRY);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.SEND_MBI_NOTIFICATION);
        assertCorrectQueueState(2, 1, 0, 1, state);

        state = stateByQueue.get(DbQueueType.PUBLISH_CALENDAR_SHOP_REQUEST_CHANGE);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.CIS_RETURN_INBOUND);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.UPDATE_CALENDARING_EXTERNAL_ID);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_FF_INBOUND_SUCCESS);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.GET_FF_OUTBOUND_SUCCESS);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.PUT_REGISTRY_SUCCESS);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.PUT_BOOKED_SLOT);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.REPROCESS_UPDATE_ITEMS_FROM_REGISTRIES);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.REPROCESS_REJECTED_BY_SERVICE);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.CREATE_DOCUMENT_TICKET);
        assertCorrectQueueState(2, 1, 0, 1, state);

        state = stateByQueue.get(DbQueueType.BUILD_ACCEPTABLE_GOODS_FILE_REQUEST);
        assertCorrectQueueState(1, 1, 0, 0, state);

        state = stateByQueue.get(DbQueueType.BUILD_UNACCEPTABLE_GOODS_FILE_REQUEST);
        assertCorrectQueueState(1, 1, 0, 0, state);

        state = stateByQueue.get(DbQueueType.BUILD_ADDITIONAL_SECONDARY_RECEPTION_ACT_REQUEST);
        assertCorrectQueueState(1, 1, 0, 0, state);

        state = stateByQueue.get(DbQueueType.BUILD_ANOMALY_CONTAINERS_WITHDRAW_ACT_REQUEST);
        assertCorrectQueueState(1, 1, 0, 0, state);

        state = stateByQueue.get(DbQueueType.UPDATE_TRACKER_STATUS);
        assertCorrectQueueState(1, 1, 0, 0, state);

        state = stateByQueue.get(DbQueueType.ENRICH_RETURN_REGISTRY);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.ENRICH_RETURN_REGISTRY_SEPARATE_TRANSACTIONS);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.CANCEL_REQUEST);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.LES_RETURN_BOX_EVENT);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.CREATE_AUTO_ADDITIONAL_SUPPLY_ON_UNKNOWN_BOXES);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.UPDATE_TRACKER_STATUS_SLOW);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.SEND_COMMON_REQUEST_TO_SERVICE);
        assertCorrectQueueState(0, 0, 0, 0, state);

        state = stateByQueue.get(DbQueueType.SEND_SUPPLY_REQUEST_TO_SERVICE);
        assertCorrectQueueState(0, 0, 0, 0, state);
    }

    @Test
    @DatabaseSetup("classpath:repository/db-queue-repository/before_find_delayed.xml")
    void findAllProcessTimeGreaterThan() {
        List<Long> taskIds = dbQueueRepository.findAllProcessTimeGreaterThan(LocalDateTime.of(2020, 12, 12, 0, 0));

        assertions.assertThat(taskIds.size()).isEqualTo(2);
        assertions.assertThat(taskIds.contains(2L)).isEqualTo(true);
        assertions.assertThat(taskIds.contains(3L)).isEqualTo(true);
    }

    private void assertCorrectQueueState(
            long elementsInQueue, long elementsWithoutRetries,
            long elementsWithFewRetries, long elementsWithManyRetries,
            @Nonnull DbQueueState state
    ) {
        assertions.assertThat(state.getElementsInQueue()).isEqualTo(elementsInQueue);
        Map<NumberOfRetriesInterval, Long> intervalToCount = state.getNumberOfElementsWithRetriesInterval();
        assertions.assertThat(intervalToCount.get(NumberOfRetriesInterval.NO_RETRIES))
                .isEqualTo(elementsWithoutRetries);
        assertions.assertThat(intervalToCount.get(NumberOfRetriesInterval.FEW_RETRIES))
                .isEqualTo(elementsWithFewRetries);
        assertions.assertThat(intervalToCount.get(NumberOfRetriesInterval.MANY_RETRIES))
                .isEqualTo(elementsWithManyRetries);
    }
}
