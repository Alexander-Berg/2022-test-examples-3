package ru.yandex.direct.core.entity.campaign.service.operation

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import one.util.streamex.StreamEx
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueueItem
import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueueObjType
import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueuePriority
import ru.yandex.direct.core.entity.balance.model.BalanceNotificationInfo
import ru.yandex.direct.core.entity.balance.repository.BalanceInfoQueueRepository
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.client.repository.ClientRepository
import ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes
import ru.yandex.direct.core.entity.notification.container.NotificationType
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParams
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsParamsItem
import ru.yandex.direct.core.entity.turbolanding.container.UpdateCounterGrantsResult
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestClientRepository
import ru.yandex.direct.core.testing.repository.TestTurboLandingRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.IntApiClientStub
import ru.yandex.direct.dbqueue.model.DbQueueJob
import ru.yandex.direct.dbqueue.repository.DbQueueRepository
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.hasErrorOrWarning
import ru.yandex.direct.validation.result.PathHelper

/**
 * Тест на проверку CampaignOptions#requireServicing
 */
@CoreTest
@RunWith(JUnitParamsRunner::class)
class AddCampaignRequireServicingTest {

    private val COUNTER_ID = 5L

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignRepository: CampaignRepository

    @Autowired
    private lateinit var clientRepository: ClientRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var testClientRepository: TestClientRepository

    @Autowired
    private lateinit var testTurboLandingRepository: TestTurboLandingRepository

    @Autowired
    private lateinit var dbQueueRepository: DbQueueRepository

    @Autowired
    private lateinit var balanceInfoQueueRepository: BalanceInfoQueueRepository

    @Autowired
    protected lateinit var campaignOperationService: CampaignOperationService

    @Autowired
    private lateinit var intApiClient: IntApiClientStub

    private lateinit var managerInfo: UserInfo
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var shard: Int = 0
    private lateinit var turboLanding: TurboLanding

