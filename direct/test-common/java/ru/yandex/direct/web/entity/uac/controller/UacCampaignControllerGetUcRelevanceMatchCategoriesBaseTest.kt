package ru.yandex.direct.web.entity.uac.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatch
import ru.yandex.direct.core.entity.uac.model.relevance_match.UacRelevanceMatchCategory
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.uac.UacCampaignSteps
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource

abstract class UacCampaignControllerGetUcRelevanceMatchCategoriesBaseTest {

    @Autowired
    protected lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    protected lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    protected lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    protected lateinit var uacCampaignSteps: UacCampaignSteps

    protected lateinit var mockMvc: MockMvc
    protected lateinit var clientInfo: ClientInfo

    protected fun casesForAutotargetingCategories(): List<List<Any?>> {
        val activeCategories = UacRelevanceMatchCategory.values()
            .map { listOf(true, setOf(it, UacRelevanceMatchCategory.EXACT_MARK).sorted()) }
        val notActiveCategories = UacRelevanceMatchCategory.values()
            .map { listOf(false, setOf(it, UacRelevanceMatchCategory.EXACT_MARK).sorted()) }
        return activeCategories
            .plus(notActiveCategories)
            .plus(listOf(listOf(true, UacRelevanceMatchCategory.values().sorted())))
            .plus(listOf(listOf(false, UacRelevanceMatchCategory.values().sorted())))
            .plus(listOf(listOf(true, emptyList<UacRelevanceMatchCategory>())))
            .plus(listOf(listOf(true, null)))
    }

    protected fun getRelevanceMatch(
        active: Boolean,
        selectedCategories: Set<UacRelevanceMatchCategory>?,
    ) = if (selectedCategories != null) {
        UacRelevanceMatch(
            active = active,
            categories = selectedCategories,
        )
    } else null

    protected fun sendRequestAndGetRelevanceMatchCategories(ucCampaignId: String): Pair<Boolean, Set<Map<*, *>>> {
        val result = mockMvc.perform(
            MockMvcRequestBuilders
                .get("/uac/campaign/${ucCampaignId}?ulogin=${clientInfo.login}")
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val resultJsonTree = JsonUtils.MAPPER.readTree(result)
        val relevanceMatchCategoriesNode = resultJsonTree["result"]["relevance_match_categories"]["categories"]
        val relevanceMatchCategories = JsonUtils.MAPPER.treeToValue(relevanceMatchCategoriesNode, List::class.java)
            .map { it as Map<*, *> }
            .toSet()
        val relevanceMatchActiveNode = resultJsonTree["result"]["relevance_match_categories"]["active"]
        val relevanceMatchActive = JsonUtils.MAPPER.treeToValue(relevanceMatchActiveNode, Boolean::class.java)
        return relevanceMatchActive to relevanceMatchCategories
    }

    protected fun getExpectedCategoryItem(
        active: Boolean,
        selectedCategories: Set<UacRelevanceMatchCategory>?,
        category: UacRelevanceMatchCategory,
    ) = mapOf(
        "disabled" to (category == UacRelevanceMatchCategory.EXACT_MARK),
        "selected" to expectSelected(active, selectedCategories, category),
        "relevance_match_category" to category.name,
    )

    private fun expectSelected(
        active: Boolean,
        selectedCategories: Set<UacRelevanceMatchCategory>?,
        category: UacRelevanceMatchCategory,
    ) = !active || selectedCategories.isNullOrEmpty() || selectedCategories.contains(category)
}
