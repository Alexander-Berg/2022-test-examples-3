package ru.yandex.direct.core.entity.offerretargeting.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget
import ru.yandex.direct.core.entity.campaign.model.DbStrategy
import ru.yandex.direct.core.entity.offerretargeting.container.OfferRetargetingModification
import ru.yandex.direct.core.entity.offerretargeting.container.OfferRetargetingUpdateContainer
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting.LAST_CHANGE_TIME
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingMapping.offerRetargetingToCoreModelChanges
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository
import ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.core.testing.steps.OfferRetargetingSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.currency.Currency
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy

abstract class OfferRetargetingOperationBaseTest {
    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var userSteps: UserSteps

    @Autowired
    private lateinit var campaignSteps: CampaignSteps

    @Autowired
    lateinit var adGroupSteps: AdGroupSteps

    @Autowired
    lateinit var offerRetargetingService: OfferRetargetingService

    @Autowired
    lateinit var offerRetargetingRepository: OfferRetargetingRepository

    @Autowired
    private lateinit var offerRetargetingSteps: OfferRetargetingSteps

    private lateinit var clientInfo: ClientInfo
    lateinit var defaultUser: UserInfo

    private lateinit var adGroupInfos: MutableList<AdGroupInfo>
    private lateinit var campaignInfos: MutableList<Pair<Long, Campaign>>
    private lateinit var offerRetargetings: MutableList<OfferRetargeting>

