package ru.yandex.direct.core.copyentity.campaign

import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.spring.AbstractSpringTest
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils
import ru.yandex.direct.core.copyentity.CopyOperation
import ru.yandex.direct.core.copyentity.CopyOperationFactory
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo as TypedCampaignInfo

abstract class BaseCopyCampaignTest : AbstractSpringTest() {

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var copyOperationFactory: CopyOperationFactory

    @Autowired
    protected lateinit var campaignTypedRepository: CampaignTypedRepository

    /**
     * Клиент, главный представитель которого используется по умолчанию, как оператор для копирования внутри клиента.
     * Исходный клиент для копирования (по умолчанию) берется из самой кампании.
     */
    protected lateinit var client: ClientInfo

    /**
     * Целевой клиент по умолчанию
     */
    protected lateinit var targetClient: ClientInfo

    /**
     * Суперпользователь
     */
    protected lateinit var superClient: ClientInfo

    protected fun targetClientAllShards() = arrayOf(ClientSteps.DEFAULT_SHARD, ClientSteps.ANOTHER_SHARD)

    protected fun sameClientCampaignCopyOperation(
        campaign: CampaignInfo,
        uid: Long = client.uid,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
    ): CopyOperation<BaseCampaign, Long> {
        return copyOperationFactory.build(
            campaign.shard, campaign.clientInfo.client!!,
            campaign.shard, campaign.clientInfo.client!!,
            uid,
            BaseCampaign::class.java,
            listOf(campaign.campaignId),
            flags,
        )
    }

    protected fun sameClientCampaignCopyOperation(
        campaigns: List<CampaignInfo>,
        client: ClientInfo = this.client,
        uid: Long = this.client.uid,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
    ): CopyOperation<BaseCampaign, Long> {
        return copyOperationFactory.build(
            client.shard, client.client!!,
            client.shard, client.client!!,
            uid,
            BaseCampaign::class.java,
            campaigns.map { it.campaignId },
            flags,
        )
    }

    protected fun betweenClientsCampaignCopyOperation(
        campaign: CampaignInfo,
        targetClient: ClientInfo = this.targetClient,
        operatorUid: Long = superClient.uid,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
    ): CopyOperation<BaseCampaign, Long> {
        return copyOperationFactory.build(
            campaign.shard, campaign.clientInfo.client!!,
            targetClient.shard, targetClient.client!!,
            operatorUid,
            BaseCampaign::class.java,
            listOf(campaign.campaignId),
            flags,
        )
    }

    protected fun betweenClientsCampaignCopyOperation(
        campaigns: List<CampaignInfo>,
        sourceClient: ClientInfo = this.client,
        targetClient: ClientInfo = this.targetClient,
        operatorUid: Long = superClient.uid,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
    ): CopyOperation<BaseCampaign, Long> {
        return copyOperationFactory.build(
            sourceClient.shard, sourceClient.client!!,
            targetClient.shard, targetClient.client!!,
            operatorUid,
            BaseCampaign::class.java,
            campaigns.map { it.campaignId },
            flags,
        )
    }

    protected fun copyValidCampaigns(
        copyOperation: CopyOperation<BaseCampaign, Long>,
        allowWarnings: Boolean = false,
    ): List<Long> = CopyEntityTestUtils.copyValidEntity(BaseCampaign::class.java, copyOperation, allowWarnings)

    protected fun <T : CommonCampaign> copyValidCampaign(
        campaign: TypedCampaignInfo<T>,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
        allowWarnings: Boolean = false,
    ): T {
        val copyOperation = sameClientCampaignCopyOperation(campaign = campaign, flags = flags)
        return copyValidCampaign(copyOperation, allowWarnings)
    }

    protected fun <T : CommonCampaign> copyValidCampaignBetweenClients(
        campaign: TypedCampaignInfo<T>,
        targetClient: ClientInfo = this.targetClient,
        operatorUid: Long = this.superClient.uid,
        flags: CopyCampaignFlags = CopyCampaignFlags(),
        allowWarnings: Boolean = false,
    ): T {
        val copyOperation = betweenClientsCampaignCopyOperation(campaign, targetClient, operatorUid, flags)
        return copyValidCampaign(copyOperation, allowWarnings, targetClient.shard)
    }

    protected fun <T : CommonCampaign> copyValidCampaign(
        copyOperation: CopyOperation<BaseCampaign, Long>,
        allowWarnings: Boolean = false,
        shard: Int = client.shard,
    ): T {
        val campaignId = copyValidCampaigns(copyOperation, allowWarnings).first()
        return getCampaign(campaignId, shard)
    }

    protected fun <T : CommonCampaign> getCampaign(campaignId: Long, shard: Int = client.shard): T {
        @Suppress("UNCHECKED_CAST")
        return campaignTypedRepository.getTypedCampaigns(shard, listOf(campaignId)).first() as T
    }

    protected fun <T : CommonCampaign> getCampaigns(campaignIds: List<Long>, shard: Int = client.shard): List<T> {
        @Suppress("UNCHECKED_CAST")
        return campaignTypedRepository.getTypedCampaigns(shard, campaignIds) as List<T>
    }
}
