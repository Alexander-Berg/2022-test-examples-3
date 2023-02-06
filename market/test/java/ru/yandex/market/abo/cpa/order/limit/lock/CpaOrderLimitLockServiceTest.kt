package ru.yandex.market.abo.cpa.order.limit.lock

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.CannotAcquireLockException
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.limit.CpaOrderLimitPartner
import ru.yandex.market.abo.cpa.order.model.PartnerModel

/**
 * @author komarovns
 */
class CpaOrderLimitLockServiceTest @Autowired constructor(
    private val cpaOrderLimitLockService: CpaOrderLimitLockService,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @AfterEach
    fun tearDown() {
        jdbcTemplate.update("TRUNCATE TABLE cpa_order_limit_lock; COMMIT;")
    }

    @Test
    fun lockTest() {
        val partner = CpaOrderLimitPartner(1, PartnerModel.DSBS)
        val latchFirst = CountDownLatch(1)
        val latchSecond = CountDownLatch(1)

        val pool = Executors.newFixedThreadPool(2)

        val feature = pool.submit {
            latchSecond.await()
            try {
                cpaOrderLimitLockService.doInLock(partner) {
                    throw RuntimeException("this exception shouldn't be thrown")
                }
            } finally {
                latchFirst.countDown()
            }
        }

        pool.submit {
            cpaOrderLimitLockService.doInLock(partner) {
                latchSecond.countDown()
                latchFirst.await()
            }
        }.get()

        val exception = assertThrows<ExecutionException> { feature.get() }
        assertThat(exception).hasCauseInstanceOf(CannotAcquireLockException::class.java)
    }
}
