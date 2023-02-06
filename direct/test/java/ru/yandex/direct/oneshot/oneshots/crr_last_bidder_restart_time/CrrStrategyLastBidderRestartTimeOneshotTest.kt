package ru.yandex.direct.oneshot.oneshots.crr_last_bidder_restart_time

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignWithStrategy
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.testing.data.TestCampaigns.dafaultAverageCpaPayForConversionStrategy
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetCrrStrategy
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.crr_last_bidder_restart_time.CrrStrategyLastBidderRestartTimeOneshot.Companion.CrrStrategySetLastBidderRestartTimePosition
import java.time.LocalDateTime


@OneshotTest
@RunWith(SpringRunner::class)
class CrrStrategyLastBidderRestartTimeOneshotTest {

    @Autowired
    private lateinit var steps: Steps

    private lateinit var oneshot: CrrStrategyLastBidderRestartTimeOneshot

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var campaignModifyRepository: CampaignModifyRepository

    @Before
    fun init() {
        campaignModifyRepository = Mockito.spy(campaignModifyRepository)
        oneshot = CrrStrategyLastBidderRestartTimeOneshot(campaignTypedRepository, campaignModifyRepository)
    }

    @Test
    fun setLastBidderRestartTime_Success() {
        val campaign = fullTextCampaign().withStrategy(crrStrategy(false))
        val campaignInfo = steps.textCampaignSteps().createCampaign(campaign)

        val result = oneshot.execute(null, null, campaignInfo.shard)
        val actualCampaign =
            campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(), listOf(campaignInfo.id)).firstOrNull()

        assertThat(actualCampaign).isNotNull
        assertThat(extractLastBidderRestartTime(actualCampaign)).isNotNull

        assertThat(result?.lastCampaignId).isEqualTo(campaignInfo.id)
        verify(campaignModifyRepository, times(1)).updateCampaignsTable(eq(campaignInfo.shard), any())
    }

    @Test
    fun doNothingIfBidderRestartTimeNotEmpty() {
        val campaign = fullTextCampaign().withStrategy(crrStrategy(true))
        val campaignInfo = steps.textCampaignSteps().createCampaign(campaign)

        val result = oneshot.execute(null, null, campaignInfo.shard)
        val actualCampaign =
            campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(), listOf(campaignInfo.id)).firstOrNull()

        assertThat(actualCampaign).isNotNull
        assertThat(extractLastBidderRestartTime(actualCampaign)).isNotNull

        assertThat(result?.lastCampaignId).isEqualTo(campaignInfo.id)
        verify(campaignModifyRepository, never()).updateCampaignsTable(eq(campaignInfo.shard), any())
    }

    @Test
    fun updateOnlyUnprocessedStrategy() {
        val processedCampaign = fullTextCampaign().withStrategy(crrStrategy(false))
        val unprocessedCampaign = fullTextCampaign().withStrategy(crrStrategy(false))
        val processedCampaignInfo = steps.textCampaignSteps().createCampaign(processedCampaign)
        val unprocessedCampaignInfo = steps.textCampaignSteps().createCampaign(unprocessedCampaign)

        val startPosition = CrrStrategySetLastBidderRestartTimePosition(processedCampaignInfo.campaignId)
        val result = oneshot.execute(null, startPosition, processedCampaignInfo.shard)
        val actualCampaign = campaignTypedRepository.getTypedCampaigns(
            unprocessedCampaignInfo.getShard(),
            listOf(unprocessedCampaignInfo.id)
        ).firstOrNull()

        assertThat(actualCampaign).isNotNull
        assertThat(extractLastBidderRestartTime(actualCampaign)).isNotNull

        assertThat(result?.lastCampaignId).isEqualTo(unprocessedCampaignInfo.id)
        verify(campaignModifyRepository, times(1)).updateCampaignsTable(eq(unprocessedCampaignInfo.shard), any())
    }

    @Test
    fun updateOnlyCrrStrategy() {
        val crrCampaign = fullTextCampaign().withStrategy(crrStrategy(false))
        val cpaCampaign = fullTextCampaign().withStrategy(cpaStrategy())
        val crrCampaignInfo = steps.textCampaignSteps().createCampaign(crrCampaign)
        val cpaCampaignInfo = steps.textCampaignSteps().createCampaign(cpaCampaign)

        listOf(crrCampaignInfo.shard, cpaCampaignInfo.shard)
            .map { oneshot.execute(null, null, it) }

        val actualCrrCampaign =
            campaignTypedRepository.getTypedCampaigns(crrCampaignInfo.getShard(), listOf(crrCampaignInfo.id))
                .firstOrNull()
        val actualCpaCampaign =
            campaignTypedRepository.getTypedCampaigns(cpaCampaignInfo.getShard(), listOf(cpaCampaignInfo.id))
                .firstOrNull()


        assertThat(extractLastBidderRestartTime(actualCrrCampaign)).isNotNull
        assertThat(extractLastBidderRestartTime(actualCpaCampaign)).isNull()

        verify(campaignModifyRepository, times(1)).updateCampaignsTable(eq(crrCampaignInfo.shard), any())
    }

    @Test
    fun shouldFinishAfterFewIterations() {
        val campaign = fullTextCampaign().withStrategy(crrStrategy(false))
        val campaignInfo = steps.textCampaignSteps().createCampaign(campaign)

        val firstIteration = oneshot.execute(null, null, campaignInfo.shard)
        val actualCampaign =
            campaignTypedRepository.getTypedCampaigns(campaignInfo.getShard(), listOf(campaignInfo.id)).firstOrNull()

        assertThat(actualCampaign).isNotNull
        assertThat(extractLastBidderRestartTime(actualCampaign)).isNotNull

        assertThat(firstIteration?.lastCampaignId).isEqualTo(campaignInfo.id)

        val nextIteration = oneshot.execute(null, firstIteration, campaignInfo.shard)
        assertThat(nextIteration).isNull()
        verify(campaignModifyRepository, times(1)).updateCampaignsTable(eq(campaignInfo.shard), any())
    }


    private fun crrStrategy(withLastBidderRestartTime: Boolean): DbStrategy {
        val time = if (withLastBidderRestartTime) LocalDateTime.now() else null
        val strategy = defaultAutobudgetCrrStrategy(123)
        strategy.strategyData.withLastBidderRestartTime(time)
        return strategy
    }

    private fun cpaStrategy(): DbStrategy {
        val strategy = dafaultAverageCpaPayForConversionStrategy()
        strategy.strategyData.withLastBidderRestartTime(null)
        return strategy
    }

    private fun extractLastBidderRestartTime(campaign: BaseCampaign?): LocalDateTime? =
        (campaign as? CampaignWithStrategy)?.strategy?.strategyData?.lastBidderRestartTime
}
