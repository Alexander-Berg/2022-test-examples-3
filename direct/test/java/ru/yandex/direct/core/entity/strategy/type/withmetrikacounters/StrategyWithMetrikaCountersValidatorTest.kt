package ru.yandex.direct.core.entity.strategy.type.withmetrikacounters

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.defect.CollectionDefects.maxCollectionSize
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@RunWith(JUnitParamsRunner::class)
internal class StrategyWithMetrikaCountersValidatorTest {
    fun testData(): Array<Array<Any>> = arrayOf(
        //проверки валидности id
//        arrayOf(
//            "counter cannot be null",
//            TestData(CampaignType.TEXT, listOf<Long?>(null), pathForFirstMetrikaCounter(), CommonDefects.notNull(), setOf(), setOf())
//        ),
//        arrayOf(
//            "counter cannot be zero",
//            TestData(CampaignType.TEXT, listOf(0), pathForFirstMetrikaCounter(), CommonDefects.validId(), setOf(), setOf())
//        ),
//        arrayOf(
//            "counters should be uniqe",
//            TestData(CampaignType.TEXT, listOf(1, 1), pathForFirstMetrikaCounter(), CollectionDefects.duplicatedElement(), setOf(1), setOf())
//        ),
//
//        //проверки для смартов
//        arrayOf(
//            "counters list cannot be empty for perf campaigns",
//            TestData(CampaignType.PERFORMANCE, listOf(), pathForMetrikaCounters(), minCollectionSize(1), setOf(), setOf())
//        ),
        arrayOf(
            "counters list without system counters cannot contains size more than 1 non-system counter for perf campaigns",
            TestData(
                CampaignType.PERFORMANCE,
                listOf(1, 2),
                pathForMetrikaCounters(),
                maxCollectionSize(1),
                setOf(1, 2),
                setOf()
            )
        ),
        arrayOf(
            "counter should be available for client for perf campaign",
            TestData(
                CampaignType.PERFORMANCE,
                listOf(1),
                pathForFirstMetrikaCounter(),
                CampaignDefects.metrikaCounterIsUnavailable(),
                setOf(),
                setOf()
            )
        ),
        arrayOf(
            "counters list cannot has size more than 1 for perf campaigns",
            TestData(
                CampaignType.PERFORMANCE,
                listOf(1, 2),
                pathForMetrikaCounters(),
                maxCollectionSize(1),
                setOf(),
                setOf()
            )
        ),
        arrayOf(
            "counters list with system counters cannot contains size more than 1 non-system counter for perf campaigns",
            TestData(
                CampaignType.PERFORMANCE,
                listOf(1, 2, 3),
                pathForMetrikaCounters(),
                maxCollectionSize(1),
                setOf(1, 2),
                setOf(2)
            )
        ),

        //проверка размера для остальных видов кампаний
        arrayOf(
            "counters list without system counters cannot contains size more than 100 non-system counter for perf campaigns",
            TestData(
                CampaignType.TEXT,
                (0..100L).toList(),
                pathForMetrikaCounters(),
                maxCollectionSize(100),
                (0..100L).toSet(),
                setOf()
            )
        ),
        arrayOf(
            "counters list without system counters cannot contains size more than 100 non-system counter for perf campaigns",
            TestData(
                CampaignType.TEXT,
                (0..101L).toList(),
                pathForMetrikaCounters(),
                maxCollectionSize(100),
                (0..101L).toSet(),
                setOf(101)
            )
        ),

        //валидные случаи
        arrayOf(
            "performance with one non-system counter ",
            TestData(CampaignType.PERFORMANCE, listOf(1), null, null, setOf(1), setOf())
        ),
        arrayOf(
            "performance with one non-system counter and one sytem",
            TestData(CampaignType.PERFORMANCE, listOf(1, 2), null, null, setOf(1, 2), setOf(2))
        ),
        arrayOf(
            "text with 100 non-system counters",
            TestData(CampaignType.TEXT, (1..100L).toList(), null, null, setOf(), setOf())
        ),
        arrayOf(
            "text with 100 non-system counters and one system",
            TestData(CampaignType.TEXT, (1..101L).toList(), null, null, setOf(), setOf(100))
        ),
    )

    private fun pathForFirstMetrikaCounter() =
        PathHelper.path(PathHelper.index(0))

    private fun pathForMetrikaCounters() = PathHelper.emptyPath()

    @Test
    @Parameters(method = "testData")
    @TestCaseName("{0}")
    fun `check that validation got invalid data`(description: String, testData: TestData) {
        val validator = StrategyWithMetrikaCountersValidator(testData.validationContainer())
        val validationResult = validator.apply(testData.strategyCounterIds)
        assertThat(validationResult).`is`(Conditions.matchedBy(testData.matcher()))
    }

    companion object {
        data class TestData(
            val campaignType: CampaignType,
            val strategyCounterIds: List<Long?>,
            val path: Path? = null,
            val defect: Defect<*>? = null,
            val availableCounterIds: Set<Long>,
            val systemCountersIds: Set<Long>
        ) {
            fun validationContainer() = StrategyWithMetrikaCountersValidator.ValidationContainer(
                campaignType,
                availableCounterIds,
                systemCountersIds
            )

            fun matcher(): Matcher<ValidationResult<Any, Defect<*>>> = if (defect != null) {
                Matchers.hasDefectDefinitionWith(
                    Matchers.validationError(
                        path,
                        defect
                    )
                )
            } else {
                Matchers.hasNoDefectsDefinitions()
            }
        }
    }

}
