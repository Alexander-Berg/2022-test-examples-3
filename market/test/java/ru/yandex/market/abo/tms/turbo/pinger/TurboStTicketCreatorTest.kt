package ru.yandex.market.abo.tms.turbo.pinger

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType
import ru.yandex.market.abo.core.pinger.model.PingerContentTask
import ru.yandex.market.abo.core.pinger.model.TaskState
import ru.yandex.market.abo.core.pinger.service.PingerContentTaskService
import ru.yandex.market.abo.tms.pinger.st.GoodsStTicketCreator
import ru.yandex.market.abo.tms.pinger.st.TurboStTicketCreator
import java.time.LocalDateTime

class TurboStTicketCreatorTest @Autowired constructor(
    val manager: TurboStTicketCreator,
    val taskService: PingerContentTaskService,
    val goodsStCreator: GoodsStTicketCreator,
    val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @Test
    fun turboSql() {
        manager.generateTickets()
    }

    @Test
    fun goodsSql() {
        val shopId = 774L
        jdbcTemplate.update("insert into ext_vertical_share_shops(partner_id, domain, business_name, feed_url) " +
            "values ($shopId, 'domain', 'name', 'url')")

        for (i in 0..2) {
            taskService.save(
                PingerContentTask.builder()
                    .genId(MpGeneratorType.GOODS_PRICE.id)
                    .state(TaskState.CONTENT_FAIL)
                    .shopId(shopId)
                    .url(RND.nextLong().toString())
                    .offerId(RND.nextLong().toString())
                    .finishTime(LocalDateTime.now())
                    .build()
            )
        }
        entityManager.flush()
        goodsStCreator.generateTickets()
    }
}
