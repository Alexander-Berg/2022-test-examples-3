package ru.yandex.direct.core.entity.campaign.service.validation.type.update

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAllowedOnAdultContentForbidden
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

class CampaignWithAllowedOnAdultContentForbiddenUpdateValidationTypeSupportTest {

    lateinit var typeSupport: CampaignWithAllowedOnAdultContentForbiddenUpdateValidationTypeSupport

    @Before
    fun setUp() {
        typeSupport = CampaignWithAllowedOnAdultContentForbiddenUpdateValidationTypeSupport()
    }

    @Test
    fun shouldReturnOkIfFlagWasNotChanged() {
        val mc = ModelChanges(RandomNumberUtils.nextPositiveLong(), CampaignWithAllowedOnAdultContentForbidden::class.java)
        val result = typeSupport.preValidate(null, ValidationResult(listOf(mc)))
        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun shouldReturnErrorIfFlagWasChanged() {
        val mc = ModelChanges(RandomNumberUtils.nextPositiveLong(), CampaignWithAllowedOnAdultContentForbidden::class.java)
            .process(true, CampaignWithAllowedOnAdultContentForbidden.IS_ALLOWED_ON_ADULT_CONTENT)
        val result = typeSupport.preValidate(null, ValidationResult(listOf(mc)))
        assertThat(result).`is`(matchedBy(hasDefectWithDefinition<Any>(
            validationError(
                path(index(0), field(CampaignWithAllowedOnAdultContentForbidden.IS_ALLOWED_ON_ADULT_CONTENT)),
                Defect(DefectIds.FORBIDDEN_TO_CHANGE)
            )
        )))
    }
}
