package ru.yandex.direct.web.entity.uac.controller

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
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.ACCESSORY_MARK
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.ALTERNATIVE_MARK
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.BROADER_MARK
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.COMPETITOR_MARK
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory.EXACT_MARK
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacCampaignControllerGetUcRelevanceMatchCategoriesTest : UacCampaignControllerGetUcRelevanceMatchCategoriesBaseTest() {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        clientInfo = testAuthHelper.createDefaultUser().clientInfo!!
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
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
        val uacCampaignInfo = uacCampaignSteps.createTextCampaign(
            clientInfo,
            relevanceMatch = relevanceMatch,
        )

        val relevanceMatchActiveToCategories = sendRequestAndGetRelevanceMatchCategories(uacCampaignInfo.uacCampaign.id)
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
}
