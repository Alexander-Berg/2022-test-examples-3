package ru.yandex.direct.oneshot.oneshots.updategeotargeting

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.spy
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doReturn
import org.mockito.MockitoAnnotations.openMocks
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.direct.ytwrapper.model.YtTable

@OneshotTest
@RunWith(JUnitParamsRunner::class)
class UpdateAdGroupGeoTargetingOneshotTest : UpdateGeoTargetingParamsHolder() {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var steps: Steps

    @Mock
    private lateinit var ytOperator: YtOperator

    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    @Qualifier("updateAdGroupGeoTargetingOneshot")
    private lateinit var oneshot: UpdateAdGroupGeoTargetingOneshot

    @Before
    fun setUp() {
        openMocks(this)
        oneshot = spy(oneshot)
        doReturn(ytOperator).`when`(oneshot).ytOperator(anyOrNull())
        doReturn(YtTable("test/path")).`when`(oneshot).ytTable(anyOrNull())
    }

    @Test
    @Parameters(method = "parametersSingleUpdate")
    fun testExecute(initialGeo: Set<Int>, expectedGeo: Set<Int>) {
        doTest(
            UpdateGeoInputData(listOf(UpdateRegionParam(14, 10819, 1))),
            initialGeo,
            expectedGeo
        )
    }

    @Test
    @Parameters(method = "parametersMultiUpdates")
    fun testExecute_multiUpdates(initialGeo: Set<Int>, expectedGeo: Set<Int>) {
        doTest(
            UpdateGeoInputData(
                listOf(
                    UpdateRegionParam(21623, 1, 98604),
                    UpdateRegionParam(100471, 1, 98604)
                )
            ),
            initialGeo,
            expectedGeo
        )
    }

    private fun doTest(inputData: UpdateGeoInputData, initialGeo: Set<Int>, expectedGeo: Set<Int>) {
        val adGroupInfo = steps.adGroupSteps().createAdGroup(
            TestGroups.activeTextAdGroup().withGeo(initialGeo.map { it.toLong() })
        )

        doReturn(1L).`when`(oneshot).getTableRowCount(anyOrNull())
        doReturn(listOf(AdGroupWithShard(shard = adGroupInfo.shard, pid = adGroupInfo.adGroupId)))
            .`when`(oneshot).readFromYtTable(
                anyOrNull(),
                any(AdGroupWithShardTableRow::class.java),
                anyOrNull(),
                anyOrNull()
            )

        val state = oneshot.execute(inputData, null)
        oneshot.execute(inputData, state)

        val actualGeo = adGroupRepository.getAdGroups(adGroupInfo.shard, listOf(adGroupInfo.adGroupId))[0].geo
        assertThat(actualGeo).containsExactlyInAnyOrderElementsOf(expectedGeo.map { it.toLong() })
    }
}
