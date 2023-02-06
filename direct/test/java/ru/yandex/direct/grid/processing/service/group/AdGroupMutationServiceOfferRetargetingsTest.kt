package ru.yandex.direct.grid.processing.service.group

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.util.BigDecimalComparator.BIG_DECIMAL_COMPARATOR
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.repository.OfferRetargetingRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName.ENABLE_OFFER_RETARGETINGS_IN_TEXT_AD_GROUP
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroup
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupOfferRetargetingItem
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupRelevanceMatchItem
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.regions.Region.KAZAKHSTAN_REGION_ID
import java.math.BigDecimal

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGroupMutationServiceOfferRetargetingsTest {
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    lateinit var processor: GridGraphQLProcessor

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var mutationService: AdGroupMutationService

    @Autowired
    lateinit var offerRetargetingRepository: OfferRetargetingRepository

    @Autowired
    lateinit var campaignRepository: CampaignRepository
    private lateinit var clientInfo: ClientInfo
    private lateinit var campaignInfo: CampaignInfo
    private lateinit var user: User
    private var shard: Int = 0
    private var operatorUid: Long = 0L

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo)
        user = clientInfo.chiefUserInfo!!.user!!
        operatorUid = user.uid
        steps.featureSteps().addClientFeature(clientInfo.clientId!!, ENABLE_OFFER_RETARGETINGS_IN_TEXT_AD_GROUP, true)
        shard = clientInfo.shard
    }

    @Test
    fun checkUpdateAdGroupOfferRetargeting_add_withDisabledFeature() {
        steps.featureSteps().addClientFeature(clientInfo.clientId!!, ENABLE_OFFER_RETARGETINGS_IN_TEXT_AD_GROUP, false)
        val gdAddTextAdGroup = gdAddTextAdGroup(true)

        val result = runCatching {
            mutationService
                .addTextAdGroups(clientInfo.clientId!!, operatorUid, operatorUid, gdAddTextAdGroup)
        }
        assertThat(result.isFailure).isTrue
    }

    @Test
    fun checkAddAdGroupOfferRetargeting_withSwitchedOn() {
        val request = gdAddTextAdGroup(true)

        val payload = mutationService
            .addTextAdGroups(clientInfo.clientId!!, operatorUid, operatorUid, request)

        assertThat(payload.addedAdGroupItems).hasSize(1)
        val adGroupId = payload.addedAdGroupItems.single().adGroupId

        val expectedOfferRetargeting = OfferRetargeting()
            .withAdGroupId(adGroupId)
            .withCampaignId(campaignInfo.campaignId)
            .withIsDeleted(false)
            .withIsSuspended(false)
            .withStatusBsSynced(StatusBsSynced.NO)

        checkNewOfferRetargeting(adGroupId, expectedOfferRetargeting)
    }

    @Test
    fun checkAddAdGroupOfferRetargeting_withSwitchedOff() {
        val request = gdAddTextAdGroup(false)

        val payload = mutationService
            .addTextAdGroups(clientInfo.clientId!!, operatorUid, operatorUid, request)

        assertThat(payload.addedAdGroupItems).hasSize(1)
        val adGroupId = payload.addedAdGroupItems.single().adGroupId

        checkOfferRetargetingIsDeleted(adGroupId)
    }

    @Test
    fun checkAddAdGroupOfferRetargeting_addWithRelevanceMatch() {
        val gdAddTextAdGroup = gdAddTextAdGroup(true)
        gdAddTextAdGroup.addItems = gdAddTextAdGroup.addItems.map {
            it.withRelevanceMatch(
                GdUpdateAdGroupRelevanceMatchItem()
                    .withIsActive(true)
            )
        }
        val result = runCatching {
            mutationService
                .addTextAdGroups(clientInfo.clientId!!, operatorUid, operatorUid, gdAddTextAdGroup)
        }

        assertThat(result.isFailure).isTrue
    }

    private fun gdAddTextAdGroup(isActive: Boolean) =
        GdAddTextAdGroup()
            .withAddItems(listOf(createDefaultAddTextAdGroupItem(campaignInfo, isActive)))

    private fun createDefaultAddTextAdGroupItem(campaignInfo: CampaignInfo, isActive: Boolean) =
        GdAddTextAdGroupItem()
            .withName(AD_GROUP_NAME)
            .withCampaignId(campaignInfo.campaignId)
            .withAdGroupMinusKeywords(emptyList())
            .withRegionIds(listOf(KAZAKHSTAN_REGION_ID.toInt()))
            .withBidModifiers(GdUpdateBidModifiers())
            .withLibraryMinusKeywordsIds(emptyList())
            .withOfferRetargeting(GdUpdateAdGroupOfferRetargetingItem().withIsActive(isActive))

    private fun checkOfferRetargetingIsDeleted(adGroupId: Long) {
        val offerRetargetings = offerRetargetingRepository
            .getOfferRetargetingsByAdGroupIds(shard, clientInfo.clientId!!, listOf(adGroupId))

        assertThat(offerRetargetings).isEmpty()
    }

    private fun checkNewOfferRetargeting(adGroupId: Long, expected: OfferRetargeting) {
        val offerRetargetingMap = offerRetargetingRepository
            .getOfferRetargetingsByAdGroupIds(shard, clientInfo.clientId!!, listOf(adGroupId))

        assertThat(offerRetargetingMap)
            .containsOnlyKeys(adGroupId)
        assertThat(offerRetargetingMap[adGroupId])
            .usingComparatorForType(BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
            .isEqualToIgnoringNullFields(expected)
    }

    companion object {
        private val AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16)
    }
}
