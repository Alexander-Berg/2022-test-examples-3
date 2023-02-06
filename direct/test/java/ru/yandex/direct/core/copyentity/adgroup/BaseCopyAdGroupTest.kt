package ru.yandex.direct.core.copyentity.adgroup

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.core.copyentity.CopyConfigBuilder
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils
import ru.yandex.direct.core.copyentity.CopyOperation
import ru.yandex.direct.core.copyentity.CopyOperationFactory
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.steps.Steps

abstract class BaseCopyAdGroupTest : AbstractSpringTest() {

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var copyOperationFactory: CopyOperationFactory

    @Autowired
    protected lateinit var adGroupService: AdGroupService

    protected lateinit var client: ClientInfo

    protected fun sameCampaignAdGroupCopyOperation(
        adGroup: AdGroupInfo,
        operatorUid: Long = client.uid,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
    ): CopyOperation<AdGroup, Long> {
        return copyOperationFactory.build(
            adGroup.shard, adGroup.clientInfo.client!!,
            adGroup.shard, adGroup.clientInfo.client!!,
            operatorUid,
            AdGroup::class.java,
            listOf(adGroup.adGroupId),
            flags,
        )
    }

    protected fun otherCampaignAdGroupCopyOperation(
        adGroup: AdGroupInfo,
        targetCampaign: CampaignInfo<*>,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
    ): CopyOperation<AdGroup, Long> {
        return copyOperationFactory.build(
            CopyConfigBuilder(
                adGroup.clientInfo.clientId!!,
                targetCampaign.clientId!!,
                client.uid,
                AdGroup::class.java,
                listOf(adGroup.adGroupId),
            )
                .withParentIdMapping(BaseCampaign::class.java, adGroup.campaignId, targetCampaign.campaignId)
                .withFlags(flags)
                .build()
        )
    }

    protected fun copyValidAdGroup(copyOperation: CopyOperation<AdGroup, Long>, allowWarnings: Boolean = false) =
        CopyEntityTestUtils.copyValidEntity(AdGroup::class.java, copyOperation, allowWarnings)

    protected fun <T : AdGroup> copyValidAdGroup(
        adGroup: AdGroupInfo,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
        allowWarnings: Boolean = false,
    ): T {
        val copyOperation = sameCampaignAdGroupCopyOperation(adGroup = adGroup, flags = flags)
        val adGroupId = copyValidAdGroup(copyOperation, allowWarnings = allowWarnings).first()
        return getAdGroup(adGroupId)
    }

    protected fun <T : AdGroup> getAdGroup(adGroupId: Long): T {
        @Suppress("UNCHECKED_CAST")
        return adGroupService.getAdGroup(adGroupId) as T
    }
}
