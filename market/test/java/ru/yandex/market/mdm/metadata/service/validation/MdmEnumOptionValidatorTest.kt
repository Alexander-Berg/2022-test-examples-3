package ru.yandex.market.mdm.metadata.service.validation

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmEnumOption
import ru.yandex.market.mdm.lib.model.mdm.MdmValidationError
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEnumOptionRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class MdmEnumOptionValidatorTest : BaseAppTestClass() {

    lateinit var validator: MdmEnumOptionValidator

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository
    @Autowired
    lateinit var mdmEnumOptionRepository: MdmEnumOptionRepository

    @Before
    fun setUp() {
        validator = MdmEnumOptionValidator(mdmAttributeRepository, mdmEnumOptionRepository)
        mdmAttributeRepository.insert(attribute(123, "111_attr"))
    }

    @Test
    fun noAfterYieldsInternalError() {
        assertSoftly {
            validator.validate(null, null) shouldContain
                error(MdmEnumOptionValidator.NO_ENUM_OPTION_CODE)
        }
    }

    @Test
    fun noAttributeIdYieldsTypedError() {
        assertSoftly {
            validator.validate(
                null,
                enumOption(-5L)
            ) shouldContain error(MdmEnumOptionValidator.NO_ATTRIBUTE_ID_CODE)

            validator.validate(
                null,
                enumOption(0L)
            ) shouldContain error(MdmEnumOptionValidator.NO_ATTRIBUTE_ID_CODE)
        }
    }

    @Test
    fun missingAttributeYieldsTypedError() {
        assertSoftly {
            validator.validate(null, enumOption(111)
            ) shouldContain error(MdmEnumOptionValidator.MISSING_ATTRIBUTE_CODE)
        }
    }

    @Test
    fun enumOptionAlreadyExistYieldsTypedError() {
        val attr = mdmAttributeRepository.findAll()[0]
        mdmEnumOptionRepository.insert(enumOption(attr.mdmId))
        assertSoftly {
            validator.validate(null, enumOption(attr.mdmId)
            ) shouldContain error(MdmEnumOptionValidator.ENUM_OPTION_ALREADY_EXIST)
        }
    }

    @Test
    fun validEnumOption() {
        val attr = mdmAttributeRepository.findAll()[0]
        assertSoftly {
            validator.validate(null, enumOption(attr.mdmId)) shouldBe emptyList()
        }
    }

    private fun error(code: String): MdmValidationError {
        return MdmValidationError(code)
    }

    private fun attribute(etId: Long, internalName: String): MdmAttribute {
        return MdmAttribute(
            mdmEntityTypeId = etId,
            internalName = internalName,
            ruTitle = internalName,
            dataType = MdmAttributeDataType.NUMERIC
        )
    }

    private fun enumOption(mdmAttributeId: Long): MdmEnumOption {
        return MdmEnumOption(
            mdmAttributeId = mdmAttributeId,
            value = "true"
        )
    }
}
