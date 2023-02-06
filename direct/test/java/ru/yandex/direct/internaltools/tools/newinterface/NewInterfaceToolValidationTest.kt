package ru.yandex.direct.internaltools.tools.newinterface

import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.internaltools.configuration.InternalToolsTest
import ru.yandex.direct.internaltools.tools.newinterface.NewInterfaceAlertTool.ALERT_TEXT_EN_FIELD
import ru.yandex.direct.internaltools.tools.newinterface.NewInterfaceAlertTool.ALERT_TEXT_RU_FIELD
import ru.yandex.direct.internaltools.tools.newinterface.NewInterfaceAlertTool.CLIENT_IDS_FIELD
import ru.yandex.direct.internaltools.tools.newinterface.model.NewInterfaceAlertParameters
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.invalidValue
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.StringDefects.notEmptyString
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner::class)
class NewInterfaceToolValidationTest {

    @Autowired
    private lateinit var newInterfaceAlertTool: NewInterfaceAlertTool

    private fun getParam(
        enableAlert: Boolean,
        alertTextRu: String?,
        alertTextEn: String?,
        clientIds: String?
    ): NewInterfaceAlertParameters {
        val param = NewInterfaceAlertParameters()
        param.enableAlert = enableAlert
        param.alertTextRu = alertTextRu
        param.alertTextEn = alertTextEn
        param.clientIds = clientIds
        return param
    }

    @Test
    fun checkValidate_noDefects() {
        val param = getParam(true, "какой-то текст", "some text", "123")
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasNoDefectsDefinitions())
    }

    @Test
    fun checkValidate_noDefects_moreClienIds() {
        val param =
            getParam(true, "какой-то текст", "some text", "1,23,456,7890,12345,678901,2345678,90123456,789012345")
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasNoDefectsDefinitions())
    }

    @Test
    fun checkValidate_noClientIds_noDefect() {
        val param = getParam(true, "какой-то текст", "some text", null)
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasNoDefectsDefinitions())
    }

    @Test
    fun checkValidate_spaceInClientIds_noDefect() {
        val param = getParam(true, "какой-то текст", "some text", "122, 123, 125")
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasNoDefectsDefinitions())
    }

    @Test
    fun checkValidate_notEnabled_noDefect() {
        val param = getParam(false, null, null, null)
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasNoDefectsDefinitions())
    }

    @Test
    fun checkValidate_nullAlertRuText() {
        val param = getParam(true, null, "some text", "123")
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasDefectDefinitionWith(validationError(path(field(ALERT_TEXT_RU_FIELD)), notNull())))
    }

    @Test
    fun checkValidate_nullAlertEnText() {
        val param = getParam(true, "какой-то текст", null, "123")
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasDefectDefinitionWith(validationError(path(field(ALERT_TEXT_EN_FIELD)), notNull())))
    }

    @Test
    fun checkValidate_emptyAlertRuText() {
        val param = getParam(true, "", "some text", "123")
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasDefectDefinitionWith(validationError(path(field(ALERT_TEXT_RU_FIELD)), notEmptyString())))
    }

    @Test
    fun checkValidate_emptyAlertEnText() {
        val param = getParam(true, "какой-то текст", "", "123")
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasDefectDefinitionWith(validationError(path(field(ALERT_TEXT_EN_FIELD)), notEmptyString())))
    }

    @Test
    fun checkValidate_notNumberInClientIds() {
        val param = getParam(true, "какой-то текст", "some text", "12a3")
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasDefectDefinitionWith(validationError(path(field(CLIENT_IDS_FIELD)), invalidValue())))
    }

    @Test
    fun checkValidate_maxLongInClientIds_noDefect() {
        val maxLong = Long.MAX_VALUE.toString()
        val param = getParam(true, "какой-то текст", "some text", maxLong)
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasNoDefectsDefinitions())
    }

    @Test
    fun checkValidate_tooBigNumberInClientIds() {
        val maxLongPlusOne = Long.MAX_VALUE.toULong().plus(1U).toString()
        val param = getParam(true, "какой-то текст", "some text", maxLongPlusOne)
        val result: ValidationResult<NewInterfaceAlertParameters, Defect<*>> = newInterfaceAlertTool.validate(param)
        assertThat(result, hasDefectDefinitionWith(validationError(path(field(CLIENT_IDS_FIELD)), invalidValue())))
    }
}
