package ru.yandex.market.mdm.metadata.service.validation

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityKind
import ru.yandex.market.mdm.lib.model.mdm.MdmValidationError
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class MdmEntityTypeValidatorTest : BaseAppTestClass() {
    lateinit var validator: MdmEntityTypeValidator

    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Before
    fun setUp() {
        validator = MdmEntityTypeValidator(mdmEntityTypeRepository)
    }

    @Test
    fun noAfterYieldsInternalError() {
        assertSoftly {
            validator.validate(null, null) shouldContain error(MdmEntityTypeValidator.NO_ENTITY_TYPE_CODE)
        }
    }

    @Test
    fun internalNameAlreadyExistYieldsInternalError() {
        mdmEntityTypeRepository.insert(mdmEntityType(description = "description", internalName = "internalName",
            ruTitle = "заголовок"))
        assertSoftly {
            validator.validate(null, mdmEntityType(description = "description", internalName = "internalName",
                ruTitle = "заголовок")) shouldContain error(MdmEntityTypeValidator.INTERNAL_NAME_ALREADY_EXIST)
        }
    }

    @Test
    fun validEntityType() {
        assertSoftly {
            validator.validate(null, mdmEntityType(description = "description", internalName = "internalName",
                ruTitle = "заголовок")) shouldBe emptyList() }
    }

    @Test
    fun metaToMetaKindConversionShouldBeValid() {
        val before = mdmEntityType(entityKind = MdmEntityKind.META, description = "desc", internalName = "name1",
            ruTitle = "title1")
        val after = mdmEntityType(entityKind = MdmEntityKind.META, description = "desc", internalName = "name1",
            ruTitle = "title2")

        validator.validate(before, after) shouldBe emptyList()
    }

    @Test
    fun anythingToMetaTypeConversionShouldNotBeValid() {
        val before = mdmEntityType(entityKind = MdmEntityKind.SERVICE, description = "desc", internalName = "name1",
            ruTitle = "title1")
        val after = mdmEntityType(entityKind = MdmEntityKind.META, description = "desc", internalName = "name1",
            ruTitle = "title2")

        validator.validate(before, after) shouldContain error (MdmEntityTypeValidator.WRONG_NEW_ENTITY_KIND)
    }

    @Test
    fun metaEntityCreationShouldBeValid() {
        val after = mdmEntityType(description = "description", internalName = "internalName", ruTitle = "заголовок",
            entityKind = MdmEntityKind.META)

        validator.validate(null, after) shouldBe emptyList()
    }

    private fun error(code: String): MdmValidationError {
        return MdmValidationError(code)
    }
}
