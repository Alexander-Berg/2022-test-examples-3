package ru.yandex.direct.core.copyentity.testing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils.assumeCopyResultIsSuccessful
import ru.yandex.direct.core.copyentity.CopyOperationContainer
import ru.yandex.direct.core.copyentity.CopyResult
import ru.yandex.direct.core.copyentity.EntityGraphNavigator
import ru.yandex.direct.core.copyentity.EntityService
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.bidmodifier.BidModifier
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoalId
import ru.yandex.direct.core.entity.retargeting.model.Retargeting
import ru.yandex.direct.core.entity.vcard.model.Vcard
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.Entity
import ru.yandex.direct.validation.result.DefectId

@Component
class CopyAssert {

    @Autowired
    private lateinit var entityGraphNavigator: EntityGraphNavigator

    fun <T : CommonCampaign> assertCampaignIsCopied(actual: T, expected: T) {
        val expectedCampaign = entityGraphNavigator.getEntityService<BaseCampaign, Long>(BaseCampaign::class.java)
            .get(ClientId.fromLong(expected.clientId), expected.uid, listOf(expected.id))
            .first()
        assertThat(actual)
            .usingRecursiveComparison(CopyAssertStrategies.CAMPAIGN_COMPARE_STRATEGY)
            .isEqualTo(expectedCampaign)
    }

    fun assertCampaignIsCopied(campaignId: Long, result: CopyResult<*>) =
        assertEntityIsCopied(BaseCampaign::class.java, listOf(campaignId), result)

    fun assertAdGroupIsCopied(adGroupId: Long, result: CopyResult<*>) =
        assertEntityIsCopied(AdGroup::class.java, listOf(adGroupId), result)

    fun assertAdGroupsAreCopied(adGroupIds: List<Long>, result: CopyResult<*>) =
        assertEntityIsCopied(AdGroup::class.java, adGroupIds, result)

    fun assertBannerIsCopied(bannerId: Long, result: CopyResult<*>) =
        assertEntityIsCopied(BannerWithAdGroupId::class.java, listOf(bannerId), result)

    fun assertBannersAreCopied(bannerIds: List<Long>, result: CopyResult<*>) =
        assertEntityIsCopied(BannerWithAdGroupId::class.java, bannerIds, result)

    fun assertVcardIsCopied(vcardId: Long, result: CopyResult<*>) =
        assertEntityIsCopied(Vcard::class.java, listOf(vcardId), result)

    fun assertBidModifierIsCopied(bidModifierId: Long, result: CopyResult<*>) =
        assertEntityIsCopied(BidModifier::class.java, listOf(bidModifierId), result)

    fun assertMobileAppIsCopied(mobileAppId: Long, result: CopyResult<*>) =
        assertEntityIsCopied(MobileApp::class.java, listOf(mobileAppId), result)

    fun assertMobileAppIsCopied(mobileAppId: Long, result: CopyResult<*>, softAssert: SoftAssertions) =
        assertEntityIsCopied(MobileApp::class.java, listOf(mobileAppId), result, softAssert)

    fun assertCampMetrikaGoalIsCopied(campaignId: Long, goalId: Long, result: CopyResult<*>) =
        assertCampMetrikaGoalsAreCopied(campaignId, listOf(goalId), result)

    fun assertCampMetrikaGoalsAreCopied(campaignId: Long, goalIds: List<Long>, result: CopyResult<*>) =
        assertEntityIsCopied(
            CampMetrikaGoal::class.java,
            goalIds.map { goalId ->
                CampMetrikaGoalId()
                    .withCampaignId(campaignId)
                    .withGoalId(goalId)
            },
            result,
        )

    fun assertRetargetingIsCopied(retargetingId: Long, result: CopyResult<*>) =
        assertRetargetingsAreCopied(listOf(retargetingId), result)

    fun assertRetargetingsAreCopied(retargetingIds: List<Long>, result: CopyResult<*>) =
        assertEntityIsCopied(Retargeting::class.java, retargetingIds, result)

