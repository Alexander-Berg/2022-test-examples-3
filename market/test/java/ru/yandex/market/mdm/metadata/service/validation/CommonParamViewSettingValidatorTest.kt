package ru.yandex.market.mdm.metadata.service.validation

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.commonParamViewSetting
import ru.yandex.market.mdm.fixtures.commonViewType
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmVersionRetiredNDaysAgo
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmValidationError
import ru.yandex.market.mdm.metadata.repository.CommonViewTypeRepository
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.service.validation.CommonParamViewSettingValidator.Companion.NO_SETTING_CODE
import ru.yandex.market.mdm.metadata.service.validation.CommonParamViewSettingValidator.Companion.NO_VIEW_TYPE_EXIST_CODE
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class CommonParamViewSettingValidatorTest : BaseAppTestClass() {

    lateinit var validator: CommonParamViewSettingValidator

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Autowired
    lateinit var commonViewTypeRepository: CommonViewTypeRepository

    @Before
    fun setup() {
        validator = CommonParamViewSettingValidator(commonViewTypeRepository, mdmAttributeRepository)
    }

    @Test
    fun `should have error when no data for update presents`() {
        assertSoftly {
            validator.validate(null, null) shouldContain error(NO_SETTING_CODE)
        }
    }

    @Test
    fun `should have error when no actual common view type in update`() {
        // given
        val retiredCommonViewType =
            commonViewTypeRepository.insert(commonViewType(version = mdmVersionRetiredNDaysAgo(5)))
        val newCommonParamViewSetting = commonParamViewSetting(commonViewTypeId = retiredCommonViewType.mdmId)

        // when
        val errors = validator.validate(null, after = newCommonParamViewSetting)

        // then
        assertSoftly {
            errors shouldContain error(NO_VIEW_TYPE_EXIST_CODE)
        }
    }

    @Test
    fun `should have error when settings for no actual attribute`() {
        // given
        val actualCommonViewType = commonViewTypeRepository.insert(commonViewType())
        val retiredAttribute = mdmAttributeRepository.insert(mdmAttribute(version = mdmVersionRetiredNDaysAgo(5)))
        val newCommonParamViewSetting = commonParamViewSetting(
            commonViewTypeId = actualCommonViewType.mdmId,
            commonParamId = retiredAttribute.mdmId,
        )
        // when
        val errors = validator.validate(null, after = newCommonParamViewSetting)

        // then
        assertSoftly {
            errors shouldContain error(CommonParamViewSettingValidator.MISSING_ATTRIBUTE_CODE)
        }
    }

    private fun error(code: String): MdmValidationError {
        return MdmValidationError(code)
    }

    @Test
    fun `should have no errors for deprecated view types`() {
        // given
        val actualCommonViewType = commonViewTypeRepository.insert(
            commonViewType(commonEntityTypeEnum = CommonEntityTypeEnum.SSKU)
        )
        val retiredAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val newCommonParamViewSetting = commonParamViewSetting(
            commonViewTypeId = actualCommonViewType.mdmId,
            commonParamId = retiredAttribute.mdmId,
        )
        // when
        val errors = validator.validate(null, after = newCommonParamViewSetting)

        // then
        errors shouldBe emptyList()
    }
}
