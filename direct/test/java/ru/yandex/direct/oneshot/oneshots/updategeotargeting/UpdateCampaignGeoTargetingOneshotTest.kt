package ru.yandex.direct.oneshot.oneshots.updategeotargeting

import com.nhaarman.mockitokotlin2.anyOrNull
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
import org.mockito.Mockito.spy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.direct.ytwrapper.model.YtTable

@OneshotTest
@RunWith(JUnitParamsRunner::class)
class UpdateCampaignGeoTargetingOneshotTest : UpdateGeoTargetingParamsHolder() {

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
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var oneshot: UpdateCampaignGeoTargetingOneshot

    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        oneshot = spy(oneshot);
        doReturn(ytOperator).`when`(oneshot).ytOperator(anyOrNull())
        doReturn(YtTable("test/path")).`when`(oneshot).ytTable(anyOrNull())
        clientInfo = steps.clientSteps().createDefaultClient()
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
        val campaignInfo = steps.campaignSteps().createCampaign(
            TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid).withGeo(initialGeo)
        )

        doReturn(1L).`when`(oneshot).getTableRowCount(anyOrNull())
        doReturn(listOf(CampaignWithShard(shard = campaignInfo.shard, campaignId = campaignInfo.campaignId)))
            .`when`(oneshot).readFromYtTable(
                anyOrNull(),
                any(CampaignWithShardTableRow::class.java),
                anyOrNull(),
                anyOrNull()
            )

        val state = oneshot.execute(inputData, null)
        oneshot.execute(inputData, state)

        val actualGeo = campaignRepository.getCampaigns(campaignInfo.shard, listOf(campaignInfo.campaignId))[0].geo
        assertThat(actualGeo).containsExactlyInAnyOrderElementsOf(expectedGeo)
    }
}
