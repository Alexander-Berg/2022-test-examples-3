package ru.yandex.market.logistics.calendaring.solomon.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.logistics.calendaring.dbqueue.DbqueueTaskType
import ru.yandex.market.logistics.calendaring.dbqueue.enums.NumberOfRetriesInterval
import ru.yandex.market.logistics.calendaring.dbqueue.state.DbQueueState
import ru.yandex.market.logistics.calendaring.solomon.base.BaseSolomonContextualTest

class DbQueueRepositoryTest(@Autowired jdbcTemplate: JdbcTemplate,
                            @Autowired private val repository: DbQueueRepository) :
    BaseSolomonContextualTest(jdbcTemplate) {

    @DatabaseSetup(value = ["classpath:fixtures/empty.xml"])
    @Test
    fun getStateByQueueWorksCorrect() {
        insert(DbqueueTaskType.UPDATE_BOOKING_EVENT, "{\"task\":123}", 0)
        insert(DbqueueTaskType.UPDATE_BOOKING_EVENT, "{\"task\":124}", 11)
        insert(DbqueueTaskType.UPDATE_BOOKING_EVENT, "{\"task\":125}", 16)
        insert(DbqueueTaskType.UPDATE_BOOKING_EVENT, "{\"task\":126}", 20)
        val stateByQueue: Map<DbqueueTaskType, DbQueueState> = repository.getStateByQueue()
        val updateBookingEventState: DbQueueState? = stateByQueue[DbqueueTaskType.UPDATE_BOOKING_EVENT]
        assertNotNullStateIsCorrect(updateBookingEventState, 4L, 1L, 1L, 2L)
    }

    private fun assertNotNullStateIsCorrect(state: DbQueueState?,
                                            elementsInQueue: Long,
                                            elementsWithoutRetries: Long,
                                            elementsWithFewRetries: Long,
                                            elementsWithManyRetries: Long) {
        softly.assertThat(state).isNotNull
        softly.assertThat(state!!.elementsInQueue).isEqualTo(elementsInQueue)
        val elementsWithRetries: Map<NumberOfRetriesInterval, Long> = state.numberOfElementsWithRetriesInterval
        softly.assertThat(elementsWithRetries[NumberOfRetriesInterval.NO_RETRIES])
            .isEqualTo(elementsWithoutRetries)
        softly.assertThat(elementsWithRetries[NumberOfRetriesInterval.FEW_RETRIES])
            .isEqualTo(elementsWithFewRetries)
        softly.assertThat(elementsWithRetries[NumberOfRetriesInterval.MANY_RETRIES])
            .isEqualTo(elementsWithManyRetries)
    }
}
