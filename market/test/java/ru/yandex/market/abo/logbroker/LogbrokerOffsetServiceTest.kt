package ru.yandex.market.abo.logbroker

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData

/**
 * @author komarovns
 */
class LogbrokerOffsetServiceTest @Autowired constructor(
    private val logbrokerOffsetService: LogbrokerOffsetService,
    private val logbrokerOffsetRepo: LogbrokerOffsetRepo,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @AfterEach
    fun tearDown() {
        logbrokerOffsetRepo.deleteAllInBatch()
        flushAndClear()
        jdbcTemplate.update("COMMIT")
    }

    @Test
    fun `skip event`() {
        prepareOffset(0)
        val count = logbrokerOffsetService.processEventsAndUpdateOffset(READER, prepareBatch(0)) {
            it.count()
        }
        assertEquals(0, count)
    }

    @Test
    fun `process event`() {
        prepareOffset(0)
        val count = logbrokerOffsetService.processEventsAndUpdateOffset(READER, prepareBatch(1)) {
            it.count()
        }
        assertEquals(1, count)
    }

    @Test
    fun `process event only once`() {
        prepareOffset(0)
        logbrokerOffsetService.processEventsAndUpdateOffset(READER, prepareBatch(1)) {
            it.count()
        }
        flushAndClear()
        val count = logbrokerOffsetService.processEventsAndUpdateOffset(READER, prepareBatch(1)) {
            it.count()
        }
        assertEquals(0, count)
    }

    @Test
    fun `first call`() {
        val count = logbrokerOffsetService.processEventsAndUpdateOffset(READER, prepareBatch(1)) {
            it.count()
        }
        flushAndClear()
        assertEquals(1, count)
        assertEquals(1L, logbrokerOffsetRepo.findByIdOrNull(LogbrokerOffset.Key(READER, TOPIC, PARTITION))?.offset)
    }

    /**
     * 1) второй тред достаёт из базы оффсеты и лочится,
     * 2) основной тред достаёт и сохраняет оффсеты,
     * 3) второй тред сохраняет оффсеты => вылетает эксепшн
     */
    @Test
    fun `optimistic lock`() {
        prepareOffset(0)
        jdbcTemplate.update("COMMIT")

        val latchCurrent = CountDownLatch(1)
        val latchSecond = CountDownLatch(1)
        val pool = Executors.newSingleThreadExecutor()
        val feature = pool.submit {
            logbrokerOffsetService.processEventsAndUpdateOffset(READER, prepareBatch(1)) {
                latchCurrent.countDown()
                latchSecond.await()
                it.count()
            }
        }
        latchCurrent.await()
        logbrokerOffsetService.processEventsAndUpdateOffset(READER, prepareBatch(1)) {
            it.count()
        }
        flushAndClear()
        jdbcTemplate.update("COMMIT")
        latchSecond.countDown()
        val exception = assertThrows<ExecutionException> { feature.get() }
        assertThat(exception).hasCauseInstanceOf(OptimisticLockingFailureException::class.java)
    }

    private fun prepareOffset(offset: Long) {
        logbrokerOffsetRepo.save(LogbrokerOffset(
            LogbrokerOffset.Key(READER, TOPIC, PARTITION), offset
        ))
        flushAndClear()
    }

    private fun prepareBatch(offset: Long) = listOf(
        MessageBatch(TOPIC, PARTITION, listOf(MessageData(null, offset, null)))
    )
}

private const val READER = "reader"
private const val TOPIC = "topic"
private const val PARTITION = 0
