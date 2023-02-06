package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.entity.retargeting.model.Rule
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultBrandSafetyRetCondition
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns
import ru.yandex.direct.core.testing.steps.RetConditionSteps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.rbac.RbacRole

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignWithBrandSafetyTest : BaseCopyCampaignTest() {
    private val BRAND_SAFETY_ADULT_ID = 4294967297L
    private val BRAND_SAFETY_TERRORISM_ID = 4294967298L
    private val BRAND_SAFETY_NEGATIVE_ID = 4294967299L
    private val BRAND_SAFETY_WEAPONS_ID = 4294967300L
    private val BRAND_SAFETY_OBSCENE_ID = 4294967301L
    private val BRAND_SAFETY_GAMBLING_ID = 4294967304L
    private val BRAND_SAFETY_TOBACCO_ID = 4294967312L

    private val BRAND_SAFETY_POLITICS_ID = 4294967302L
    private val BRAND_SAFETY_CHILD_ID = 4294967303L
    private val BRAND_SAFETY_NEWS_ID = 4294967305L
    private val BRAND_SAFETY_RELIGION_ID = 4294967307L
    private val BRAND_SAFETY_GAMES_ID = 4294967308L
    private val BRAND_SAFETY_DATING_ID = 4294967309L
    private val BRAND_SAFETY_MEDICINE_ID = 4294967310L
    private val BRAND_SAFETY_ALCO_ID = 4294967311L
    private val BRAND_SAFETY_PARA_ID = 4294967313L
    private val BRAND_SAFETY_UKRAINE_ID = 4294967314L

    private val BRAND_SAFETY_BASE_CATEGORIES = listOf(
        BRAND_SAFETY_ADULT_ID,
        BRAND_SAFETY_TERRORISM_ID,
        BRAND_SAFETY_NEGATIVE_ID,
        BRAND_SAFETY_WEAPONS_ID,
        BRAND_SAFETY_OBSCENE_ID,
        BRAND_SAFETY_GAMBLING_ID,
        BRAND_SAFETY_TOBACCO_ID
    )

    private val BRAND_SAFETY_ADDITIONAL_CATEGORIES_TO_FEATURE = mapOf(
        BRAND_SAFETY_POLITICS_ID to FeatureName.BRANDSAFETY_POLITICS,
        BRAND_SAFETY_CHILD_ID to FeatureName.BRANDSAFETY_CHILD,
        BRAND_SAFETY_NEWS_ID to FeatureName.BRANDSAFETY_NEWS,
        BRAND_SAFETY_RELIGION_ID to FeatureName.BRANDSAFETY_RELIGION,
        BRAND_SAFETY_GAMES_ID to FeatureName.BRANDSAFETY_GAMES,
        BRAND_SAFETY_DATING_ID to FeatureName.BRANDSAFETY_DATING,
        BRAND_SAFETY_MEDICINE_ID to FeatureName.BRANDSAFETY_MEDICINE,
        BRAND_SAFETY_ALCO_ID to FeatureName.BRANDSAFETY_ALCO,
        BRAND_SAFETY_PARA_ID to FeatureName.BRANDSAFETY_PARA,
        BRAND_SAFETY_UKRAINE_ID to FeatureName.BRANDSAFETY_UKRAINE
    )

    private val BRAND_SAFETY_ADDITIONAL_CATEGORIES: List<Long> =
        BRAND_SAFETY_ADDITIONAL_CATEGORIES_TO_FEATURE.keys.toList()

    @Autowired
    private lateinit var retConditionSteps: RetConditionSteps

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        targetClient = steps.clientSteps().createDefaultClient()
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)
    }

    fun brandSafetyParameters(): Array<Array<Any>> {
        val firstCombination = listOf(
            BRAND_SAFETY_POLITICS_ID,
            BRAND_SAFETY_CHILD_ID,
            BRAND_SAFETY_NEWS_ID,
            BRAND_SAFETY_RELIGION_ID,
            BRAND_SAFETY_GAMES_ID,
        )

        val secondCombination = listOf(
            BRAND_SAFETY_GAMES_ID,
            BRAND_SAFETY_DATING_ID,
            BRAND_SAFETY_MEDICINE_ID,
            BRAND_SAFETY_ALCO_ID,
            BRAND_SAFETY_PARA_ID,
            BRAND_SAFETY_UKRAINE_ID,
        )

        val thirdCombination = listOf(
            BRAND_SAFETY_POLITICS_ID,
            BRAND_SAFETY_DATING_ID,
            BRAND_SAFETY_MEDICINE_ID,
            BRAND_SAFETY_ALCO_ID,
            BRAND_SAFETY_NEWS_ID,
            BRAND_SAFETY_RELIGION_ID,
        )

        val additionalTestCases = BRAND_SAFETY_ADDITIONAL_CATEGORIES_TO_FEATURE.entries.map { entry ->
            arrayOf(
                "${entry.key} with feature",
                listOf(entry.key),
                setOf(entry.value),
                listOf(entry.key)
            )
        }.toTypedArray()

        return arrayOf(
            arrayOf(
                "one base",
                listOf(BRAND_SAFETY_ADULT_ID),
                emptySet<FeatureName>(),
                listOf(BRAND_SAFETY_ADULT_ID)
            ),
            arrayOf(
                "several base",
                listOf(
                    BRAND_SAFETY_ADULT_ID,
                    BRAND_SAFETY_NEGATIVE_ID,
                    BRAND_SAFETY_TERRORISM_ID,
                    BRAND_SAFETY_TOBACCO_ID
                ),
                emptySet<FeatureName>(),
                listOf(
                    BRAND_SAFETY_ADULT_ID,
                    BRAND_SAFETY_NEGATIVE_ID,
                    BRAND_SAFETY_TERRORISM_ID,
                    BRAND_SAFETY_TOBACCO_ID
                )
            ),
            arrayOf(
                "all base",
                BRAND_SAFETY_BASE_CATEGORIES,
                emptySet<FeatureName>(),
                BRAND_SAFETY_BASE_CATEGORIES
            ),
            arrayOf(
                "all base categories with feature",
                BRAND_SAFETY_BASE_CATEGORIES,
                BRAND_SAFETY_ADDITIONAL_CATEGORIES_TO_FEATURE.values.toSet(),
                BRAND_SAFETY_BASE_CATEGORIES
            ),
            arrayOf(
                "one additional without feature",
                listOf(BRAND_SAFETY_DATING_ID),
                emptySet<FeatureName>(),
                emptyList<Long>()
            ),
            arrayOf(
                "one additional with different feature",
                listOf(BRAND_SAFETY_DATING_ID),
                setOf(FeatureName.BRANDSAFETY_ALCO),
                emptyList<Long>()
            ),
            arrayOf(
                "all base + all additional without features",
                BRAND_SAFETY_BASE_CATEGORIES + BRAND_SAFETY_ADDITIONAL_CATEGORIES,
                emptySet<FeatureName>(),
                BRAND_SAFETY_BASE_CATEGORIES
            ),
            arrayOf("all additional with first combination of features",
                BRAND_SAFETY_ADDITIONAL_CATEGORIES,
                firstCombination.map { category -> BRAND_SAFETY_ADDITIONAL_CATEGORIES_TO_FEATURE[category] }
                    .toSet(),
                firstCombination
            ),
            arrayOf("all additional with second combination of features",
                BRAND_SAFETY_ADDITIONAL_CATEGORIES,
                secondCombination.map { category -> BRAND_SAFETY_ADDITIONAL_CATEGORIES_TO_FEATURE[category] }
                    .toSet(),
                secondCombination
            ),
            arrayOf("all additional with third combination of features",
                BRAND_SAFETY_ADDITIONAL_CATEGORIES,
                thirdCombination.map { category -> BRAND_SAFETY_ADDITIONAL_CATEGORIES_TO_FEATURE[category] }
                    .toSet(),
                thirdCombination
            ),
            arrayOf(
                "all additional with all features",
                BRAND_SAFETY_ADDITIONAL_CATEGORIES,
                BRAND_SAFETY_ADDITIONAL_CATEGORIES_TO_FEATURE.values.map { feature -> feature }.toSet(),
                BRAND_SAFETY_ADDITIONAL_CATEGORIES,
            ),
            arrayOf(
                "empty",
                emptyList<Long>(),
                emptySet<FeatureName>(),
                emptyList<Long>(),
            ),
        ) + additionalTestCases
    }

    @Test
    @TestCaseName("{method}({0})}")
    @Parameters(method = "brandSafetyParameters")
    fun `test brandSafety categories preprocessing`(
        description: String,
        brandSafetyCategories: List<Long>,
        features: Set<FeatureName>,
        expectedBrandSafetyCategories: List<Long>
    ) {
        val goals: List<Goal> =
            brandSafetyCategories.map { x ->
                Goal().apply {
                    type = GoalType.BRANDSAFETY
                    id = x
                    time = 0
                }
            }
                .toList()

        val retCondition = defaultBrandSafetyRetCondition(targetClient.clientId)
        retCondition.rules = listOf(Rule().withGoals(goals))

        val retargetingConditionInfo = retConditionSteps.createRetCondition(retCondition)

        val campaign = steps.textCampaignSteps()
            .createCampaign(
                client,
                TestTextCampaigns.fullTextCampaign()
                    .withBrandSafetyCategories(brandSafetyCategories)
                    .withBrandSafetyRetCondId(retargetingConditionInfo.retConditionId)
            )

        features.forEach { feature -> steps.featureSteps().enableClientFeature(targetClient.clientId, feature) }

        val copiedCampaign = copyValidCampaignBetweenClients(campaign)

        assertThat(copiedCampaign.brandSafetyCategories.sorted()).isEqualTo(expectedBrandSafetyCategories.sorted())
    }
}
