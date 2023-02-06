package ru.yandex.direct.core.entity.campaign.service.validation.type.update

import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer
import ru.yandex.direct.core.validation.defects.RightsDefects
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.DefectInfo
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult
import javax.annotation.ParametersAreNonnullByDefault

private val VALIDATION_CONTAINER = CampaignValidationContainer.create(0, 1L, ClientId.fromLong(4L))
private const val ORIGINAL_PLACE_ID = 1L
private const val ORIGINAL_IS_MOBILE = true

@ParametersAreNonnullByDefault
class InternalCampaignUpdateValidationTypeSupportTest {

    private val typeSupport = InternalCampaignUpdateValidationTypeSupport()

    @Test
    fun preValidatePlaceIdUnchanged() {
        val campaign = createCampaign()
        val mc = ModelChanges(campaign.id, InternalCampaign::class.java)
        val vr = typeSupport.preValidate(VALIDATION_CONTAINER, ValidationResult(listOf(mc)))
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun preValidatePlaceIdReplacedWithSameValue() {
        val campaign = createCampaign()
        val mc = ModelChanges(campaign.id, InternalCampaign::class.java)
            .process(ORIGINAL_PLACE_ID, InternalCampaign.PLACE_ID)
        val vr = typeSupport.preValidate(VALIDATION_CONTAINER, ValidationResult(listOf(mc)))
        assertThat(vr, hasDefectWithDefinition(validationError(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PLACE_ID)),
            RightsDefects.forbiddenToChange())))
    }

    @Test
    fun preValidateModifiedPlaceId() {
        val campaign = createCampaign()
        val mc = ModelChanges(campaign.id, InternalCampaign::class.java)
            .process(ORIGINAL_PLACE_ID + 1, InternalCampaign.PLACE_ID)
        val vr = typeSupport.preValidate(VALIDATION_CONTAINER, ValidationResult(listOf(mc)))
        assertThat(vr, hasDefectWithDefinition(validationError(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PLACE_ID)),
            RightsDefects.forbiddenToChange())))
    }

    @Test
    fun preValidateIsMobileUnchanged() {
        val campaign = createCampaign()
        val mc = ModelChanges(campaign.id, InternalCampaign::class.java)
        val vr = typeSupport.preValidate(VALIDATION_CONTAINER, ValidationResult(listOf(mc)))
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun preValidateIsMobileReplacedWithSameValue() {
        val campaign = createCampaign()
        val mc = ModelChanges(campaign.id, InternalCampaign::class.java)
            .process(ORIGINAL_IS_MOBILE, InternalCampaign.IS_MOBILE)
        val vr = typeSupport.preValidate(VALIDATION_CONTAINER, ValidationResult(listOf(mc)))
        assertThat(vr, hasDefectWithDefinition(validationError(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.IS_MOBILE)),
            RightsDefects.forbiddenToChange())))
    }

    @Test
    fun preValidateModifiedIsMobile() {
        val campaign = createCampaign()
        val mc = ModelChanges(campaign.id, InternalCampaign::class.java)
            .process(!ORIGINAL_IS_MOBILE, InternalCampaign.IS_MOBILE)
        val vr = typeSupport.preValidate(VALIDATION_CONTAINER, ValidationResult(listOf(mc)))
        assertThat(vr, hasDefectWithDefinition(validationError(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.IS_MOBILE)),
            RightsDefects.forbiddenToChange())))
    }

    @Test
    fun validateIsMobile_WhenIsTrue() {
        val campaign = createCampaign(isMobile = true)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validateIsMobile_WhenIsFalse() {
        val campaign = createCampaign(isMobile = false)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validateIsMobile_WhenIsNull() {
        val campaign = createCampaign(isMobile = null)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasDefectWithDefinition(validationError(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.IS_MOBILE)),
                CommonDefects.notNull())))
    }

    @Test
    fun validatePageId_WhenEmptyList() {
        val campaign = createCampaign(pageIds = emptyList())
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validatePageId_WhenListOfValidIds() {
        val campaign = createCampaign(pageIds = listOf(1L, 2L, 3L))
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validatePageId_WhenNull() {
        val campaign = createCampaign(pageIds = null)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasDefectWithDefinition(validationError(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PAGE_ID)),
                CommonDefects.notNull())))
    }

    @Test
    fun validatePageId_WhenSingletonListOfNull() {
        val campaign = createCampaign(pageIds = listOf<Long?>(null))
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasDefectWithDefinition(validationError(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PAGE_ID), PathHelper.index(0)),
                CommonDefects.notNull())))
    }

    @Test
    fun validatePageId_WhenSingletonListOfInvalidId() {
        val campaign = createCampaign(pageIds = listOf(-3L))
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasDefectWithDefinition(validationError(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PAGE_ID), PathHelper.index(0)),
                CommonDefects.validId())))
    }


    @Test
    fun validatePageId_WhenNullAndInvalidIdInList() {
        val campaign = createCampaign(pageIds = listOf(-3L, null))
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr.flattenErrors(), containsInAnyOrder(listOf(
            equalTo(DefectInfo(
                PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PAGE_ID), PathHelper.index(0)),
                -3L,
                    CommonDefects.validId())),
            equalTo(DefectInfo(
                PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PAGE_ID), PathHelper.index(1)),
                null,
                    CommonDefects.notNull())))))
    }

    @Test
    fun validatePageId_WhenInvalidIdInOtherwiseValidList() {
        val invalidId = -3L
        val campaign = createCampaign(pageIds = listOf(1L, 2L, 3L, invalidId, 4L, 5L))
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr.flattenErrors(), equalTo(listOf(DefectInfo(
            PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PAGE_ID), PathHelper.index(3)),
            invalidId,
                CommonDefects.validId()))))
    }

    private fun createCampaign(placeId: Long? = ORIGINAL_PLACE_ID,
                               isMobile: Boolean? = ORIGINAL_IS_MOBILE,
                               pageIds: List<Long?>? = emptyList()
    ): InternalCampaign {
        return InternalAutobudgetCampaign()
            .withId(RandomNumberUtils.nextPositiveLong())
            .withIsMobile(isMobile)
            .withPageId(pageIds)
            .withPlaceId(placeId)
    }

}