    @Before
    open fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        defaultUser = clientInfo.chiefUserInfo!!
        adGroupInfos = mutableListOf()
        campaignInfos = mutableListOf()
        offerRetargetings = mutableListOf()
        addCampaign()
    }

    val campaignsByIds: Map<Long, Campaign>
        get() = campaignInfos.toMap()

    val adGroupsById: Map<Long, AdGroup>
        get() = adGroupInfos.associate { it.adGroupId to it.adGroup }

    val adGroupIds: Set<Long>
        get() = adGroupInfos.mapTo(mutableSetOf()) { it.adGroupId }

    val offerRetargetingsByIds: Map<Long, OfferRetargeting>
        get() = offerRetargetings.associateBy { it.id }

    val adGroupIdsByOfferRetargetingIds: Map<Long, Long>
        get() = offerRetargetings.associate { it.id to it.adGroupId }

    val campaignIdsByAdGroupIds: Map<Long, Long>
        get() = adGroupInfos.associate { it.adGroupId to it.campaignId }

    fun addCampaign(): Campaign {
        val campaignInfo = campaignSteps.createActiveCampaign(clientInfo)
        val strategy = DbStrategy()
        strategy.autobudget = CampaignsAutobudget.YES
        val campaign = Campaign()
            .withId(campaignInfo.campaignId)
            .withType(CampaignType.TEXT)
            .withAutobudget(strategy.isAutoBudget)
            .withStrategy(strategy)
            .withCurrency(CurrencyCode.RUB)

        adGroupInfos += adGroupSteps.createAdGroup(defaultAdGroup(campaignInfo.campaignId), campaignInfo)
        campaignInfos += campaignInfo.campaignId to campaign

        return campaign
    }

    fun getFullAddOperation(offerRetargeting: OfferRetargeting): OfferRetargetingAddOperation {
        return offerRetargetingService.createFullAddOperation(
            listOf(offerRetargeting),
            clientId,
            operatorUid,
            currency
        )

    }

    fun getFullUpdateOperationWithContainer(offerRetargetingChanges: ModelChanges<OfferRetargeting>): OfferRetargetingUpdateOperation {
        val container =
            OfferRetargetingUpdateContainer(
                operatorUid,
                clientId,
                clientUid,
                currency,
                campaignsByIds,
                campaignIdsByAdGroupIds,
                adGroupIdsByOfferRetargetingIds,
                offerRetargetingsByIds
            )
        return offerRetargetingService.createFullUpdateOperation(listOf(offerRetargetingChanges), container)
    }

    fun getFullUpdateOperation(offerRetargetingChanges: ModelChanges<OfferRetargeting>): OfferRetargetingUpdateOperation =
        offerRetargetingService.createFullUpdateOperation(
            listOf(offerRetargetingChanges),
            clientId,
            clientUid,
            currency,
            operatorUid
        )

    fun getFullDeleteOperation(ids: List<Long>): OfferRetargetingDeleteOperation {
        return offerRetargetingService.createFullDeleteOperation(
            clientId, operatorUid, ids, offerRetargetingsByIds
        )
    }

    fun getFullModifyOperation(offerRetargetingModification: OfferRetargetingModification): OfferRetargetingModifyOperation {
        return offerRetargetingService.createFullModifyOperation(
            clientId, clientUid, operatorUid, offerRetargetingModification
        )
    }

    protected open fun defaultAdGroup(campaignId: Long): AdGroup = defaultTextAdGroup(campaignId)

    open val defaultOfferRetargeting: OfferRetargeting
        get() = ru.yandex.direct.core.testing.data.defaultOfferRetargeting

    fun OfferRetargeting.withAdGroupInfo(adGroupIndex: Int): OfferRetargeting = apply {
        adGroupId = adGroupInfos[adGroupIndex].adGroupId
        campaignId = adGroupInfos[adGroupIndex].campaignId
    }

    fun addOfferRetargetingToAdGroup(
        adGroupIndex: Int,
        offerRetargetingToAdd: OfferRetargeting = defaultOfferRetargeting
    ): OfferRetargeting {
        val savedOfferRetargeting = offerRetargetingSteps.addOfferRetargetingToAdGroup(
            offerRetargetingToAdd.withAdGroupInfo(adGroupIndex), adGroupInfos[adGroupIndex]
        )

        offerRetargetings += savedOfferRetargeting
        return savedOfferRetargeting
    }

    fun addDefaultOfferRetargetingToAdGroupForUpdate(
        adGroupIndex: Int,
        changesFunction: (OfferRetargeting) -> OfferRetargeting,
    ): ModelChanges<OfferRetargeting> {
        val offerRetargeting = defaultOfferRetargeting.withAdGroupInfo(adGroupIndex)
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(adGroupIndex)

        val changedOfferRetargeting = changesFunction(offerRetargeting).withId(savedOfferRetargeting.id)
        return offerRetargetingToCoreModelChanges(changedOfferRetargeting)
    }

    val defaultAdGroupId: Long
        get() = adGroupInfos[0].adGroupId

    val activeCampaignId: Long
        get() = campaignInfos[0].first

    val shard: Int
        get() = defaultUser.shard

    val clientId: ClientId
        get() = clientInfo.clientId!!

    val operatorUid: Long
        get() = defaultUser.uid

    val clientUid: Long
        get() = defaultUser.clientInfo!!.client!!.chiefUid

    val currency: Currency
        get() = defaultUser.clientInfo!!.client!!.workCurrency.currency

    fun checkOfferRetargetingByIdIsEqualTo(id: Long, expectedOfferRetargeting: OfferRetargeting) {
        val actualOfferRetargeting =
            offerRetargetingRepository.getOfferRetargetingsByIds(shard, clientId, listOf(id))[id]

        val compareStrategy = allFieldsExcept(
            newPath(LAST_CHANGE_TIME.name()),
        )
        println(expectedOfferRetargeting)
        assertThat(actualOfferRetargeting)
            .isNotNull
            .`is`(matchedBy(beanDiffer(expectedOfferRetargeting.withId(id)).useCompareStrategy(compareStrategy)))
    }

    fun checkOfferRetargetingByIdIsDeleted(id: Long) {
        val actualOfferRetargeting =
            offerRetargetingRepository.getOfferRetargetingsByIds(shard, clientId, listOf(id))[id]

        assertThat(actualOfferRetargeting)
            .withFailMessage { "OfferRetargeting with id $id haven't been deleted" }
            .isNull()
    }
}
