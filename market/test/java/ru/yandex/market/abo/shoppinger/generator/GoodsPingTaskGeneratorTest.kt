package ru.yandex.market.abo.shoppinger.generator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType
import ru.yandex.market.abo.core.pinger.model.MpLastFireTime
import ru.yandex.market.abo.core.pinger.model.MpSchedule
import ru.yandex.market.abo.core.pinger.model.MpScheduleState
import ru.yandex.market.abo.core.pinger.service.MpScheduleService
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import java.time.LocalDateTime


internal class GoodsPingTaskGeneratorTest @Autowired constructor(
    val goodsPingTaskGenerator: GoodsPingTaskGenerator,
    val mpScheduleService: MpScheduleService,
    val mpLastFireTimeUpdater: PgBatchUpdater<MpLastFireTime>,
    val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @ParameterizedTest
    @MethodSource("schedule states method source")
    fun getTasks(state: MpScheduleState, firedMinutesAgo: Long, taskCount: Int) {
        val schedule = MpSchedule(PARTNER_ID, GEN, state, LocalDateTime.now())
        mpScheduleService.save(listOf(schedule))
        flushAndClear()

        val lastFireTime = MpLastFireTime(PARTNER_ID, GEN, LocalDateTime.now().minusMinutes(firedMinutesAgo))
        mpLastFireTimeUpdater.insertOrUpdate(listOf(lastFireTime))
        flushAndClear()

        jdbcTemplate.update(
            """
            INSERT INTO mp_url_goods VALUES
            (1, ${GEN.id}, $PARTNER_ID, '${URL}1', '$WARE_MD5'),
            (2, ${GEN.id}, $PARTNER_ID, '${URL}2', '$WARE_MD5'),
            (3, ${GEN.id}, $PARTNER_ID, '${URL}3', '$WARE_MD5'),
            (4, ${GEN.id}, $PARTNER_ID, '${URL}4', '$WARE_MD5'),
            (5, ${GEN.id}, $PARTNER_ID, '${URL}5', '$WARE_MD5')
        """.trimIndent()
        )

        jdbcTemplate.update(
            """
            INSERT INTO
               pinger_content_task (id, gen_id, shop_id, url, ware_md5, http_status, content_size, consumed_time)
            VALUES
            (1, ${GEN.id}, $PARTNER_ID, '${URL}1', '$WARE_MD5', 200, 500, now() - interval '2 minutes'),
            (2, ${GEN.id}, $PARTNER_ID, '${URL}2', '$WARE_MD5', 200, 499, now() - interval '1 minute'),
            (3, ${GEN.id}, $PARTNER_ID, '${URL}3', '$WARE_MD5', 404, 500, now())
        """.trimIndent()
        )

        val tasks = goodsPingTaskGenerator.getTasks(state)
        assertEquals(taskCount, tasks.size)
    }


    companion object {
        private const val PARTNER_ID = 1L
        private const val URL = "url"
        private const val WARE_MD5 = "ware_md5"
        private val GEN = MpGeneratorType.GOODS_PING

        @JvmStatic
        fun `schedule states method source`(): Iterable<Arguments> = listOf(
            Arguments.of(MpScheduleState.PING, 4, 0),
            Arguments.of(MpScheduleState.PING, 6, 1),
            Arguments.of(MpScheduleState.FREQUENT_PING, 1, 0),
            Arguments.of(MpScheduleState.FREQUENT_PING, 3, 3),
            Arguments.of(MpScheduleState.CONTROL_PING, 4, 0),
            Arguments.of(MpScheduleState.CONTROL_PING, 6, 1)
        )
    }
}
