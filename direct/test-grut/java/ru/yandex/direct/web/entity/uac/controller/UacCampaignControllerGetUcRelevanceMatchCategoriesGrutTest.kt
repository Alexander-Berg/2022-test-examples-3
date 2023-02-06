package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.uac.createYdbCampaign
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.core.entity.uac.model.TargetStatus
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.ACCESSORY_MARK
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.ALTERNATIVE_MARK
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.BROADER_MARK
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.COMPETITOR_MARK
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.EXACT_MARK
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbCampaign
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.steps.CampaignSteps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.web.configuration.GrutDirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper

@GrutDirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerGetUcRelevanceMatchCategoriesGrutTest : UacCampaignControllerGetUcRelevanceMatchCategoriesBaseTest() {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var campaignSteps: CampaignSteps

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        grutSteps.createClient(clientInfo)
    }

    /**
     * Проверяем получение выбранных категорий автотаргетинга в зависимости от сохраненных данных в заявке
     */
    @Test
    @TestCaseName("Active {0} categories {1}")
    @Parameters(method = "casesForAutotargetingCategories")
    fun testGetRelevanceMatchCategories(
        active: Boolean,
        selectedCategories: List<UacRelevanceMatchCategory>?,
    ) {
        val selectedCategoriesSet = selectedCategories?.toSet()

        val relevanceMatch = getRelevanceMatch(active, selectedCategoriesSet)
        val ucCampaign = createUcCampaign(relevanceMatch)

        val relevanceMatchActiveToCategories = sendRequestAndGetRelevanceMatchCategories(ucCampaign.id)
        val relevanceMatchActive = relevanceMatchActiveToCategories.first
        val relevanceMatchCategoryItems = relevanceMatchActiveToCategories.second

        val expectRelevanceMatchCategories = setOf(
            getExpectedCategoryItem(active, selectedCategoriesSet, EXACT_MARK),
            getExpectedCategoryItem(active, selectedCategoriesSet, BROADER_MARK),
            getExpectedCategoryItem(active, selectedCategoriesSet, ACCESSORY_MARK),
            getExpectedCategoryItem(active, selectedCategoriesSet, ALTERNATIVE_MARK),
            getExpectedCategoryItem(active, selectedCategoriesSet, COMPETITOR_MARK),
        )

        SoftAssertions.assertSoftly {
            assertThat(relevanceMatchActive)
                .`as`("Флаг включения выбора категорий автотаргетинга")
                .isEqualTo(relevanceMatch != null && active)
            assertThat(relevanceMatchCategoryItems)
                .`as`("Выбранные категории автотаргетинга")
                .isEqualTo(expectRelevanceMatchCategories)
        }
    }

    private fun createUcCampaign(relevanceMatch: UacRelevanceMatch?): UacYdbCampaign {
        val directCampaign = TestCampaigns.activeTextCampaign(clientInfo.clientId, clientInfo.uid)
            .withOrderId(0L)
            .withBalanceInfo(TestCampaigns.emptyBalanceInfo(CurrencyCode.RUB))
            .withStrategy(TestCampaigns.averageCpaStrategy())
            .withSource(CampaignSource.UAC)
        val campaignInfo = campaignSteps.createCampaign(directCampaign, clientInfo)
        val ucCampaign = createYdbCampaign(
            id = campaignInfo.campaignId.toIdString(),
            relevanceMatch = relevanceMatch,
            targetStatus = TargetStatus.STARTED,
            accountId = clientInfo.clientId!!.toString(),
            startedAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
        )
        grutSteps.createTextCampaign(clientInfo, ucCampaign)
        return ucCampaign
    }
}
