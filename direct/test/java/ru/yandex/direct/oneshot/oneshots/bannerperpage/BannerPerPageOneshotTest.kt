package ru.yandex.direct.oneshot.oneshots.bannerperpage

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.OneshotTestsUtils.Companion.hasDefect
import ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo

@OneshotTest
@RunWith(SpringRunner::class)
class BannerPerPageOneshotTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var dsl: DslContextProvider

    @Autowired
    lateinit var oneshot: BannerPerPageOneshot

    @Test
    fun validation_whenValidInputData_success() {
        val inputData = InputData(maxValue = 30L, newValue = 5L)

        val vr = oneshot.validate(inputData)

        assertThat(vr.hasAnyErrors()).`as`("hasAnyErrors").isFalse()
    }

    @Test
    fun validation_whenNotValidInputData_failure() {
        val inputData = InputData(maxValue = 0, newValue = -1L)

        val vr = oneshot.validate(inputData)

        assertSoftly {
            it.assertThat(vr).`as`("maxValueError")
                    .hasDefect("maxValue", greaterThanOrEqualTo(1L))
            it.assertThat(vr).`as`("newValueError")
                    .hasDefect("newValue", greaterThanOrEqualTo(0L))
        }
    }

    @Test
    fun execute_whenFirstIteration_success() {
        val campaignInfo = steps.campaignSteps().createActivePerformanceCampaign()
        setBannersPerPage(campaignInfo, 40L)
        val inputData = InputData(maxValue = 30, newValue = 5)

        val state = oneshot.execute(inputData, null, campaignInfo.shard)!!

        val actualValue = getBannersPerPage(campaignInfo)
        assertSoftly {
            it.assertThat(actualValue).`as`("actualValue")
                    .isEqualTo(5)
            it.assertThat(state.lastCid).`as`("lastCid")
                    .isEqualTo(campaignInfo.campaignId)
        }
    }

    @Test
    fun execute_whenNextIteration_success() {
        val newValue = 5L
        val oldValue = 50L
        val clientInfo = steps.clientSteps().createDefaultClient()
        val firstCampaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo)
        setBannersPerPage(firstCampaignInfo, oldValue)
        val secondCampaignInfo = steps.campaignSteps().createActivePerformanceCampaign(clientInfo)
        setBannersPerPage(secondCampaignInfo, oldValue)
        val inputData = InputData(maxValue = 30, newValue = newValue)
        val prevState = State(firstCampaignInfo.campaignId)

        oneshot.execute(inputData, prevState, clientInfo.shard)

        val firstActualValue = getBannersPerPage(firstCampaignInfo)
        val secondActualValue = getBannersPerPage(secondCampaignInfo)
        assertSoftly {
            it.assertThat(firstActualValue).`as`("firstActualValue")
                    .isEqualTo(oldValue)
            it.assertThat(secondActualValue).`as`("secondActualValue")
                    .isEqualTo(newValue)
        }
    }

    @Test
    fun execute_whenLastIteration_success() {
        val campaignInfo = steps.campaignSteps().createActivePerformanceCampaign()
        val inputData = InputData(maxValue = 30, newValue = 5)
        val prevState = State(lastCid = campaignInfo.campaignId)

        val state = oneshot.execute(inputData, prevState, campaignInfo.shard)

        assertThat(state).`as`("state").isNull()
    }

    private fun getBannersPerPage(campaignInfo: CampaignInfo) =
            dsl.ppc(campaignInfo.shard)
                    .select(Tables.CAMP_OPTIONS.BANNERS_PER_PAGE)
                    .from(Tables.CAMP_OPTIONS)
                    .where(Tables.CAMP_OPTIONS.CID.eq(campaignInfo.campaignId))
                    .fetchOne(Tables.CAMP_OPTIONS.BANNERS_PER_PAGE)

    private fun setBannersPerPage(campaignInfo: CampaignInfo, newValue: Long) =
            dsl.ppc(campaignInfo.shard)
                    .update(Tables.CAMP_OPTIONS)
                    .set(Tables.CAMP_OPTIONS.BANNERS_PER_PAGE, newValue)
                    .where(Tables.CAMP_OPTIONS.CID.eq(campaignInfo.campaignId))
                    .execute()

}
