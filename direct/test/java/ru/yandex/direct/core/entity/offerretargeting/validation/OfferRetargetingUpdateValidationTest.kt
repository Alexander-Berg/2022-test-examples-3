package ru.yandex.direct.core.entity.offerretargeting.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification
import ru.yandex.direct.core.entity.offerretargeting.container.OfferRetargetingUpdateContainer
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.entity.offerretargeting.validation.OfferRetargetingDefects.offerRetargetingAlreadySuspended
import ru.yandex.direct.core.entity.offerretargeting.validation.OfferRetargetingDefects.offerRetargetingCantBeUsedInAutoBudgetCampaign
import ru.yandex.direct.core.entity.offerretargeting.validation.OfferRetargetingDefects.offerRetargetingNotSuspended
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.validation.defects.RightsDefects.noRights
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult
import java.math.BigDecimal
import java.time.LocalDateTime

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingUpdateValidationTest : OfferRetargetingOperationBaseTest() {
    @Autowired
    private lateinit var offerRetargetingValidationService: OfferRetargetingValidationService

    @Autowired
    private lateinit var testCampaignRepository: TestCampaignRepository

    @Test
    fun preValidate_Success() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        val modelChanges = ModelChanges(savedOfferRetargeting.id, OfferRetargeting::class.java)
        modelChanges.process(activeCampaignId, OfferRetargeting.CAMPAIGN_ID)

        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.preValidateUpdateOfferRetargetings(
            listOf(modelChanges), container
        )

        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun preValidate_OfferRetargetingCantBeUsedInAutoBudgetCompany_error() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        val modelChanges = ModelChanges(savedOfferRetargeting.id, OfferRetargeting::class.java)
        modelChanges.process(BigDecimal.ONE, OfferRetargeting.PRICE)
        modelChanges.process(activeCampaignId, OfferRetargeting.CAMPAIGN_ID)

        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.preValidateUpdateOfferRetargetings(
            listOf(modelChanges), container
        )
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0)), offerRetargetingCantBeUsedInAutoBudgetCampaign())
                )
            )
        )
    }

    @Test
    fun preValidate_2SameOfferRetargetingInOneOperation_error() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        val modelChanges = ModelChanges(savedOfferRetargeting.id, OfferRetargeting::class.java)
        modelChanges.process(BigDecimal.ONE, OfferRetargeting.PRICE)
        modelChanges.process(activeCampaignId, OfferRetargeting.CAMPAIGN_ID)

        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.preValidateUpdateOfferRetargetings(
            listOf(modelChanges, modelChanges), container
        )
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0)), duplicatedElement())
                )
            )
        )
    }

    @Test
    fun preValidate_NotExistingIds_error() {
        val modelChanges = ModelChanges(NOT_EXISTING_OFFER_RETARGETING_ID, OfferRetargeting::class.java)

        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.preValidateUpdateOfferRetargetings(
            listOf(modelChanges), container
        )
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0), field("id")), objectNotFound())
                )
            )
        )
    }

    @Test
    fun preValidate_SuspendAlreadySuspended_WarningNotSuspendedOfferRetargeting() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0, defaultOfferRetargeting.withIsSuspended(true))
        val modelChanges = ModelChanges(savedOfferRetargeting.id, OfferRetargeting::class.java)
        modelChanges.process(true, OfferRetargeting.IS_SUSPENDED)

        val container = createOfferRetargetingUpdateOperationContainer()
        val vr = offerRetargetingValidationService.preValidateUpdateOfferRetargetings(
            listOf(modelChanges), container
        )

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(index(0), field("id")), offerRetargetingAlreadySuspended())
                )
            )
        )
    }

    @Test
    fun preValidate_ResumeNotSuspended_WarningNotSuspendedOfferRetargeting() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0, defaultOfferRetargeting.withIsSuspended(false))
        val modelChanges = ModelChanges(savedOfferRetargeting.id, OfferRetargeting::class.java)
        modelChanges.process(false, OfferRetargeting.IS_SUSPENDED)

        val container = createOfferRetargetingUpdateOperationContainer()
        val vr = offerRetargetingValidationService.preValidateUpdateOfferRetargetings(
            listOf(modelChanges), container
        )

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(index(0), field("id")), offerRetargetingNotSuspended())
                )
            )
        )
    }

    @Test
    fun validate_Success() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(
            listOf(savedOfferRetargeting)
        )
        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.validateUpdateOfferRetargetings(
            preValidationResult, container
        )
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_NotVisibleCampaignAccessChecker_ObjectNotFound() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        defaultUser = steps.clientSteps().createDefaultClient().chiefUserInfo!!
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(listOf(savedOfferRetargeting))

        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.validateUpdateOfferRetargetings(
            preValidationResult,
            container
        )
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0), field("adGroupId")), objectNotFound())
                )
            )
        )
    }

    @Test
    fun validate_NotWritableCampaignAccessChecker_NoRights() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        defaultUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER).chiefUserInfo!!
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(listOf(savedOfferRetargeting))

        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.validateUpdateOfferRetargetings(
            preValidationResult,
            container
        )
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0), field("adGroupId")), noRights())
                )
            )
        )
    }

    @Test
    fun validate_ArchivedCampaignAccessChecker_ArchivedCampaignModification() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        testCampaignRepository.archiveCampaign(shard, activeCampaignId)
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(listOf(savedOfferRetargeting))

        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.validateUpdateOfferRetargetings(
            preValidationResult,
            container
        )
        assertThat(actual).`is`(
            matchedBy(
                hasDefectWithDefinition<Any>(
                    validationError(path(index(0), field("adGroupId")), archivedCampaignModification())
                )
            )
        )
    }

    @Test
    fun validate_OfferRetargetingCategories_Success() {
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(listOf())
        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.validateUpdateOfferRetargetings(
            preValidationResult,
            container
        )
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_OfferRetargetingCategories_Empty_Success() {
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(listOf())
        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.validateUpdateOfferRetargetings(
            preValidationResult,
            container
        )
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_OfferRetargetingCategories_Null_Success() {
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(emptyList())
        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.validateUpdateOfferRetargetings(
            preValidationResult,
            container
        )
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_OfferRetargetingCategories_Empty_CampaignTypeNotSupported_Success() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        campaignsByIds[savedOfferRetargeting.campaignId]!!.withType(CampaignType.MOBILE_CONTENT)
        val preValidationResult = ValidationResult<List<OfferRetargeting>, Defect<*>>(
            listOf()
        )
        val container = createOfferRetargetingUpdateOperationContainer()
        val actual = offerRetargetingValidationService.validateUpdateOfferRetargetings(
            preValidationResult,
            container
        )
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    private fun createOfferRetargetingUpdateOperationContainer(): OfferRetargetingUpdateContainer {
        return OfferRetargetingUpdateContainer(
            operatorUid,
            clientId,
            clientUid,
            currency,
            campaignsByIds,
            campaignIdsByAdGroupIds,
            adGroupIdsByOfferRetargetingIds,
            offerRetargetingsByIds
        )
    }

    override val defaultOfferRetargeting: OfferRetargeting =
        OfferRetargeting()
            .withLastChangeTime(LocalDateTime.now())
            .withIsSuspended(false)
            .withStatusBsSynced(StatusBsSynced.NO)

    companion object {
        private const val NOT_EXISTING_OFFER_RETARGETING_ID = 9999L
    }
}
