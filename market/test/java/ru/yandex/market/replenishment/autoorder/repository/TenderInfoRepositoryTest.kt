package ru.yandex.market.replenishment.autoorder.repository

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.repository.postgres.TenderInfoRepository
import ru.yandex.market.replenishment.autoorder.service.TimeService
import ru.yandex.market.replenishment.autoorder.utils.TestUtils
import java.time.LocalDateTime

class TenderInfoRepositoryTest : FunctionalTest() {
    companion object {
        private val NOW_DATE_TIME = LocalDateTime.of(2022, 6, 2, 10, 48, 0)
    }

    @Autowired
    lateinit var tenderInfoRepository: TenderInfoRepository

    @Autowired
    lateinit var timeService: TimeService

    @Test
    @DbUnitDataSet(
        before = ["TenderInfoRepositoryTest_updatePriceDateToWithDefaultParams_isOk.before.csv"],
        after = ["TenderInfoRepositoryTest_updatePriceDateToWithDefaultParams_isOk.after.csv"]
    )
    fun updatePriceDateToWithDefaultParams_isOk() {
        TestUtils.mockTimeService(timeService, NOW_DATE_TIME)
        tenderInfoRepository.updatePriceDateTo(1L, NOW_DATE_TIME.toLocalDate())
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderInfoRepositoryTest_updatePriceDateToWithCatteamParams_isOk.before.csv"],
        after = ["TenderInfoRepositoryTest_updatePriceDateToWithCatteamParams_isOk.after.csv"]
    )
    fun updatePriceDateToWithCatteamParams_isOk() {
        TestUtils.mockTimeService(timeService, NOW_DATE_TIME)
        tenderInfoRepository.updatePriceDateTo(1L, NOW_DATE_TIME.toLocalDate())
    }
}