    private fun <T : Entity<KeyT>, KeyT> assertEntityIsCopied(
        entity: Class<T>,
        sourceEntityIds: Collection<KeyT>,
        result: CopyResult<*>,
        softAssert: SoftAssertions? = null,
    ) {
        assumeCopyResultIsSuccessful(result)

        @Suppress("UNCHECKED_CAST")
        val service: EntityService<T, KeyT> = entityGraphNavigator.getEntityService(entity)
        val container: CopyOperationContainer = result.entityContext.copyContainer

        val sourceEntities = service.get(container.clientIdFrom, container.operatorUid, sourceEntityIds)

        val copiedEntityIds = getCopiedEntityIds(entity, sourceEntityIds, result)
        val copiedEntities = service.get(container.clientIdTo, container.operatorUid, copiedEntityIds)

        val compareConfiguration: RecursiveComparisonConfiguration = CopyAssertStrategies.STRATEGY_BY_ENTITIES[entity]!!
        if (softAssert == null) {
            if (sourceEntities.size == 1) {
                assertThat(copiedEntities.first())
                    .usingRecursiveComparison(compareConfiguration)
                    .isEqualTo(sourceEntities.first())
            } else {
                assertThat(copiedEntities)
                    .usingRecursiveComparison(compareConfiguration)
                    .isEqualTo(sourceEntities)
            }
        } else {
            if (sourceEntities.size == 1) {
                softAssert.assertThat(copiedEntities.first())
                    .usingRecursiveComparison(compareConfiguration)
                    .isEqualTo(sourceEntities.first())
            } else {
                softAssert.assertThat(copiedEntities)
                    .usingRecursiveComparison(compareConfiguration)
                    .isEqualTo(sourceEntities)
            }
        }
    }

    fun <T : Entity<KeyT>, KeyT> getFirstEntityFromContext(entityClass: Class<T>, result: CopyResult<*>) : T =
        result.entityContext.getObjects(entityClass).first()

    @Suppress("RedundantModalityModifier")  // потому что allopen
    final inline fun <reified T : Entity<KeyT>, KeyT> getCopiedEntity(id: KeyT, result: CopyResult<*>) =
        getCopiedEntities(T::class.java, listOf(id), result).first()

    fun <T : Entity<KeyT>, KeyT> getCopiedEntity(entity: Class<T>, id: KeyT, result: CopyResult<*>) =
        getCopiedEntities(entity, listOf(id), result).first()

    fun <T : Entity<KeyT>, KeyT> getCopiedEntities(
        entity: Class<T>,
        sourceEntityIds: Collection<KeyT>,
        result: CopyResult<*>,
    ): List<T> {
        assumeCopyResultIsSuccessful(result)

        @Suppress("UNCHECKED_CAST")
        val service: EntityService<T, KeyT> = entityGraphNavigator.getEntityService(entity)
        val container: CopyOperationContainer = result.entityContext.copyContainer

        val copiedEntityIds = getCopiedEntityIds(entity, sourceEntityIds, result)
        val entities: List<T> = service.get(container.clientIdTo, container.operatorUid, copiedEntityIds)
        // Пробуем восстановить оригинальный порядок, так как сервис может вернуть не в том порядке,
        // в котором спрашивали
        val entitiesById = entities.associateBy { it.id }
        return copiedEntityIds.mapNotNull { entitiesById[it] }
    }

    private fun <T : Entity<KeyT>, KeyT> getCopiedEntityIds(
        entity: Class<T>,
        sourceEntityIds: Collection<KeyT>,
        result: CopyResult<*>,
    ): List<KeyT> {
        @Suppress("UNCHECKED_CAST")
        val baseEntity: Class<out Entity<KeyT>> = entityGraphNavigator.getClosestToEntityAncestor(entity)
                as Class<out Entity<KeyT>>

        val mapping: Map<KeyT, KeyT> = result.getEntityMappings(baseEntity)

        return sourceEntityIds.map { mapping[it]!! }
    }

    fun <T : Entity<KeyT>, KeyT> getAllCopiedEntityIds(
        entity: Class<T>,
        result: CopyResult<*>,
    ): List<KeyT> {
        assumeCopyResultIsSuccessful(result)
        return getAllCopiedEntityIdsWithoutChecks(entity, result)
    }

    private fun <T : Entity<KeyT>, KeyT> getAllCopiedEntityIdsWithoutChecks(
        entity: Class<T>,
        result: CopyResult<*>,
    ): List<KeyT> {
        @Suppress("UNCHECKED_CAST")
        val baseEntity: Class<out Entity<KeyT>> = entityGraphNavigator.getClosestToEntityAncestor(entity)
            as Class<out Entity<KeyT>>

        return result.getEntityMappings(baseEntity).values.mapNotNull { it }.toList()
    }

    fun assertResultContainsAllDefects(
        result: CopyResult<*>,
        defects: Collection<DefectId<*>>
    ) {
        assertThat(result.massResult.validationResult.flattenErrors().map { it.defect.defectId() }
            .containsAll(defects)).isTrue
    }

    fun softlyAssertResultContainsAllDefects(
        result: CopyResult<*>,
        defects: Collection<DefectId<*>>,
        softAssert: SoftAssertions,
    ) {
        softAssert.assertThat(result.massResult.validationResult.flattenErrors().map { it.defect.defectId() }
            .containsAll(defects)).isTrue
    }
}
