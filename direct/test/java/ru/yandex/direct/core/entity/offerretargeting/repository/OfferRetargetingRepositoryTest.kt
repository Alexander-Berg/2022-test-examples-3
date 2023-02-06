package ru.yandex.direct.core.entity.offerretargeting.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.data.defaultOfferRetargeting
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.AdGroupSteps
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.core.testing.steps.OfferRetargetingSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.operation.AddedModelId
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.utils.FunctionalUtils
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingRepositoryTest {
    @Autowired
    private lateinit var userSteps: UserSteps

    @Autowired
    private lateinit var campaignSteps: CampaignSteps

    @Autowired
    private lateinit var adGroupSteps: AdGroupSteps

    @Autowired
    private lateinit var offerRetargetingSteps: OfferRetargetingSteps

    @Autowired
    private lateinit var offerRetargetingRepository: OfferRetargetingRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    private lateinit var defaultUser: UserInfo
    private lateinit var activeCampaign: CampaignInfo
    private lateinit var defaultAdGroup: AdGroupInfo
    private lateinit var offerRetargeting: OfferRetargeting

    @Before
    fun before() {
        defaultUser = userSteps.createDefaultUser()
        activeCampaign = campaignSteps.createActiveCampaign(defaultUser.clientInfo)
        val campaignId = activeCampaign.campaignId
        defaultAdGroup = adGroupSteps.createAdGroup(TestGroups.defaultTextAdGroup(campaignId), activeCampaign)
        offerRetargeting = defaultOfferRetargeting
            .withCampaignId(campaignId)
            .withAdGroupId(defaultAdGroup.adGroupId)
            .withIsDeleted(false)
            .withIsSuspended(true)
    }

    @Test
    fun addOfferRetargetings_addedCorrectly() {
        val offerRetargetingsAddedModelIds = offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            defaultUser.clientInfo!!.clientId!!,
            listOf(offerRetargeting),
            setOf(defaultAdGroup.adGroupId)
        )
        val offerRetargetingsIds = offerRetargetingsAddedModelIds.map { it.id }

        val offerRetargetingsByIds = offerRetargetingRepository.getOfferRetargetingsByIds(
            defaultUser.shard,
            defaultUser.clientInfo!!.clientId!!,
            offerRetargetingsIds
        )
        val compareStrategy = compareStrategy
        assertThat(offerRetargetingsByIds[offerRetargetingsIds[0]])
            .`is`(matchedBy(beanDiffer(offerRetargeting).useCompareStrategy(compareStrategy)))
    }

    @Test
    fun addOfferRetargetings_groupHasNotDeletedOfferRetargeting_NotUpdatedExistsOfferRetargeting() {
        val addedOfferRetargetingId = offerRetargetingRepository
            .addOfferRetargetings(
                dslContextProvider.ppc(defaultUser.shard).configuration(),
                defaultUser.clientInfo!!.clientId!!,
                listOf(offerRetargeting),
                setOf(defaultAdGroup.adGroupId)
            )[0]
        val toAdd = defaultOfferRetargeting
            .withAdGroupId(defaultAdGroup.adGroupId)
            .withCampaignId(defaultAdGroup.campaignId)
        val offerRetargetingsAddedModelIds = offerRetargetingRepository
            .addOfferRetargetings(
                dslContextProvider.ppc(defaultUser.shard).configuration(),
                defaultUser.clientInfo!!.clientId!!,
                listOf(toAdd), setOf(defaultAdGroup.adGroupId)
            )
        val expectedOfferRetargetingId = AddedModelId.ofExisting(addedOfferRetargetingId.id)
        assertThat(offerRetargetingsAddedModelIds[0]).isEqualTo(expectedOfferRetargetingId)
        val ids = FunctionalUtils.mapList(offerRetargetingsAddedModelIds) { obj: AddedModelId -> obj.id }
        val offerRetargetingsByIds = offerRetargetingRepository
            .getOfferRetargetingsByIds(defaultUser.shard, defaultUser.clientInfo!!.clientId!!, ids)
        val compareStrategy = compareStrategy
        assertThat(offerRetargetingsByIds[ids[0]])
            .`is`(matchedBy(beanDiffer(offerRetargeting).useCompareStrategy(compareStrategy)))
    }

    @Test
    fun updateOfferRetargeting() {
        val updatedPrice = BigDecimal("10.00")
        val offerRetargetingIds = offerRetargetingSteps
            .addOfferRetargetingsToAdGroup(listOf(offerRetargeting), defaultAdGroup)
        val modelChanges = ModelChanges(offerRetargetingIds[0], OfferRetargeting::class.java)
        modelChanges.process(updatedPrice, OfferRetargeting.PRICE)
        offerRetargetingRepository.update(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            listOf(modelChanges.applyTo(offerRetargeting))
        )
        val offerRetargetingsByIds = offerRetargetingRepository.getOfferRetargetingsByIds(
            defaultUser.shard,
            defaultUser.clientInfo!!.clientId!!,
            offerRetargetingIds
        )
        val compareStrategy = compareStrategy
        val expectedOfferRetargeting = offerRetargeting.withPrice(updatedPrice)
        assertThat(offerRetargetingsByIds[offerRetargetingIds[0]])
            .`is`(matchedBy(beanDiffer(expectedOfferRetargeting).useCompareStrategy(compareStrategy)))
    }

    @Test
    fun addOfferRetargetings_adGroupHasDeletedOfferRetargeting_addedCorrectly() {
        val addedOfferRetargetingId = offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            defaultUser.clientInfo!!.clientId!!,
            listOf<OfferRetargeting>(offerRetargeting.withIsDeleted(true)),
            setOf(defaultAdGroup.adGroupId)
        ).single()

        val toAdd = defaultOfferRetargeting
            .withAdGroupId(offerRetargeting.adGroupId)
            .withCampaignId(offerRetargeting.campaignId)

        val offerRetargetingsAddedModelIds: List<AddedModelId> = offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            defaultUser.clientInfo!!.clientId!!,
            listOf(toAdd), setOf(defaultAdGroup.adGroupId)
        )

        assertThat(offerRetargetingsAddedModelIds[0]).isEqualTo(addedOfferRetargetingId)

        val result = offerRetargetingRepository.getOfferRetargetingsByAdGroupIds(
            defaultAdGroup.shard,
            defaultAdGroup.clientId,
            setOf(defaultAdGroup.adGroupId)
        )
        assertThat(result.size).isOne
        assertThat(result[defaultAdGroup.adGroupId]!!.isDeleted).isFalse
    }

    @Test
    fun getOfferRetargetingByGroupIds() {
        offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            defaultUser.clientInfo!!.clientId!!,
            listOf(offerRetargeting),
            setOf(defaultAdGroup.adGroupId)
        )
        val offerRetargetingsByGroupIds = offerRetargetingRepository.getOfferRetargetingsByAdGroupIds(
            defaultUser.shard,
            defaultUser.clientInfo!!.clientId!!,
            listOf(defaultAdGroup.adGroupId)
        )

        assertThat(offerRetargetingsByGroupIds[defaultAdGroup.adGroupId])
            .`is`(matchedBy(beanDiffer(offerRetargeting).useCompareStrategy(compareStrategy)))
    }

    @Test
    fun getOfferRetargetingByGroupIdsWithoutDeleted_AdGroupDoesntHaveDeleted() {
        offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            defaultUser.clientInfo!!.clientId!!,
            listOf(offerRetargeting),
            setOf(defaultAdGroup.adGroupId)
        )
        val offerRetargetingsByGroupIds = offerRetargetingRepository.getOfferRetargetingsByAdGroupIds(
            defaultUser.shard,
            defaultUser.clientInfo!!.clientId!!,
            listOf(defaultAdGroup.adGroupId),
            false
        )

        assertThat(offerRetargetingsByGroupIds[defaultAdGroup.adGroupId])
            .`is`(matchedBy(beanDiffer(offerRetargeting).useCompareStrategy(compareStrategy)))
    }

    @Test
    fun getOfferRetargetingByGroupIdsWithDeleted_AdGroupDoesntHaveDeleted() {
        offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            defaultUser.clientInfo!!.clientId!!,
            listOf(offerRetargeting),
            setOf(defaultAdGroup.adGroupId)
        )

        val offerRetargetingsByGroupIds = offerRetargetingRepository.getOfferRetargetingsByAdGroupIds(
            defaultUser.shard,
            defaultUser.clientInfo!!.clientId!!,
            listOf(defaultAdGroup.adGroupId),
            true
        )

        assertThat(offerRetargetingsByGroupIds[defaultAdGroup.adGroupId])
            .`is`(matchedBy(beanDiffer(offerRetargeting).useCompareStrategy(compareStrategy)))
    }

    @Test
    fun getOfferRetargetingByGroupIdsWithoutDeleted_AdGroupHasDeleted() {
        offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            defaultUser.clientInfo!!.clientId!!,
            listOf(offerRetargeting.withIsDeleted(true)),
            setOf(defaultAdGroup.adGroupId)
        )
        val offerRetargetingsByGroupIds = offerRetargetingRepository.getOfferRetargetingsByAdGroupIds(
            defaultUser.shard,
            defaultUser.clientInfo!!.clientId!!,
            listOf(defaultAdGroup.adGroupId),
            false
        )
        assertThat(offerRetargetingsByGroupIds.size).isEqualTo(0)
    }

    @Test
    fun getOfferRetargetingByGroupIdsWithDeleted_AdGroupHasDeleted() {
        offerRetargetingRepository.addOfferRetargetings(
            dslContextProvider.ppc(defaultUser.shard).configuration(),
            defaultUser.clientInfo!!.clientId!!,
            listOf(offerRetargeting.withIsDeleted(true)),
            setOf(defaultAdGroup.adGroupId)
        )
        val offerRetargetingsByGroupIds = offerRetargetingRepository.getOfferRetargetingsByAdGroupIds(
            defaultUser.shard,
            defaultUser.clientInfo!!.clientId!!,
            listOf(defaultAdGroup.adGroupId),
            true
        )

        assertThat(offerRetargetingsByGroupIds[defaultAdGroup.adGroupId])
            .`is`(matchedBy(beanDiffer(offerRetargeting).useCompareStrategy(compareStrategy)))
    }

    private val compareStrategy: DefaultCompareStrategy
        get() = DefaultCompareStrategies.allFieldsExcept(
            BeanFieldPath.newPath(OfferRetargeting.ID.name()),
            BeanFieldPath.newPath(OfferRetargeting.STATUS_BS_SYNCED.name()),
            BeanFieldPath.newPath(OfferRetargeting.LAST_CHANGE_TIME.name())
        )
}
