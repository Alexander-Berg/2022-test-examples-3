package ru.yandex.market.abo.core.rating.partner.dynamic

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.CANCELLATION_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.LATE_SHIP_RATE
import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric.RETURN_RATE
import ru.yandex.market.abo.cpa.order.model.PartnerModel.DSBB

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 21.10.2021
 */
class PartnerRatingDynamicRepoTest @Autowired constructor(
    private val repo: PartnerRatingDynamicRepo
) : EmptyTest() {

    @BeforeEach
    fun init() {
        repo.save(PartnerRatingDynamic(
            CALC_DATE, 123, DSBB,
            100.0,
            2.0,
            RatingComponentDiffsWrapper(listOf(
                RatingComponentDiff(LATE_SHIP_RATE, 1.0, 0.02),
                RatingComponentDiff(RETURN_RATE, 1.0, 0.02),
                RatingComponentDiff(CANCELLATION_RATE, 1.0, 0.02)
            ))
        ))
        flushAndClear()
    }

    @Test
    fun `find dynamics by calc date test`() = assertEquals(1, repo.findCalcDateDynamics(CALC_DATE, DSBB).size)

    @Test
    fun `mark dynamics not actual test`() {
        repo.markNotActualBefore(CALC_DATE, DSBB)
        flushAndClear()
        val actualDynamics = repo.findCalcDateDynamics(CALC_DATE, DSBB).filter { it.actual }
        assertTrue(actualDynamics.isEmpty())
    }

    @Test
    fun `delete dynamics by calc date test`() {
        repo.deleteByCalcDateBefore(CALC_DATE, DSBB)
        flushAndClear()
        assertTrue(repo.findCalcDateDynamics(CALC_DATE, DSBB).isEmpty())
    }

    companion object {
        private val CALC_DATE = LocalDate.of(2021, 10, 21)
    }
}
