package ru.yandex.direct.core.entity.campaign.service.validation.type.add

import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.doReturn
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign
import ru.yandex.direct.core.entity.campaign.model.InternalCampaign
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer
import ru.yandex.direct.core.entity.internalads.model.InternalAdsOperatorProductAccess
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct
import ru.yandex.direct.core.entity.internalads.service.InternalAdsOperatorProductAccessService
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.DefectInfo
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult
import javax.annotation.ParametersAreNonnullByDefault

private const val OPERATOR_UID = 1L
private val PRODUCT_CLIENT_ID = ClientId.fromLong(4L)
private val VALIDATION_CONTAINER = CampaignValidationContainer.create(0, OPERATOR_UID, PRODUCT_CLIENT_ID)
private val INTERNAL_ADS_PRODUCT = InternalAdsProduct().withName("product-app")

@RunWith(MockitoJUnitRunner::class)
@ParametersAreNonnullByDefault
class InternalCampaignAddValidationTypeSupportTest {

    @Mock
    private lateinit var internalAdsOperatorProductAccess: InternalAdsOperatorProductAccess

    @Mock
    private lateinit var internalAdsOperatorProductAccessService: InternalAdsOperatorProductAccessService

    @Mock
    private lateinit var internalAdsProductService: InternalAdsProductService

    @InjectMocks
    private lateinit var typeSupport: InternalCampaignAddValidationTypeSupport

    @Before
    fun setUp() {
        doReturn(true)
                .`when`(internalAdsOperatorProductAccess).hasWriteAccessToPlace(anyLong())
        doReturn(internalAdsOperatorProductAccess)
                .`when`(internalAdsOperatorProductAccessService)
                .getAccess(VALIDATION_CONTAINER.operatorUid, VALIDATION_CONTAINER.clientId)
        doReturn(INTERNAL_ADS_PRODUCT)
                .`when`(internalAdsProductService).getProduct(PRODUCT_CLIENT_ID)
    }


    @Test
    fun validateSuccess() {
        val campaign = createCampaign()
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validatePlaceId_IsNull() {
        val campaign = createCampaign(placeId = null)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasDefectWithDefinition(validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PLACE_ID)),
                CommonDefects.notNull())))
    }

    @Test
    fun validatePlaceId_IsInvalidId() {
        val campaign = createCampaign(placeId = -3)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasDefectWithDefinition(validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.PLACE_ID)),
                CommonDefects.validId())))
    }

    @Test
    fun validateIsMobile_WhenIsTrueAndProductIsMobile() {
        val campaign = createCampaign(isMobile = true)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasNoDefectsDefinitions())
    }

    @Test
    fun validateIsMobile_WhenIsFalseAndProductIsMobile() {
        val campaign = createCampaign(isMobile = false)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasDefectWithDefinition(validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.IS_MOBILE)),
                CommonDefects.inconsistentState())))
    }

    @Test
    fun validateIsMobile_WhenIsTrueAndProductIsNotMobile() {
        doReturn(InternalAdsProduct().withName("product"))
                .`when`(internalAdsProductService).getProduct(PRODUCT_CLIENT_ID)
        val campaign = createCampaign(isMobile = true)
        val vr = typeSupport.validate(VALIDATION_CONTAINER, ValidationResult(listOf(campaign)))
        assertThat(vr, hasDefectWithDefinition(validationError(
                PathHelper.path(PathHelper.index(0), PathHelper.field(InternalCampaign.IS_MOBILE)),
                CommonDefects.inconsistentState())))
    }

    @Test
    fun validateIsMobile_WhenIsFalseAndProductIsNotMobile() {
        doReturn(InternalAdsProduct().withName("product"))
                .`when`(internalAdsProductService).getProduct(PRODUCT_CLIENT_ID)
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
        val campaign = createCampaign(pageIds = listOf(null))
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

    private fun createCampaign(placeId: Long? = RandomNumberUtils.nextPositiveLong(),
                               isMobile: Boolean? = true,
                               pageIds: List<Long?>? = emptyList()
    ): InternalCampaign {
        return InternalAutobudgetCampaign()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withIsMobile(isMobile)
                .withPageId(pageIds)
                .withPlaceId(placeId)
    }

}
