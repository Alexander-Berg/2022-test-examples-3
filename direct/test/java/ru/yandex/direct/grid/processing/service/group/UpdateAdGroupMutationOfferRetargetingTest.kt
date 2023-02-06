package ru.yandex.direct.grid.processing.service.group

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.feature.FeatureName.ENABLE_OFFER_RETARGETINGS_IN_TEXT_AD_GROUP
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem
import ru.yandex.direct.grid.processing.util.ContextHelper.buildContext
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import java.math.BigDecimal

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class UpdateAdGroupMutationOfferRetargetingTest : UpdateAdGroupMutationBaseTest() {
    @Before
    fun init() {
        super.initTestData()
        steps.featureSteps().addClientFeature(clientInfo.clientId!!, ENABLE_OFFER_RETARGETINGS_IN_TEXT_AD_GROUP, true)
        val defaultOfferRetargeting = steps.offerRetargetingSteps()
            .defaultOfferRetargetingForGroup(textAdGroupInfo)
        offerRetargeting = steps.offerRetargetingSteps()
            .addOfferRetargetingToAdGroup(defaultOfferRetargeting, textAdGroupInfo)
    }

    @Test
    fun checkUpdateAdGroupOfferRetargeting_stayOn() {
        val request = requestBuilder()
            .requestFromOriginalAdGroupParams
            .build()
        val result = processor.processQuery(null, getQuery(request), null, buildContext(operator))

        assertThat(result.errors).isEmpty()

        val expectedPayload = GdUpdateAdGroupPayload()
            .withUpdatedAdGroupItems(listOf(GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)))

        val data = result.getData<Map<String, Any>>()
        softAssertions.assertThat(data).containsOnlyKeys(MUTATION_NAME)

        val payload = GraphQlJsonUtils.convertValue(data[MUTATION_NAME], GdUpdateAdGroupPayload::class.java)
        softAssertions.assertThat(payload)
            .isEqualToComparingFieldByFieldRecursively(expectedPayload)

        checkOfferRetargetingDbState(offerRetargeting)
        softAssertions.assertAll()
    }

    @Test
    fun checkUpdateAdGroupOfferRetargeting_stayOn_withRelevanceMatch() {
        val request = requestBuilder()
            .requestFromOriginalAdGroupParams
            .setGeneralPrice(BigDecimal.ONE)
            .setRelevanceMatch(true, null)
            .build()
        val result = processor.processQuery(null, getQuery(request), null, buildContext(operator))
        softAssertions.assertThat(result.errors).hasSize(1)
        softAssertions.assertAll()
    }

    @Test
    fun checkUpdateAdGroupOfferRetargeting_switchOff() {
        val request = requestBuilder()
            .requestFromOriginalAdGroupParams
            .disableOfferRetargeting()
            .build()
        val result = processor.processQuery(null, getQuery(request), null, buildContext(operator))

        assertThat(result.errors).isEmpty()

        val expectedPayload = GdUpdateAdGroupPayload()
            .withUpdatedAdGroupItems(listOf(GdUpdateAdGroupPayloadItem().withAdGroupId(adGroupId)))
        val data = result.getData<Map<String, Any>>()

        softAssertions.assertThat(data).containsOnlyKeys(MUTATION_NAME)

        val payload = GraphQlJsonUtils.convertValue(data[MUTATION_NAME], GdUpdateAdGroupPayload::class.java)

        softAssertions.assertThat(payload)
            .isEqualToComparingFieldByFieldRecursively(expectedPayload)

        checkOfferRetargetingIsDeleted(adGroupId)
        softAssertions.assertAll()
    }
}
