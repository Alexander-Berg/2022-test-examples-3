package ru.yandex.direct.core.copyentity.retargeting

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.campaign.BaseCopyCampaignTest
import ru.yandex.direct.core.copyentity.testing.CopyAssert
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultRetCondition
import ru.yandex.direct.core.testing.info.RetargetingInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.rbac.RbacRole

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignRetargetingsTest : BaseCopyCampaignTest() {

    @Autowired
    private lateinit var copyAssert: CopyAssert

    private lateinit var campaign: TextCampaignInfo

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
        campaign = steps.textCampaignSteps().createDefaultCampaign(client)
    }

    @Test
    fun `copy retargeting`() {
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(campaign)
        val retargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup)

        val result = sameClientCampaignCopyOperation(campaign).copy()

        copyAssert.assertRetargetingIsCopied(retargeting.retargetingId, result)
    }

    @Test
    fun `copy multiple retargetings`() {
        val adGroups = (1..2).map { steps.adGroupSteps().createActiveTextAdGroup(campaign) }
        val retConditions = (1..2).map { steps.retConditionSteps().createDefaultRetCondition(client) }
        val retargetings = retConditions.flatMap { retCondition ->
            adGroups.map { adGroup ->
                steps.retargetingSteps().createRetargeting(
                    RetargetingInfo()
                        .withAdGroupInfo(adGroup)
                        .withRetConditionInfo(retCondition)
                )
            }
        }

        val result = sameClientCampaignCopyOperation(campaign).copy()

        val retargetingIds = retargetings.map { it.retargetingId }
        copyAssert.assertRetargetingsAreCopied(retargetingIds, result)
    }

    @Test
    fun `copy retargeting between clients twice do not duplicates retargeting conditions`() {
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(campaign)
        val retargeting = steps.retargetingSteps().createDefaultRetargeting(adGroup)

        targetClient = steps.clientSteps().createDefaultClient()
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)

        val firstCopyResult =
            betweenClientsCampaignCopyOperation(campaign).copy()

        val firstRetCondition: RetargetingConditionBase =
            copyAssert.getCopiedEntity(retargeting.retConditionId, firstCopyResult)

        val secondCopyResult =
            betweenClientsCampaignCopyOperation(campaign).copy()

        val secondRetCondition: RetargetingConditionBase =
            copyAssert.getCopiedEntity(retargeting.retConditionId, secondCopyResult)

        softly {
            assertThat(firstRetCondition.id)
                .describedAs("first copied retargeting condition id must be different from original")
                .isNotEqualTo(retargeting.retConditionId)
            assertThat(secondRetCondition.id)
                .describedAs("second copied retargeting condition id must be equal to first copied id")
                .isEqualTo(firstRetCondition.id)
        }
    }

    private fun getRetargetingsMappings() = arrayOf(
        arrayOf(listOf(1, 2, 3, 4, 5), listOf(6, 2, 7, 3, 8)),
        arrayOf(listOf(1, 2, 1, 2, 1), listOf(5, 4, 3, 2, 1)),
        arrayOf(listOf(1, 2, 3), listOf(4, 5, 6)),
        arrayOf(listOf(5, 4, 3, 2, 1), listOf(1, 2, 3, 4, 5)),
        arrayOf(listOf(1, 3, 5, 7), listOf(1, 2, 3, 4, 5, 6, 7)),
    )

    /**
     * Тест проверяет, что условия ретаргетинга, индексы которых указаны в [firstGroupRetargetingsMapping] будут
     * скопированы между клиентами при первом копировании. Так же проверяется, что из условий ретаргетинга, индексы
     * которых указаны в [secondGroupRetargetingsMapping], скопируются только те условия, которых не было в первом
     * копировании. А те что были - получат те же самые идентификаторы, что и при первом копировании.
     */
    @Test
    @Parameters(method = "getRetargetingsMappings")
    fun `copy retargetings between clients twice do not duplicates retargeting conditions`(
        firstGroupRetargetingsMapping: List<Int>,
        secondGroupRetargetingsMapping: List<Int>,
    ) {
        val maxConditionIndex: Int = (firstGroupRetargetingsMapping + secondGroupRetargetingsMapping).maxOf { it }
        val retConditions = (0..maxConditionIndex).map { steps.retConditionSteps().createDefaultRetCondition(client) }
        val secondCampaign = steps.textCampaignSteps().createDefaultCampaign(client)
        val firstCampaignAdGroups =
            firstGroupRetargetingsMapping.map { steps.adGroupSteps().createActiveTextAdGroup(campaign) }
        val secondCampaignAdGroups =
            secondGroupRetargetingsMapping.map { steps.adGroupSteps().createActiveTextAdGroup(secondCampaign) }

        val firstCampaignRetargetings = firstGroupRetargetingsMapping.mapIndexed { index, retConditionIndex ->
            steps.retargetingSteps().createRetargeting(
                RetargetingInfo()
                    .withAdGroupInfo(firstCampaignAdGroups[index])
                    .withRetConditionInfo(retConditions[retConditionIndex])
            )
        }

        val secondCampaignRetargetings = secondGroupRetargetingsMapping.mapIndexed { index, retConditionIndex ->
            steps.retargetingSteps().createRetargeting(
                RetargetingInfo()
                    .withAdGroupInfo(secondCampaignAdGroups[index])
                    .withRetConditionInfo(retConditions[retConditionIndex])
            )
        }

        targetClient = steps.clientSteps().createDefaultClient()
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)

        val firstCopyResult = betweenClientsCampaignCopyOperation(campaign).copy()

        val firstCopiedRetConditions: List<RetargetingConditionBase> =
            copyAssert.getCopiedEntities(
                RetargetingConditionBase::class.java,
                firstCampaignRetargetings.map { it.retConditionId },
                firstCopyResult
            )

        val firstCopiedRetConditionsIdsByOriginalIds: Map<Long, Long> =
            firstCampaignRetargetings.mapIndexed { index, retargeting ->
                Pair(
                    retargeting.retConditionId,
                    firstCopiedRetConditions[index].id
                )
            }.distinct().toMap()

        val secondCopyResult = betweenClientsCampaignCopyOperation(secondCampaign).copy()

        val secondCopiedRetConditions: List<RetargetingConditionBase> =
            copyAssert.getCopiedEntities(
                RetargetingConditionBase::class.java,
                secondCampaignRetargetings.map { it.retConditionId },
                secondCopyResult
            )

        val firstCampaignOriginalRetargetingConditionsIds = firstCampaignRetargetings.map { it.retConditionId }.toSet()

        val firstCopiedRetConditionsIds = firstCopiedRetConditions.map { it.id }.toSet()

        val firstGroupRetargetingsMappingSet = firstGroupRetargetingsMapping.toSet()
        // Получим список идентификаторов условий ретаргетинга из второго копирования, которые должны соответствовать
        // идентификаторам из первого копирования
        val secondCopiedDuplicateRetConditionsIds =
            secondGroupRetargetingsMapping.mapIndexedNotNull { index, mapIndex ->
                if (firstGroupRetargetingsMappingSet.contains(mapIndex)) {
                    secondCopiedRetConditions[index].id
                } else {
                    null
                }
            }.toList()

        // И соответствующий список идентификаторов условий ретаргетинга из первого копирования
        val firstCopiedDuplicateRetConditionsIds = secondGroupRetargetingsMapping.mapNotNull {
            if (firstGroupRetargetingsMappingSet.contains(it)) {
                firstCopiedRetConditionsIdsByOriginalIds[retConditions[it].retConditionId]
            } else {
                null
            }
        }.toList()

        val secondCopiedUniqueRetConditionsIds =
            secondCopiedRetConditions.map { it.id }.toSet() - secondCopiedDuplicateRetConditionsIds.toSet()

        softly {
            assertThat(firstCopiedRetConditionsIds)
                .describedAs("all first campaign retargeting conditions are copied")
                .hasSize(firstGroupRetargetingsMappingSet.size)

            assertThat(firstCopiedRetConditionsIds)
                .describedAs("first copied retargeting condition ids must be different from original")
                .isNotIn(firstCampaignOriginalRetargetingConditionsIds)

            assertThat(secondCopiedUniqueRetConditionsIds)
                .describedAs(
                    "unique copied retargeting condition ids size must be equal " +
                        "to all copied retargeting conditions size minus duplicate ones"
                )
                .hasSize(secondCopiedRetConditions.size - secondCopiedDuplicateRetConditionsIds.size)

            assertThat(secondCopiedUniqueRetConditionsIds)
                .describedAs(
                    "unique copied retargeting condition ids must not be in first copied retargeting condition ids"
                )
                .isNotIn(firstCopiedRetConditionsIds)

            assertThat(secondCopiedDuplicateRetConditionsIds)
                .describedAs(
                    "duplicate copied retargeting condition ids must be equal to first copied ones"
                )
                .isEqualTo(firstCopiedDuplicateRetConditionsIds)
        }
    }

    private fun generateRetargetingCopyNames() = arrayOf(
        arrayOf(
            listOf("условие ретаргетинга", "условие ретаргетинга 2"),
            listOf("условие ретаргетинга", "условие ретаргетинга 2"),
            listOf("условие ретаргетинга (копия)", "условие ретаргетинга 2 (копия)")
        ),
        arrayOf(
            listOf("условие ретаргетинга 1", "условие ретаргетинга 1 (копия)", "условие ретаргетинга 1 (копия 2)"),
            listOf("условие ретаргетинга 1", "условие ретаргетинга 1 (копия)"),
            listOf("условие ретаргетинга 1 (копия 3)", "условие ретаргетинга 1 (копия 4)")
        ),
        arrayOf(
            listOf("условие ретаргетинга (копия)", "условие ретаргетинга (копия 2)"),
            listOf("условие ретаргетинга", "условие ретаргетинга (копия)"),
            listOf("условие ретаргетинга", "условие ретаргетинга (копия 3)")
        ),
        arrayOf(
            listOf(
                "условие ретаргетинга",
                "условие ретаргетинга (копия)",
                "условие ретаргетинга (копия 3)",
                "условие ретаргетинга (копия 99)"
            ),
            listOf(
                "условие ретаргетинга",
                "условие ретаргетинга (копия 2)",
                "условие ретаргетинга (копия 3)",
                "условие ретаргетинга (копия 5)"
            ),
            listOf(
                "условие ретаргетинга (копия 100)",
                "условие ретаргетинга (копия 2)",
                "условие ретаргетинга (копия 101)",
                "условие ретаргетинга (копия 5)"
            )
        ),
        arrayOf(
            listOf("тестовое условие ретаргетинга (копия)", "тестовое условие ретаргетинга (копия 2)"),
            listOf("тестовое условие ретаргетинга", "тестовое условие ретаргетинга (копия 3)"),
            listOf("тестовое условие ретаргетинга", "тестовое условие ретаргетинга (копия 3)")
        ),
    )

    @Test
    @Parameters(method = "generateRetargetingCopyNames")
    fun `copy retargetings between clients do not duplicates names`(
        targetClientRetConditionNames: List<String>,
        sourceClientRetConditionNames: List<String>,
        expectedRetConditionNamesAfterCopyBetweenClients: List<String>,
    ) {
        targetClient = steps.clientSteps().createDefaultClient()
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)

        val sourceRetConditions = sourceClientRetConditionNames.map {
            steps.retConditionSteps().createRetCondition(
                defaultRetCondition(client.clientId).withName(it) as RetargetingCondition,
                client
            )
        }

        targetClientRetConditionNames.forEach {
            steps.retConditionSteps().createRetCondition(
                defaultRetCondition(targetClient.clientId).withName(it) as RetargetingCondition,
                targetClient
            )
        }

        val sourceCampaignAdGroups = sourceRetConditions.map { steps.adGroupSteps().createActiveTextAdGroup(campaign) }

        val sourceCampaignRetargetings = sourceCampaignAdGroups.mapIndexed { index, adGroup ->
            steps.retargetingSteps().createRetargeting(
                RetargetingInfo()
                    .withAdGroupInfo(adGroup)
                    .withRetConditionInfo(sourceRetConditions[index])
            )
        }

        val copyResult = betweenClientsCampaignCopyOperation(campaign).copy()

        val copiedRetConditions: List<RetargetingConditionBase> =
            copyAssert.getCopiedEntities(
                RetargetingConditionBase::class.java,
                sourceCampaignRetargetings.map { it.retConditionId },
                copyResult
            )

        val copiedRetConditionsNames = copiedRetConditions.map { it.name }

        assertThat(copiedRetConditionsNames)
            .describedAs("copied retargeting conditions names must be equals to expected")
            .isEqualTo(expectedRetConditionNamesAfterCopyBetweenClients)
    }

    @Test
    fun `copy interest retargeting condition between clients always creates new one`() {
        steps.featureSteps()
            .addClientFeature(client.clientId!!, FeatureName.TEXT_BANNER_INTERESTS_RET_COND_ENABLED, true)
        val adGroup = steps.adGroupSteps().createActiveTextAdGroup(campaign)
        val retargeting = steps.retargetingSteps().createRetargeting(
            RetargetingInfo()
                .withAdGroupInfo(adGroup)
                .withRetConditionInfo(steps.retConditionSteps().createDefaultInterestRetCondition(client))
        )

        targetClient = steps.clientSteps().createDefaultClient()
        steps.featureSteps()
            .addClientFeature(targetClient.clientId!!, FeatureName.TEXT_BANNER_INTERESTS_RET_COND_ENABLED, true)
        superClient = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER)

        val firstCopyResult = betweenClientsCampaignCopyOperation(campaign).copy()

        val firstCopiedRetConditionsIds =
            copyAssert.getAllCopiedEntityIds(RetargetingConditionBase::class.java, firstCopyResult)

        val secondCopyResult = betweenClientsCampaignCopyOperation(campaign).copy()

        val secondCopiedRetConditionsIds =
            copyAssert.getAllCopiedEntityIds(RetargetingConditionBase::class.java, secondCopyResult)

        val originalAndFirstRetConditions = firstCopiedRetConditionsIds + retargeting.retConditionId

        softly {
            assertThat(firstCopiedRetConditionsIds)
                .describedAs("only one retargeting condition is copied in first copy operation")
                .hasSize(1)

            assertThat(firstCopiedRetConditionsIds)
                .describedAs("first copied retargeting condition ids must be different from original")
                .doesNotContain(retargeting.retConditionId)

            assertThat(secondCopiedRetConditionsIds)
                .describedAs("only one retargeting condition is copied in second copy operation")
                .hasSize(1)

            assertThat(secondCopiedRetConditionsIds)
                .describedAs(
                    "second copied interest retargeting condition id must be different from original and first copied")
                .doesNotContainAnyElementsOf(originalAndFirstRetConditions)
        }
    }
}
