package ru.yandex.market.mdm.metadata.service.validation

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEnumOption
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalReference
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalSystem
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment
import ru.yandex.market.mdm.lib.service.PgSequenceMdmIdGenerator
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEnumOptionRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class MdmExternalReferenceValidatorTest : BaseAppTestClass() {
    @Autowired
    lateinit var attributeRepository: MdmAttributeRepository

    @Autowired
    lateinit var entityTypeRepository: MdmEntityTypeRepository

    @Autowired
    lateinit var optionRepository: MdmEnumOptionRepository

    @Autowired
    lateinit var mdmIdGenerator: PgSequenceMdmIdGenerator

    lateinit var validator: MdmExternalReferenceValidator

    @Before
    fun setUp() {
        validator = MdmExternalReferenceValidator(attributeRepository, entityTypeRepository, optionRepository)
    }

    @Test
    fun `if after is null return error`() {
        // given
        val before = MdmExternalReference(
            mdmPath = MdmPath(),
            externalId = 123,
            externalSystem = MdmExternalSystem.LMS
        )
        val after: MdmExternalReference? = null

        // when
        val validationErrors = validator.validate(before, after)

        // then
        validationErrors shouldHaveSize 1
        validationErrors[0].code shouldBe MdmExternalReferenceValidator.NO_EXTERNAL_REFERENCE_CODE
    }

    @Test
    fun `if after have empty path return error`() {
        // given
        val before: MdmExternalReference? = null
        val after = MdmExternalReference(
            mdmPath = MdmPath(),
            externalId = 123,
            externalSystem = MdmExternalSystem.LMS
        )

        // when
        val validationErrors = validator.validate(before, after)

        // then
        validationErrors shouldHaveSize 1
        validationErrors[0].code shouldBe ValidationsHelper.EMPTY_PATH_CODE
    }

    @Test
    fun `if path have duplicate ids return error`() {
        // given
        val entityType1 = MdmEntityType(
            internalName = "austerlitz",
            ruTitle = "Аустерлиц",
            description = "Battle of the Three Emperors 2 December 1805"
        )
        val entityType3 = MdmEntityType(
            internalName = "auerstedt",
            ruTitle = "Ауэрштедт 1806",
            description = "Louis Nicolas Davout's III Corps"
        )
        entityTypeRepository.insertBatch(listOf(entityType1, entityType3))
        val attribute2 = MdmAttribute(
            dataType = MdmAttributeDataType.STRUCT,
            internalName = "Jena",
            mdmEntityTypeId = entityType1.mdmId,
            ruTitle = "Битва при Йене 1806",
            structMdmEntityTypeId = entityType3.mdmId
        )
        attributeRepository.insert(attribute2)

        val before: MdmExternalReference? = null
        val after = MdmExternalReference(
            mdmPath = MdmPath(
                listOf(
                    MdmPathSegment(
                        mdmId = entityType1.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                    ),
                    MdmPathSegment(
                        mdmId = attribute2.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ATTR
                    ),
                    MdmPathSegment(
                        mdmId = entityType3.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                    ),
                    MdmPathSegment(
                        mdmId = attribute2.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ATTR
                    ),
                )
            ),
            externalId = 123,
            externalSystem = MdmExternalSystem.LMS
        )

        // when
        val validationErrors = validator.validate(before, after).map { it.code }

        // then
        validationErrors shouldContainExactlyInAnyOrder listOf(ValidationsHelper.CIRCLED_PATH_CODE)
    }

    @Test
    fun `if path full and all segments exists return no error`() {
        // given
        val entityType1 = MdmEntityType(
            internalName = "austerlitz",
            ruTitle = "Аустерлиц",
            description = "Battle of the Three Emperors 2 December 1805"
        )
        val entityType3 = MdmEntityType(
            internalName = "auerstedt",
            ruTitle = "Ауэрштедт 1806",
            description = "Louis Nicolas Davout's III Corps"
        )
        entityTypeRepository.insertBatch(listOf(entityType1, entityType3))
        val attribute2 = MdmAttribute(
            dataType = MdmAttributeDataType.STRUCT,
            internalName = "Jena",
            mdmEntityTypeId = entityType1.mdmId,
            ruTitle = "Битва при Йене 1806",
            structMdmEntityTypeId = entityType3.mdmId
        )
        val attribute4 = MdmAttribute(
            dataType = MdmAttributeDataType.ENUM,
            internalName = "preussisch-eylau",
            mdmEntityTypeId = entityType3.mdmId,
            ruTitle = "Битва при Прейсиш-Эйлау 1807"
        )
        attributeRepository.insertBatch(listOf(attribute2, attribute4))
        val option5 = MdmEnumOption(
            mdmAttributeId = attribute4.mdmId,
            value = "Jean-Joseph Ange d'Hautpoul"
        )
        optionRepository.insert(option5)

        val before: MdmExternalReference? = null
        val after = MdmExternalReference(
            mdmPath = MdmPath(
                listOf(
                    MdmPathSegment(
                        mdmId = entityType1.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                    ),
                    MdmPathSegment(
                        mdmId = attribute2.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ATTR
                    ),
                    MdmPathSegment(
                        mdmId = entityType3.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                    ),
                    MdmPathSegment(
                        mdmId = attribute4.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ATTR
                    ),
                    MdmPathSegment(
                        mdmId = option5.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ENUM_OPTION
                    )
                )
            ),
            externalId = 123,
            externalSystem = MdmExternalSystem.MBO
        )

        // when
        val validationErrors = validator.validate(before, after).map { it.code }

        // then
        validationErrors shouldHaveSize 0
    }

    @Test
    fun `if path contains not existing objects return error on each`() {
        // given
        val entityType1 = MdmEntityType(
            internalName = "austerlitz",
            ruTitle = "Аустерлиц",
            description = "Battle of the Three Emperors 2 December 1805"
        )
        val entityType3 = MdmEntityType(
            internalName = "auerstedt",
            ruTitle = "Ауэрштедт 1806",
            description = "Louis Nicolas Davout's III Corps"
        )
        entityTypeRepository.insertBatch(listOf(entityType1, entityType3))
        val attribute2 = MdmAttribute(
            dataType = MdmAttributeDataType.STRUCT,
            internalName = "Jena",
            mdmEntityTypeId = entityType1.mdmId,
            ruTitle = "Битва при Йене 1806",
            structMdmEntityTypeId = entityType3.mdmId
        )
        val attribute4 = MdmAttribute(
            dataType = MdmAttributeDataType.ENUM,
            internalName = "preussisch-eylau",
            mdmEntityTypeId = entityType3.mdmId,
            ruTitle = "Битва при Прейсиш-Эйлау 1807"
        )
        attributeRepository.insertBatch(listOf(attribute2, attribute4))
        val option5 = MdmEnumOption(
            mdmAttributeId = attribute4.mdmId,
            value = "Jean-Joseph Ange d'Hautpoul"
        )
        optionRepository.insert(option5)

        val before: MdmExternalReference? = null
        val unknownId1 = mdmIdGenerator.id
        val unknownId2 = mdmIdGenerator.id
        val after = MdmExternalReference(
            mdmPath = MdmPath(
                listOf(
                    MdmPathSegment(
                        mdmId = entityType1.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                    ),
                    MdmPathSegment(
                        mdmId = attribute2.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ATTR
                    ),
                    MdmPathSegment(
                        mdmId = unknownId1,
                        mdmMetaType = MdmMetaType.MDM_ENTITY_TYPE
                    ),
                    MdmPathSegment(
                        mdmId = attribute4.mdmId,
                        mdmMetaType = MdmMetaType.MDM_ATTR
                    ),
                    MdmPathSegment(
                        mdmId = unknownId2,
                        mdmMetaType = MdmMetaType.MDM_ENUM_OPTION
                    )
                )
            ),
            externalId = 123,
            externalSystem = MdmExternalSystem.MBO
        )

        // when
        val validationErrors = validator.validate(before, after)

        // then
        validationErrors.map { it.code } shouldContainExactly listOf(
            MdmExternalReferenceValidator.PATH_CONTAINS_UNRECOGNIZED_SEGMENT,
            MdmExternalReferenceValidator.PATH_CONTAINS_UNRECOGNIZED_SEGMENT
        )
        validationErrors.map { it.message } shouldContainExactlyInAnyOrder listOf(
            "Неизвестный сегмент ${MdmPathSegment(unknownId1, MdmMetaType.MDM_ENTITY_TYPE)} " +
                "в пути ${after.mdmPath}.",
            "Неизвестный сегмент ${MdmPathSegment(unknownId2, MdmMetaType.MDM_ENUM_OPTION)} " +
                "в пути ${after.mdmPath}."
        )
    }
}
