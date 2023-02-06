package ru.yandex.direct.grid.processing.service.conversioncenter.validation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Test
import ru.yandex.direct.grid.processing.model.goal.GdConversionSourceSettingsUnion
import ru.yandex.direct.grid.processing.model.goal.GdConversionSourceTypeCode
import ru.yandex.direct.grid.processing.model.goal.GdLinkConversionSourceSettings
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSource
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSourceConversionAction
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSourceConversionActionValue
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSourceConversionActionValueType
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectInfo
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult
import ru.yandex.direct.validation.util.D

class ConversionCenterValidationServiceTest {
    private val validationService = ConversionCenterValidationService()

    @Test
    fun validateUpdateConversionSourceInput() {
        val validInput = validLinkGdUpdateConversionSource()

        val vr = validationService.validateUpdateConversionSourceInput(validInput)

        assertThat(vr).doesNotContainAnyErrors()
    }

    @Test
    fun validateUpdateConversionSourceInput_UnsupportedType() {
        val validInput = validLinkGdUpdateConversionSource().withType(GdConversionSourceTypeCode.FILE)

        val vr = validationService.validateUpdateConversionSourceInput(validInput)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("type")), CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION)
        )
    }

    @Test
    fun validateUpdateConversionSourceInput_WrongSettings() {
        val validInput = validLinkGdUpdateConversionSource()
            .withSettings(GdConversionSourceSettingsUnion())

        val vr = validationService.validateUpdateConversionSourceInput(validInput)

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("settings")), invalidValue())
        )
    }

    private fun validLinkGdUpdateConversionSource() = GdUpdateConversionSource()
        .withType(GdConversionSourceTypeCode.LINK)
        .withName("xxx")
        .withSettings(
            GdConversionSourceSettingsUnion()
                .withLinkSettings(
                    GdLinkConversionSourceSettings()
                        .withUrl("https://example.com/some-path-to-file")
                )
        )
        .withConversionActions(
            listOf(
                GdUpdateConversionSourceConversionAction()
                    .withName("some action name 1")
                    .withValue(
                        GdUpdateConversionSourceConversionActionValue()
                            .withValueType(GdUpdateConversionSourceConversionActionValueType.NOT_SET)
                    ),
            )
        )
        .withCounterId(3234234L)
        .withUpdateFileReminder(true)
        .withUpdateFileReminderDaysCount(2)
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.doesNotContainAnyErrors() {
    extracting { it.flattenErrors() }.asList().isEmpty()
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.containsExactlyErrors(
    vararg matchers: Matcher<DefectInfo<Defect<Any>>>
) {
    extracting { it.flattenErrors() }.asList().`is`(
        Conditions.matchedBy(Matchers.containsInAnyOrder(*matchers))
    )
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.doesNotContainErrors(
    vararg matchers: Matcher<DefectInfo<Defect<Any>>>
) {
    extracting { it.flattenErrors() }.asList().`is`(
        Conditions.matchedBy(Matchers.not(Matchers.hasItems(*matchers)))
    )
}
