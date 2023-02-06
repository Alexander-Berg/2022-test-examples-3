package ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldHave
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntity
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.MdmEntity
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValue
import ru.yandex.market.mdm.service.common_entity.service.arm.flatMdmMetadataWithOneStructAttribute

class FlatMdmMetadataTest {

    @Test
    fun `should transform flat metadata to rich attributes`() {
        // given
        val mdmEntityTypeOuter = mdmEntityType()
        val mdmEntityTypeInner = mdmEntityType()
        val mdmAttributeInner = mdmAttribute(
            internalName = "internal",
            mdmEntityTypeId = mdmEntityTypeInner.mdmId,
        )

        mdmEntityTypeInner.attributes = listOf(mdmAttributeInner)
        val mdmMetadata = flatMdmMetadataWithOneStructAttribute(
            baseEntityType = mdmEntityTypeOuter,
            structEntityType = mdmEntityTypeInner,
            structAttributeName = "outer"
        )

        // when
        val attributes = mdmMetadata.getRichAttributes()

        // then
        attributes shouldHaveSize 1
        attributes.first().toAttribute().internalName shouldBe "outer||internal"
        attributes.first().getSettings().shouldBeEmpty()
    }

    @Test
    fun `should convert entity to flat common entity and back`() {
        // given
        val baseMdmId = 111L // the same value for all inner and outer entities because don't use
        val baseVersion = MdmVersion() // the same value for all inner and outer entities because don't use
        val internalEntityTypeSimpleAttribute =
            mdmAttribute(internalName = "internalSimple", dataType = MdmAttributeDataType.INT64)
        val internalEntityTypeSimpleNotPresentedAttribute =
            mdmAttribute(internalName = "internalNotPresent", dataType = MdmAttributeDataType.INT64)
        val outerEntityTypeSimpleAttribute =
            mdmAttribute(internalName = "outerSimple", dataType = MdmAttributeDataType.BOOLEAN)
        val internalEntityType = mdmEntityType(
            attributes = listOf(
                internalEntityTypeSimpleAttribute,
                internalEntityTypeSimpleNotPresentedAttribute
            )
        )
        val outerEntityTypeStructAttribute = mdmAttribute(
            internalName = "outerStruct",
            dataType = MdmAttributeDataType.STRUCT,
            structMdmEntityTypeId = internalEntityType.mdmId
        )
        val outerEntityType =
            mdmEntityType(
                mdmId = 1,
                attributes = listOf(outerEntityTypeSimpleAttribute, outerEntityTypeStructAttribute)
            )
        val metadata = FlatMdmMetadata(
            outerEntityType,
            listOf(outerEntityType, internalEntityType),
            listOf(),
        )

        val internalEntityTypeAttributeValue = MdmAttributeValues(
            mdmAttributeId = internalEntityTypeSimpleAttribute.mdmId, values = listOf(
                MdmAttributeValue.int64MdmAttributeValue(100L)
            )
        )
        val internalEntity = mdmEntity(
            mdmId = baseMdmId,
            mdmEntityTypeId = internalEntityType.mdmId,
            values = listOf(internalEntityTypeAttributeValue),
            version = baseVersion
        )
        val outerEntitySimpleAttributeValue = MdmAttributeValues(
            mdmAttributeId = outerEntityTypeSimpleAttribute.mdmId, values = listOf(
                MdmAttributeValue.boolMdmAttributeValue(true)
            )
        )
        val outerEntityStructAttributeValue = MdmAttributeValues(
            mdmAttributeId = outerEntityTypeStructAttribute.mdmId, values = listOf(
                MdmAttributeValue.structMdmAttributeValue(internalEntity)
            )
        )
        val outerEntity = mdmEntity(
            mdmId = baseMdmId,
            mdmEntityTypeId = outerEntityType.mdmId,
            values = listOf(outerEntitySimpleAttributeValue, outerEntityStructAttributeValue),
            version = baseVersion
        )

        // when
        val converted = metadata.convertToCommonEntity(outerEntity)
        val convertedBackOuter = metadata.convertToMdmEntity(converted)

        // then
        converted.commonParamValues!!.shouldNotBeEmpty()
        converted.commonParamValues!! shouldHave sameExceptVersions(
            CommonParamValue.byInt64(
                "outerStruct||internalSimple",
                100L
            )
        )
        converted.commonParamValues!! shouldHave sameExceptVersions(CommonParamValue.byBoolean("outerSimple", true))

        // and
        convertedBackOuter should sameExceptVersions(outerEntity)
    }

    @Test
    fun `should not create empty entities when converting from Common to Mdm`() {
        // Тест проверяет, что при обходе сущности и конвертации из Common мы не создаем пустые внутрении сущности
        // given
        val mdmEntityTypeOuter = mdmEntityType()
        val mdmEntityTypeInner = mdmEntityType()
        val mdmAttributeInnerNotPresented = mdmAttribute(
            internalName = "internalSimple",
            mdmEntityTypeId = mdmEntityTypeInner.mdmId,
        )
        val mdmAttributeOuter = mdmAttribute(
            internalName = "outerSimple",
            mdmEntityTypeId = mdmEntityTypeOuter.mdmId,
            dataType = MdmAttributeDataType.INT64
        )

        mdmEntityTypeInner.attributes = listOf(mdmAttributeInnerNotPresented)
        mdmEntityTypeOuter.attributes = listOf(mdmAttributeOuter)
        val mdmMetadata = flatMdmMetadataWithOneStructAttribute(
            baseEntityType = mdmEntityTypeOuter,
            structEntityType = mdmEntityTypeInner,
            structAttributeName = "outer"
        )

        val commonEntity = CommonEntity(
            commonEntityType = mdmMetadata.provideCommonEntityType(),
            commonParamValues = listOf(CommonParamValue(commonParamName = "outerSimple", int64s = listOf(5L)))
        )

        // when
        val mdmEntity = mdmMetadata.convertToMdmEntity(commonEntity)

        // then
        mdmEntity.values shouldHaveSize 1
        mdmEntity.values[mdmAttributeOuter.mdmId]!!.values.first().int64 shouldBe 5L
    }

    private fun sameExceptVersions(other: CommonParamValue) = object : Matcher<List<CommonParamValue>> {
        override fun test(value: List<CommonParamValue>) = MatcherResult(
            value.any { it.equalsExceptVersions(other) },
            "$value should contain $other",
            "$value should contain $other"
        )
    }

    private fun sameExceptVersions(other: MdmEntity) = object : Matcher<MdmEntity> {
        override fun test(value: MdmEntity) = MatcherResult(
            value.equalsExceptVersions(other),
            "$value should be $other",
            "$value should be $other"
        )
    }
}