    fun testData() = listOf( // hasIdmPrimaryManager,requireServicing,expectedServiced,expectedWarning
        listOf(true, false, false, false),
        listOf(true, true, true, false),
        listOf(true, null, true, false),
        listOf(false, true, false, true),
        listOf(false, false, false, false),
        listOf(false, null, false, false)
    )

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT)
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard

        turboLanding = steps.turboLandingSteps().createDefaultTurboLanding(clientId)
        testTurboLandingRepository.addTurbolandingMetricaCounters(shard, turboLanding.id, listOf(COUNTER_ID))

        steps.dbQueueSteps().registerJobType(DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB)
        steps.dbQueueSteps().clearQueue(DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB)
    }

    @After
    fun after() {
        intApiClient.clear()
    }

    fun createManager() {
        managerInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).chiefUserInfo!!
        campaignRepository.setManagerForAllClientCampaigns(shard, clientId, managerInfo.uid)
        updatePrimaryManagerUid(shard, clientId, managerInfo.uid)
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("hasIdmPrimaryManager {0} requireServicing {1}")
    fun test(
        hasIdmPrimaryManager: Boolean,
        requireServicing: Boolean?,
        expectedServiced: Boolean,
        expectedWarning: Boolean
    ) {
        if (hasIdmPrimaryManager) {
            createManager();
        }

        val addCampaignOptions = CampaignOptions.Builder()
            .withSendAutoServicingMailNotification(true)
        if (requireServicing != null) {
            addCampaignOptions
                .withRequireServicing(listOf(requireServicing))
        }

        val campaign = defaultTextCampaign()

        val addOperation = campaignOperationService.createRestrictedCampaignAddOperation(
            listOf(campaign),
            clientInfo.uid,
            UidAndClientId.of(clientInfo.uid, clientInfo.clientId),
            addCampaignOptions.build(),
        )

        val result = addOperation.prepareAndApply()
        assertThat(result.validationResult.flattenErrors()).isEmpty()

        if (expectedWarning) {
            Assertions.assertThat(result.validationResult).hasErrorOrWarning(
                PathHelper.path(PathHelper.index(0)),
                CampaignDefectIds.Gen.REQUIRE_SERVICING_WILL_BE_IGNORED
            )
        }

        val balanceNotificationNewCamp = java.util.List.of(
            BalanceNotificationInfo()
                .withCidOrUid(clientInfo.uid)
                .withObjType(BalanceInfoQueueObjType.UID)
                .withPriority(BalanceInfoQueuePriority.USER_ON_SAVING_NEW_CAMPAIGN)
        )

        val balanceInfoQueueItemsNewCamp: List<BalanceInfoQueueItem> = balanceInfoQueueRepository
            .getExistingRecordsInWaitStatus(dslContextProvider.ppc(shard), balanceNotificationNewCamp)

        val uidsBalanceNewCamp = StreamEx.of(balanceInfoQueueItemsNewCamp)
            .map { obj: BalanceInfoQueueItem -> obj.cidOrUid }
            .toSet()

        assertThat(uidsBalanceNewCamp)
            .containsOnly(clientInfo.uid)
            .`as`("UID-ы USER_ON_SAVING_NEW_CAMPAIGN в очереди на переотправку в баланс")

        val expectedNotification = IntApiClientStub.TestNotification()
        expectedNotification.notificationType = NotificationType.AUTO_SERVICING.notificationName
        expectedNotification.data = mapOf(
            "manager_uid" to if (hasIdmPrimaryManager) managerInfo.uid.toString() else "",
            "client_fio" to clientInfo.chiefUserInfo!!.user!!.fio,
            "fio" to clientInfo.chiefUserInfo!!.user!!.fio,
            "camp_name" to campaign.name,
            "campaign_name" to campaign.name,
            "manager_fio" to if (hasIdmPrimaryManager) managerInfo.user!!.fio else "",
            "client_login" to clientInfo.login,
            "user_login" to clientInfo.login,
            "cid" to result.result[0].result.toString(),
            "campaign_id" to result.result[0].result.toString(),
            "campaign_type" to campaign.type.name.lowercase(),
            "client_email" to clientInfo.chiefUserInfo!!.user!!.email,
            "client_id" to clientId.toString(),
            "client_phone" to clientInfo.chiefUserInfo!!.user!!.phone,
        )
        expectedNotification.options = mapOf()

        val actualCampaign = getCampaignFromResult(result)
        val actualNotifications = intApiClient.notifications
        if (expectedServiced) {
            val managerClients = testClientRepository.getBindedClientsToManager(shard, managerInfo.uid)

            val expectedJob = DbQueueJob<UpdateCounterGrantsParams, UpdateCounterGrantsResult>()
                .withUid(clientInfo.uid)
                .withClientId(clientId)
                .withArgs(
                    UpdateCounterGrantsParams()
                        .withItems(listOf(
                            UpdateCounterGrantsParamsItem()
                                .withCounterId(COUNTER_ID)
                        )))

            val actualJob = dbQueueRepository.grabSingleJob(shard, DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB)
            val actualJobUserIds = actualJob!!.args.items[0].userIds

            val balanceNotificationManagerChanged = java.util.List.of(
                BalanceNotificationInfo()
                    .withCidOrUid(clientInfo.uid)
                    .withObjType(BalanceInfoQueueObjType.UID)
                    .withPriority(BalanceInfoQueuePriority.PRIORITY_CAMP_ON_MANAGER_CHANGED)
            )

            val balanceInfoQueueItemsManagerChanged: List<BalanceInfoQueueItem> = balanceInfoQueueRepository
                .getExistingRecordsInWaitStatus(dslContextProvider.ppc(shard), balanceNotificationManagerChanged)

            val uidsBalanceManagerChanged = StreamEx.of(balanceInfoQueueItemsManagerChanged)
                .map { obj: BalanceInfoQueueItem -> obj.cidOrUid }
                .toSet()

            SoftAssertions.assertSoftly { soft: SoftAssertions ->
                soft.assertThat(actualCampaign.managerUid).isEqualTo(managerInfo.uid)
                soft.assertThat(managerClients).containsExactly(clientInfo.clientId!!.asLong())
                soft.assertThat(actualJob).`as`("update counter grants job")
                    .`is`(matchedBy(beanDiffer(expectedJob)
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
                soft.assertThat(actualJobUserIds).`as`("uids in job")
                    .containsExactlyInAnyOrder(managerInfo.uid, clientInfo.uid)
                soft.assertThat(uidsBalanceManagerChanged)
                    .`as`("UID-ы PRIORITY_CAMP_ON_MANAGER_CHANGED в очереди на переотправку в баланс")
                    .containsOnly(clientInfo.uid)
                soft.assertThat(actualNotifications.size).isEqualTo(1)
                soft.assertThat(actualNotifications[0]).`as`("notification")
                    .`is`(matchedBy(beanDiffer(expectedNotification)))
            }
        } else {
            assertNull(actualCampaign.managerUid)

            val actualJob = dbQueueRepository.grabSingleJob(shard, DbQueueJobTypes.UPDATE_COUNTER_GRANTS_JOB)
            assertNull(actualJob)

            assertThat(actualNotifications.size).isEqualTo(0)
        }
    }

    private fun getCampaignFromResult(result: MassResult<Long>): TextCampaign =
        campaignTypedRepository.getTypedCampaigns(shard, listOf(result[0].result)).get(0) as TextCampaign;

    private fun updatePrimaryManagerUid(
        shard: Int,
        clientId: ClientId,
        managerUid: Long) {
        val modelChanges = ModelChanges(clientId.asLong(), Client::class.java)
        modelChanges.process(managerUid, Client.PRIMARY_MANAGER_UID)
        modelChanges.process(true, Client.IS_IDM_PRIMARY_MANAGER)
        val client: Client = clientRepository.get(shard, setOf(clientId)).get(0)
        val appliedChanges = modelChanges.applyTo(client)
        clientRepository.update(shard, setOf(appliedChanges))
    }

}
