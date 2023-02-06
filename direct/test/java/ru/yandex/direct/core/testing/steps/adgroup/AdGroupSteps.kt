package ru.yandex.direct.core.testing.steps.adgroup

import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.testing.info.adgroup.AdGroupInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.steps.campaign.CampaignStepsFactory
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import kotlin.reflect.KClass

abstract class AdGroupSteps<A : AdGroup, T : AdGroupInfo<A>>(
    private val campaignStepsFactory: CampaignStepsFactory,
    private val dslContextProvider: DslContextProvider,
    private val adGroupRepository: AdGroupRepository,
    private val allowedAdGroupClasses: Set<KClass<out BaseCampaign>>,
) {
    fun createDefaultAdGroup(): T {
        return createAdGroup(getAdGroupInfo())
    }

    @Suppress("UNCHECKED_CAST")
    fun createDefaultAdGroup(campaignInfo: CampaignInfo<*>): T {
        return createAdGroup(getAdGroupInfo()
            .withCampaignInfo(campaignInfo) as T)
    }

    @Suppress("UNCHECKED_CAST")
    fun createAdGroup(campaignInfo: CampaignInfo<*>, adgroup: A): T {
        return createAdGroup(getAdGroupInfo()
            .withCampaignInfo(campaignInfo)
            .withAdGroup(adgroup) as T)
    }

    @Suppress("UNCHECKED_CAST")
    fun createAdGroup(adgroup: A): T {
        return createAdGroup(getAdGroupInfo()
            .withAdGroup(adgroup) as T)
    }

    fun createAdGroup(adGroupInfo: T): T {
        checkAdGroupInfoConsistency(adGroupInfo)

        initializeCampaignInfo(adGroupInfo)

        // создаем кампанию
        if (adGroupInfo.campaignInfo.typedCampaign.id == null) {
            campaignStepsFactory.createCampaign(adGroupInfo.campaignInfo)
        }

        val adGroup = adGroupInfo.adGroup
        adGroup.campaignId = adGroupInfo.campaignId

        val clientInfo = adGroupInfo.clientInfo

        createRelations(adGroupInfo)

        adGroupRepository.addAdGroups(dslContextProvider.ppc(clientInfo.shard).configuration(),
            clientInfo.clientId!!, listOf(adGroup))

        return adGroupInfo
    }

    protected abstract fun getAdGroupInfo() : T

    protected abstract fun initializeCampaignInfo(adGroupInfo: T)

    abstract fun getAdGroupInfoClass(): KClass<T>

    protected open fun createRelations(adGroupInfo: T) {
    }

    protected open fun checkAdGroupInfoConsistency(adGroupInfo: T) {
        if (adGroupInfo.isCampaignInfoInitialized()) {
            val campaign = adGroupInfo.campaignInfo.typedCampaign
            check(allowedAdGroupClasses.contains(campaign::class)) {
                "campaign class must be in whiteList, but current " + campaign::class
            }
        }
    }
}

