package ru.yandex.market.mdm.service.common_entity.service.constructor.converters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmExternalReferenceForAttribute
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalSystem
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValue
import ru.yandex.market.mdm.service.common_entity.util.commonEnumValue

class AttributeExternalReferenceConverterTest {

    @Test
    fun `convert empty external ru title to null`() {
        // given
        val converter = AttributeExternalReferenceConverter()
        val entityWithNormalExternalAttributeRuTitle = CommonEntity(
            123L,
            CommonEntityType(CommonEntityTypeEnum.MDM_ENTITY_TYPE),
            listOf(
                CommonParamValue.byLong(
                    commonParamName = "external_attr_id",
                    value = 12
                ),
                CommonParamValue.byString(
                    commonParamName = "mdm_reference_path",
                    value = "MDM_ENTITY_TYPE-1/MDM_ATTR-12"
                ),
                CommonParamValue(
                    commonParamName = "external_system",
                    options = listOf(commonEnumValue(MdmExternalSystem.OLD_MDM))
                ),
                CommonParamValue(
                    commonParamName = "external_attribute_ru_title",
                    strings = listOf("  123   ")
                ),
                CommonParamValue.byString(
                    commonParamName = "external_attr_name",
                    value = "some"
                )
            )
        )
        val entityWithEmptyExternalAttributeRuTitle = entityWithNormalExternalAttributeRuTitle.editValue(
            CommonParamValue(
                commonParamName = "external_attribute_ru_title",
                strings = listOf("")
            )
        )

        // when
        val entityWithNormalExternalAttributeRuTitleConversionResult =
            converter.common2mdm(entityWithNormalExternalAttributeRuTitle)
        val entityWithEmptyExternalAttributeRuTitleConversionResult =
            converter.common2mdm(entityWithEmptyExternalAttributeRuTitle)

        //then
        entityWithNormalExternalAttributeRuTitleConversionResult.mdmAttributeExternalReferenceDetails?.externalRuTitle shouldBe "  123   "
        entityWithEmptyExternalAttributeRuTitleConversionResult.mdmAttributeExternalReferenceDetails?.externalRuTitle shouldBe null
    }

    @Test
    fun `should convert to common entity and back`() {
        // given
        val converter = AttributeExternalReferenceConverter()
        val initialReference = mdmExternalReferenceForAttribute()

        // when
        val commonEntity = converter.mdm2common(initialReference, CommonEntityType(CommonEntityTypeEnum.MDM_ATTR_EXTERNAL_REFERENCE))
        val reconvertedReference = converter.common2mdm(commonEntity)

        // then
        reconvertedReference shouldBe initialReference
    }
}
