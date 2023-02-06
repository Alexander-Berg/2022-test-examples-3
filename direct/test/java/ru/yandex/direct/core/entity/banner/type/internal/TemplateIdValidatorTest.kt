package ru.yandex.direct.core.entity.banner.type.internal

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.MODERATED_TEMPLATE_ID
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.path

@RunWith(JUnitParamsRunner::class)
class TemplateIdValidatorTest {

    fun testData() = listOf(
            listOf(setOf(1L), false, 1L, null),

            listOf(setOf(1L), false, null, CommonDefects.notNull()),
            listOf(setOf(1L), false, -1L, CommonDefects.validId()),
            listOf(setOf(1L), false, 2L, BannerDefects.internalTemplateNotAllowed()),

            listOf(setOf(MODERATED_TEMPLATE_ID), true, MODERATED_TEMPLATE_ID, null),
            listOf(setOf(MODERATED_TEMPLATE_ID), false, MODERATED_TEMPLATE_ID,
                    BannerDefects.internalTemplateNotAllowed()),
            listOf(setOf(1L), true, 1L, BannerDefects.internalTemplateNotAllowed()),
    )

    @Test
    @Parameters(method = "testData")
    @TestCaseName("checkTemplateIdValidator for templateId={2} with allowedTemplateIds={0}"
            + " and isModeratedPlace={1}, expectedDefect={3}")
    fun checkTemplateIdValidator(allowedTemplateIds: Set<Long>, isModeratedPlace: Boolean,
                                 templateId: Long?,
                                 expectedDefect: Defect<Void>?) {
        val validator = TemplateIdValidator.templateIdValidator(allowedTemplateIds, isModeratedPlace)
        val validationResult = validator.apply(templateId)

        if (expectedDefect == null) {
            assertThat(validationResult, hasNoErrorsAndWarnings())
        } else {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(path(), expectedDefect)))
        }
    }

}
