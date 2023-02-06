package ru.yandex.direct.oneshot.oneshots.bsexport

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.BsExportMultipliersObject.timeTargetChanged
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.TimeTargetChangedInfo
import ru.yandex.direct.libs.timetarget.TimeTarget
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.BsExportMultipliersService
import ru.yandex.direct.oneshot.configuration.OneshotTest

@OneshotTest
@RunWith(SpringRunner::class)
class BsExportMultipliersTimeTargetOneshotTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var dsl: DslContextProvider

    private lateinit var oneshot: BsExportMultipliersTimeTargetOneshot

    private lateinit var bsExportMultipliersService: BsExportMultipliersService

    private var shard: Int? = null
    private var campaignId: Long? = null

    @Before
    fun before() {
        bsExportMultipliersService = Mockito.mock(BsExportMultipliersService::class.java)
        oneshot = BsExportMultipliersTimeTargetOneshot(dsl, bsExportMultipliersService)

        val textCampaign = TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(manualStrategy())
                .withTimeTarget(TimeTarget.parseRawString("1ABCDEFGHIJKLMNOPQRSTUVWX2ABCD"))
        val campaign = steps.campaignSteps().createCampaign(textCampaign)
        shard = campaign.shard
        campaignId = campaign.campaignId
    }

    @Test
    fun execute_Initial() {
        val state = oneshot.execute(null, null, shard!!)

        Assertions.assertThat(state).`as`("state").isNotNull
        Assertions.assertThat(state!!.lastCampaignId).isEqualTo(0)

        verify(bsExportMultipliersService, Mockito.never())
                .updateMultipliers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyList())
    }

    @Test
    fun execute() {
        val state = oneshot.execute(null, State(campaignId!!.minus(1)), shard!!)

        Assertions.assertThat(state).`as`("state").isNotNull
        Assertions.assertThat(state!!.lastCampaignId).isEqualTo(campaignId)

        verify(bsExportMultipliersService).updateMultipliers(shard!!, listOf(
                timeTargetChanged(TimeTargetChangedInfo(campaignId!!), 0, "", "")))
    }

    @Test
    fun execute_Last() {
        val state: State? = oneshot.execute(null, State(campaignId!!), shard!!)

        Assertions.assertThat(state).isNull()

        verify(bsExportMultipliersService, Mockito.never())
                .updateMultipliers(ArgumentMatchers.anyInt(), ArgumentMatchers.anyList())
    }
}
