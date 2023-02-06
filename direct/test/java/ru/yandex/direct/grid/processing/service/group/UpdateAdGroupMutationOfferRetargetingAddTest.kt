package ru.yandex.direct.grid.processing.service.group

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.feature.FeatureName.ENABLE_OFFER_RETARGETINGS_IN_TEXT_AD_GROUP
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem
import ru.yandex.direct.grid.processing.util.ContextHelper.buildContext
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue
import java.math.BigDecimal

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class UpdateAdGroupMutationOfferRetargetingAddTest : UpdateAdGroupMutationBaseTest() {

    @Before
    fun init() {
        super.initTestData()
        steps.featureSteps().addClientFeature(clientInfo.clientId!!, ENABLE_OFFER_RETARGETINGS_IN_TEXT_AD_GROUP, true)
    }

    @Test
    fun checkUpdateAdGroupOfferRetargeting_add_withDisabledFeature() {
        steps.featureSteps().addClientFeature(clientInfo.clientId!!, ENABLE_OFFER_RETARGETINGS_IN_TEXT_AD_GROUP, false)
        val request = requestBuilder()
            .requestFromOriginalAdGroupParams
            .setOfferRetargeting(true, null)
            .build()

        val result = processor.processQuery(null, getQuery(request), null, buildContext(operator))
        softAssertions.assertThat(result.errors).hasSize(1)
        softAssertions.assertAll()
    }

    @Test
    fun checkUpdateAdGroupOfferRetargeting_addWithRelevanceMatch() {
        val request = requestBuilder()
            .requestFromOriginalAdGroupParams
            .setGeneralPrice(BigDecimal.ONE)
            .setOfferRetargeting(true, null)
            .setRelevanceMatch(true, null)
            .build()

        val result = processor.processQuery(null, getQuery(request), null, buildContext(operator))
        softAssertions.assertThat(result.errors).hasSize(1)
        softAssertions.assertAll()
    }

    @Test
    fun checkUpdateAdGroupOfferRetargeting_add() {
        val request = requestBuilder()
            .requestFromOriginalAdGroupParams
            .setOfferRetargeting(true, null)
            .build()

        val result = processor.processQuery(null, getQuery(request), null, buildContext(operator))

        assertThat(result.errors).isEmpty()

        val expectedPayload = GdUpdateAdGroupPayload()
            .withUpdatedAdGroupItems(listOf(GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)))

        val data = result.getData<Map<String, Any>>()
        softAssertions.assertThat(data).containsOnlyKeys(MUTATION_NAME)

        val payload = convertValue(data[MUTATION_NAME], GdUpdateAdGroupPayload::class.java)

        softAssertions.assertThat(payload)
            .isEqualToComparingFieldByFieldRecursively(expectedPayload)

        val expectedOfferRetargeting = OfferRetargeting()
            .withAdGroupId(adGroupId)
            .withCampaignId(textAdGroupInfo.campaignId)
            .withIsDeleted(false)
            .withIsSuspended(false)
            .withStatusBsSynced(StatusBsSynced.NO)

        checkNewOfferRetargeting(adGroupId, expectedOfferRetargeting)
        softAssertions.assertAll()
    }

    @Test
    fun checkUpdateAdGroupOfferRetargeting_stayOff() {
        val request = requestBuilder()
            .requestFromOriginalAdGroupParams
            .build()

        val result = processor.processQuery(null, getQuery(request), null, buildContext(operator))
        assertThat(result.errors).isEmpty()

        val data = result.getData<Map<String, Any>>()
        softAssertions.assertThat(data).containsOnlyKeys(MUTATION_NAME)

        val expectedPayload = GdUpdateAdGroupPayload()
            .withUpdatedAdGroupItems(listOf(GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)))

        val payload = convertValue(data[MUTATION_NAME], GdUpdateAdGroupPayload::class.java)
        softAssertions.assertThat(payload).isEqualToComparingFieldByFieldRecursively(expectedPayload)

        checkOfferRetargetingIsDeleted(textAdGroupInfo.adGroupId)
        softAssertions.assertAll()
    }
}
