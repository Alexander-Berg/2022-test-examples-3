package ru.yandex.market.pricingmgmt.loaders.hacksskupromomatview

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.api.ControllerTest
import ru.yandex.market.pricingmgmt.loaders.HackSskuPromoAggregateLoader
import ru.yandex.market.pricingmgmt.repository.postgres.HackSskuPromoAggregateRepository
import ru.yandex.market.pricingmgmt.service.EnvironmentService
import ru.yandex.market.pricingmgmt.service.TimeService
import java.time.OffsetDateTime
import java.time.ZoneOffset

class HackSskuPromoAggregateLoaderTest(
    @Autowired private val hackSskuPromoAggregateLoader: HackSskuPromoAggregateLoader,
    @Autowired private val hackSskuPromoAggregateRepository: HackSskuPromoAggregateRepository,
    @Autowired private val environmentService: EnvironmentService,
    @Autowired private val timeService: TimeService
) : ControllerTest() {

    @BeforeEach
    internal fun setUp() {
        Mockito.`when`(timeService.getNowOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2021, 12, 23, 8, 0, 0, 0, ZoneOffset.UTC))
    }

    @Test
    @DbUnitDataSet(before = ["HackSskuPromoMatviewLoaderTest.testEnvironmentFeatureFlag.before.csv"])
    fun testEnvironmentFeatureFlag() {
        hackSskuPromoAggregateLoader.run()

        val list = hackSskuPromoAggregateRepository.selectHackSskuPromoAggregate()
        Assertions.assertEquals(0, list.size)
    }

    @Test
    @DbUnitDataSet(before = ["HackSskuPromoMatviewLoaderTest.testWithData.before.csv"])
    fun testWithData() {
        hackSskuPromoAggregateLoader.run()

        val list = hackSskuPromoAggregateRepository.selectHackSskuPromoAggregate()
        Assertions.assertEquals(1, list.size)
        Assertions.assertEquals("000111.1", list[0])
    }

    @Test
    @DbUnitDataSet(before = ["HackSskuPromoMatviewLoaderTest.testWithNoData.before.csv"])
    fun testWithNoData() {
        hackSskuPromoAggregateLoader.run()

        val list = hackSskuPromoAggregateRepository.selectHackSskuPromoAggregate()
        Assertions.assertEquals(0, list.size)
    }

    @Test
    @DbUnitDataSet(before = ["HackSskuPromoMatviewLoaderTest.testWithData.before.csv"])
    fun testRefresh() {
        hackSskuPromoAggregateLoader.run()
        var list = hackSskuPromoAggregateRepository.selectHackSskuPromoAggregate()
        Assertions.assertEquals(1, list.size)
        Assertions.assertEquals("000111.1", list[0])

        Mockito.`when`(timeService.getNowOffsetDateTime())
            .thenReturn(OffsetDateTime.of(2021, 12, 23, 23, 30, 0, 0, ZoneOffset.UTC))
        hackSskuPromoAggregateLoader.run()
        list = hackSskuPromoAggregateRepository.selectHackSskuPromoAggregate()
        Assertions.assertEquals(0, list.size)
    }

    @Test
    @DbUnitDataSet(before = ["HackSskuPromoMatviewLoaderTest.testRecreate.before.csv"])
    fun testDaysBefore() {
        environmentService.setLong(EnvironmentService.B2B_RESTRICT_FOR_PROMO_BEFORE_DAYS, 15)
        hackSskuPromoAggregateLoader.run()
        var list = hackSskuPromoAggregateRepository.selectHackSskuPromoAggregate()
        Assertions.assertEquals(1, list.size)
        Assertions.assertEquals("000111.1", list[0])

        environmentService.setLong(EnvironmentService.B2B_RESTRICT_FOR_PROMO_BEFORE_DAYS, 1)
        hackSskuPromoAggregateLoader.run()
        list = hackSskuPromoAggregateRepository.selectHackSskuPromoAggregate()
        Assertions.assertEquals(0, list.size)
    }
}
