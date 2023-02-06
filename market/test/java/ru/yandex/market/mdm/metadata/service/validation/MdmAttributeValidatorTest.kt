package ru.yandex.market.mdm.metadata.service.validation

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityKind
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmValidationError
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class MdmAttributeValidatorTest: BaseAppTestClass() {
    companion object {
        const val ENTITY_TYPE_ID = 12L
        const val OTHER_ENTITY_TYPE_ID = 13L
    }

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository
    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository
    lateinit var validator: MdmAttributeValidator

    @Before
    fun setup() {
        validator = MdmAttributeValidator(mdmEntityTypeRepository, mdmAttributeRepository)

        mdmEntityTypeRepository.insert(
            MdmEntityType(ENTITY_TYPE_ID, internalName = "some_ET", description = "", ruTitle = "")
        )
        mdmEntityTypeRepository.insert(
            MdmEntityType(OTHER_ENTITY_TYPE_ID, internalName = "other_ET", description = "", ruTitle = "")
        )
    }

    @Test
    fun noAfterYieldsInternalError() {
        assertSoftly {
            validator.validate(null, null) shouldContain error(MdmAttributeValidator.NO_ATTRIBUTE_CODE)
        }
    }

    @Test
    fun noEntityTypeIdYieldsTypedError() {
        assertSoftly {
            validator.validate(null, attribute(-5L, "some_name")) shouldContain
                error(MdmAttributeValidator.NO_ENTITY_TYPE_ID_CODE)

            validator.validate(null, attribute(0L, "some_name")) shouldContain
                error(MdmAttributeValidator.NO_ENTITY_TYPE_ID_CODE)
        }
    }

    @Test
    fun deletedEntityTypeIdYieldsTypedError() {
        mdmEntityTypeRepository.retireLatestById(ENTITY_TYPE_ID)

        assertSoftly {
            validator.validate(null, attribute(ENTITY_TYPE_ID, "some_name")) shouldContain
                error(MdmAttributeValidator.MISSING_ENTITY_TYPE_CODE)
        }
    }

    @Test
    fun wrongEntityTypeIdYieldsTypedError() {
        val nonExistentEtId = OTHER_ENTITY_TYPE_ID + 10000
        assertSoftly {
            validator.validate(null, attribute(nonExistentEtId, "some_name")) shouldContain
                error(MdmAttributeValidator.MISSING_ENTITY_TYPE_CODE)
        }
    }

    @Test
    fun noInternalNameYieldsTypedError() {
        assertSoftly {
            validator.validate(null, attribute(ENTITY_TYPE_ID, " \t \n ")) shouldContain
                error(ValidationsHelper.EMPTY_INTERNAL_NAME_CODE)

            validator.validate(null, attribute(ENTITY_TYPE_ID, " ")) shouldContain
                error(ValidationsHelper.EMPTY_INTERNAL_NAME_CODE)
        }
    }

    @Test
    fun wrongInternalNameYieldsTypedError() {
        assertSoftly {
            validator.validate(null, attribute(ENTITY_TYPE_ID, " \txyz\n ")) shouldContain
                error(ValidationsHelper.MALFORMED_INTERNAL_NAME_CODE)

            validator.validate(null, attribute(ENTITY_TYPE_ID, "xyzйцукен")) shouldContain
                error(ValidationsHelper.MALFORMED_INTERNAL_NAME_CODE)

            validator.validate(null, attribute(ENTITY_TYPE_ID, "xyz  ")) shouldContain
                error(ValidationsHelper.MALFORMED_INTERNAL_NAME_CODE)

            validator.validate(null, attribute(ENTITY_TYPE_ID, "x@y")) shouldContain
                error(ValidationsHelper.MALFORMED_INTERNAL_NAME_CODE)

            validator.validate(null,
                attribute(ENTITY_TYPE_ID, "x".repeat(ValidationsHelper.INTERNAL_NAME_LEN_MAX + 1))) shouldContain
                error(ValidationsHelper.LONG_INTERNAL_NAME_CODE)
        }
    }

    @Test
    fun nonUniqueNameWithinSameTypeYieldsError() {
        mdmAttributeRepository.insert(attribute(ENTITY_TYPE_ID, "radius_cm"))

        assertSoftly {
            validator.validate(null, attribute(ENTITY_TYPE_ID, "radius_cm")) shouldContain
                error(MdmAttributeValidator.DUPE_NAME_CODE)
        }
    }

    @Test
    fun nonUniqueNameInOtherTypeIsOk() {
        mdmAttributeRepository.insert(attribute(OTHER_ENTITY_TYPE_ID, "radius_cm"))

        assertSoftly {
            validator.validate(null, attribute(ENTITY_TYPE_ID, "radius_cm")) shouldBe emptyList()
        }
    }

    @Test
    fun validNewAttributeIsOk() {
        assertSoftly {
            validator.validate(null, attribute(ENTITY_TYPE_ID, "radius_cm")) shouldBe emptyList()
        }
    }

    @Test
    fun validUpdateExistingAttributeIsOk() {
        val existing = attribute(ENTITY_TYPE_ID, "xyz")
        val update = mdmAttributeRepository.insert(existing).copy(ruTitle = "yay")

        assertSoftly {
            validator.validate(existing, update) shouldBe emptyList()
        }
    }

    @Test
    fun shouldReturnErrorForComplicatedMetaArgumentCreation() {
        val metaEntityType = createMetaEntityType()
        val createdAttribute = attribute(metaEntityType.mdmId, "xyz", MdmAttributeDataType.STRUCT)

        validator.validate(null, createdAttribute) shouldContain error(MdmAttributeValidator.INVALID_DATA_TYPE)
    }

    @Test
    fun shouldReturnOkForSimpleMetaArgumentCreation() {
        val metaEntityType = createMetaEntityType()
        val createdAttribute = attribute(metaEntityType.mdmId, "xyz")

        validator.validate(null, createdAttribute) shouldBe emptyList()
    }

    @Test
    fun shouldReturnErrorForComplicatedMetaArgumentUpdate() {
        val metaEntityType = createMetaEntityType()
        val existingAttribute = attribute(metaEntityType.mdmId, "xyz")
        val updatedAttribute = mdmAttributeRepository.insert(existingAttribute).copy(
            dataType = MdmAttributeDataType.STRUCT
        )

        validator.validate(existingAttribute, updatedAttribute) shouldContain
            error(MdmAttributeValidator.INVALID_DATA_TYPE)
    }

    @Test
    fun shouldReturnOkForSimpleMetaArgumentUpdate() {
        val metaEntityType = createMetaEntityType()
        val existingAttribute = attribute(metaEntityType.mdmId, "xyz")
        val updatedAttribute = mdmAttributeRepository.insert(existingAttribute).copy(ruTitle = "yay")

        validator.validate(existingAttribute, updatedAttribute) shouldBe emptyList()
    }

    private fun attribute(
        etId: Long,
        internalName: String,
        dataType: MdmAttributeDataType = MdmAttributeDataType.NUMERIC
    ): MdmAttribute =
        mdmAttribute(mdmEntityTypeId = etId, internalName = internalName, ruTitle = internalName, dataType = dataType)

    private fun createMetaEntityType() =
        mdmEntityType(entityKind = MdmEntityKind.META).let {
            mdmEntityTypeRepository.insert(it)
        }

    private infix fun error(code: String): MdmValidationError {
        return MdmValidationError(code)
    }
}
