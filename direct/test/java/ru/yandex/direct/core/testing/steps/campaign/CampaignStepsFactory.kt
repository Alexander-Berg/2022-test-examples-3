package ru.yandex.direct.core.testing.steps.campaign

import org.springframework.stereotype.Component
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo

@Component
class CampaignStepsFactory(
    private val campaignSteps: Collection<CampaignSteps<*, out CampaignInfo<*>>>
) {

    fun createCampaign(campaignInfo: CampaignInfo<*>): CampaignInfo<*> {
        val step = resolveSteps(campaignInfo)
        return step.createCampaign(campaignInfo)
    }

    private fun resolveSteps(campaignInfo: CampaignInfo<*>): CampaignSteps<*, CampaignInfo<*>> {
        return (campaignSteps.find {
            campaignInfo::class == it.getCampaignInfoClass()
        } ?: throw UnsupportedOperationException("cannot find step for campaign class " + campaignInfo::class))
            as CampaignSteps<*, CampaignInfo<*>>
    }
}
