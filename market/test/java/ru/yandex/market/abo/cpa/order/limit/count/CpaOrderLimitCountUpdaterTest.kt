package ru.yandex.market.abo.cpa.order.limit.count

import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.cpa.order.model.PartnerModel
import ru.yandex.market.abo.cpa.order.model.PartnerModel.CROSSDOCK
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBS

/**
 * @author komarovns
 */
class CpaOrderLimitCountUpdaterTest @Autowired constructor(
    private val updater: CpaOrderLimitCountUpdater,
    private val repo: CpaOrderLimitCountRepo
) : EmptyTest() {

    @ParameterizedTest
    @CsvSource("1, 4", "-1, 2")
    fun update(updateCount: Long, expectedCount: Long) {
        repo.save(limitCount(DSBS, 3))
        flushAndClear()

        val updateResult = updater.update(listOf(
            limitCount(DSBS, updateCount),
            limitCount(CROSSDOCK, 2)
        ))

        sequenceOf(updateResult, repo.findAll()).forEach { countList ->
            assertThat(countList)
                .extracting({ it.key.partnerModel }, { it.count })
                .containsExactlyInAnyOrder(
                    tuple(DSBS, expectedCount),
                    tuple(CROSSDOCK, 2L)
                )
        }
    }

    private fun limitCount(model: PartnerModel, count: Long) = CpaOrderLimitCount(SHOP_ID, model.id, DAY, count)
}

private const val SHOP_ID = 774L
private val DAY = LocalDate.now()
