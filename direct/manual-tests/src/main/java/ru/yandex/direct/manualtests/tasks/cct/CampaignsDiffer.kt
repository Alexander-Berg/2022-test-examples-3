package ru.yandex.direct.manualtests.tasks.cct

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.yandex.direct.core.copyentity.CopyOperation
import ru.yandex.direct.core.copyentity.CopyOperationContainer
import ru.yandex.direct.core.copyentity.CopyOperationFactory
import ru.yandex.direct.core.copyentity.CopyResult
import ru.yandex.direct.core.copyentity.EntityContext
import ru.yandex.direct.core.copyentity.EntityLoadService
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.entity.campoperationqueue.CampOperationQueueRepository
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.client.service.ClientService
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.i18n.I18NBundle
import ru.yandex.direct.intapi.client.IntApiClient
import ru.yandex.direct.intapi.client.model.response.CampaignsCopyResponse
import ru.yandex.direct.manualtests.app.TestTasksRunner
import ru.yandex.direct.operation.Applicability

@Component
class CampaignsDiffer(
    private val shardHelper: ShardHelper,
    private val clientService: ClientService,
    private val campaignService: CampaignService,
    private val entityLoadService: EntityLoadService,
    private val copyOperationFactory: CopyOperationFactory,
    private val campOperationQueueRepository: CampOperationQueueRepository,
    private val campaignEntityCompareService: CampaignEntityCompareService,
    private val intApiClient: IntApiClient,
) : Runnable {

    private val logger = LoggerFactory.getLogger(CampaignsDiffer::class.java)

    override fun run() {
        val cids: List<Long> = listOf(
        )

        val totalDiff: GraphDiff? = cids
            .map { cid -> comparePerl(cid) }
            .reduce { left: GraphDiff?, right: GraphDiff? ->
                if (left != null && right != null) {
                    left.apply { merge(right) }
                }

                left ?: right
            }

        println(campaignEntityCompareService.buildDiff(totalDiff))
    }

    private fun compareJava(originalCid: Long): GraphDiff? {
        val clientId = ClientId.fromLong(shardHelper.getClientIdByCampaignId(originalCid))
        val shard: Int = shardHelper.getShardByClientId(clientId)
        val client = clientService.getClient(clientId)
            ?: throw IllegalStateException("No client with id $clientId")

        val copyResult: CopyResult<Long> = copyJava(shard, client, originalCid) ?: return null

        val copyContainer = copyResult.entityContext.copyContainer

        val leftContext: EntityContext = loadContext(copyContainer, originalCid)

        val rightCid: Long = copyResult.getEntityMapping(BaseCampaign::class.java)[originalCid] as Long
        val rightContext: EntityContext = loadContext(copyContainer, rightCid)

        return campaignEntityCompareService.compareCampaigns(leftContext, rightContext)
            .also { deleteCampaigns(client, listOf(rightCid)) }
    }

    private fun comparePerl(originalCid: Long): GraphDiff? {
        val clientId = ClientId.fromLong(shardHelper.getClientIdByCampaignId(originalCid))
        val shard: Int = shardHelper.getShardByClientId(clientId)
        val client = clientService.getClient(clientId)
            ?: throw IllegalStateException("No client with id $clientId")

        val perlCopyResult: CampaignsCopyResponse = copyPerl(shard, client, originalCid) ?: return null
        val javaCopyResult: CopyResult<Long> = copyJava(shard, client, originalCid) ?: return null

        val copyContainer = javaCopyResult.entityContext.copyContainer

        val leftCid: Long = perlCopyResult.cid!!
        val leftContext: EntityContext = loadContext(copyContainer, leftCid)

        val rightCid: Long = javaCopyResult.getEntityMapping(BaseCampaign::class.java)[originalCid] as Long
        val rightContext: EntityContext = loadContext(copyContainer, rightCid)

        return campaignEntityCompareService.compareCampaigns(leftContext, rightContext)
            .also { deleteCampaigns(client, listOf(leftCid, rightCid)) }
    }

    private fun copyJava(shard: Int, client: Client, originalCid: Long): CopyResult<Long>? {
        logger.info("Copying campaign $originalCid via java")
        val chiefUid: Long = client.agencyUserId ?: client.chiefUid
        val operation: CopyOperation<BaseCampaign, Long> = copyOperationFactory.build(
            shard, client, shard, client,
            chiefUid, BaseCampaign::class.java,
            listOf(originalCid), CopyCampaignFlags(),
        )

        val copyResult: CopyResult<Long> = operation.copy()

        copyResult.logFailedResultsForMonitoring()
        if (!copyResult.massResult.isSuccessful) {
            logger.error("Failed to copy campaign $originalCid via java")
            return null
        }

        val resultCid: Long? = copyResult.getEntityMapping(BaseCampaign::class.java)[originalCid] as Long?
        if (resultCid == null) {
            logger.error("Failed to copy campaign $originalCid via java")
            return null
        }

        logger.info("Successfully copied campaign $originalCid into $resultCid")

        return copyResult
    }

    private fun copyPerl(shard: Int, client: Client, originalCid: Long): CampaignsCopyResponse? {
        logger.info("Copying campaign $originalCid via perl")

        campOperationQueueRepository.deleteCampaignFromQueue(shard, listOf(originalCid))
        campOperationQueueRepository.deleteCampaignFromCopyQueue(shard, listOf(originalCid))

        val perlCopyResult: CampaignsCopyResponse = intApiClient.copyCampaigns(
            client.agencyUserId ?: client.chiefUid,
            client.id,
            client.id,
            listOf(originalCid),
            I18NBundle.RU,
            true,
        )

        if (!perlCopyResult.success) {
            logger.error("Failed to copy campaign $originalCid via perl: ${perlCopyResult.error}")
            return null
        }

        logger.info("Successfully copied campaign $originalCid into ${perlCopyResult.cid}")

        return perlCopyResult
    }

    private fun loadContext(copyContainer: CopyOperationContainer, cid: Long): EntityContext {
        val entityContext = EntityContext(copyContainer)
        entityLoadService.loadGraph(BaseCampaign::class.java, listOf(cid), entityContext)
        return entityContext
    }

    private fun deleteCampaigns(client: Client, campaignIds: List<Long>) {
        campaignService.deleteCampaigns(
            campaignIds,
            client.chiefUid,
            ClientId.fromLong(client.id),
            Applicability.PARTIAL,
        )
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            TestTasksRunner.runTask(CampaignsDiffer::class.java, args)
        }
    }
}
