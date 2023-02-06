package ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.converter

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.randomId
import ru.yandex.market.mdm.lib.model.mdm.I18nStrings
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValues
import ru.yandex.market.mdm.service.common_entity.model.CommonReference
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.SimpleRichAttribute
import ru.yandex.market.mdm.service.common_entity.service.arm.structuredMdmMetadataWithOneStructAttribute
import java.math.BigDecimal

class MdmAttributeValueConverterTest {

    private val metadata = structuredMdmMetadataWithOneStructAttribute()


    @Test
    fun `should convert reference field to CommonParamValue`() {
        // given
        val referenceValue = MdmAttributeValues(randomId(), listOf(MdmAttributeValue(referenceMdmId = 15)))
        val richAttribute =
            SimpleRichAttribute(listOf(mdmAttribute(dataType = MdmAttributeDataType.REFERENCE, structMdmEntityTypeId = 1717)), mapOf())

        // when
        val commonParamValue = referenceValue.toCommonParamValue(richAttribute, metadata)

        // then
        commonParamValue.references shouldContainExactly listOf(CommonReference(15, 1717))
    }

    @Test
    fun `should convert string field to CommonParamValue`() {
        // given
        val stringValue = MdmAttributeValues(randomId(), listOf(MdmAttributeValue(string = I18nStrings.fromRu("15"))))
        val richAttribute =
            SimpleRichAttribute(listOf(mdmAttribute(dataType = MdmAttributeDataType.STRING)), mapOf())

        // when
        val commonParamValue = stringValue.toCommonParamValue(richAttribute, metadata)

        // then
        commonParamValue.strings shouldContainExactly listOf("15")
    }

    @Test
    fun `should convert numeric field to CommonParamValue`() {
        // given
        val numericValue = MdmAttributeValues(randomId(), listOf(MdmAttributeValue(numeric = BigDecimal.valueOf(15))))
        val richAttribute =
            SimpleRichAttribute(listOf(mdmAttribute(dataType = MdmAttributeDataType.NUMERIC)), mapOf())

        // when
        val commonParamValue = numericValue.toCommonParamValue(richAttribute, metadata)

        // then
        commonParamValue.numerics shouldContainExactly listOf(BigDecimal.valueOf(15))
    }

    @Test
    fun `should convert int64 field to CommonParamValue`() {
        // given
        val int64Value = MdmAttributeValues(randomId(), listOf(MdmAttributeValue(int64 = 15)))
        val richAttribute =
            SimpleRichAttribute(listOf(mdmAttribute(dataType = MdmAttributeDataType.INT64)), mapOf())

        // when
        val commonParamValue = int64Value.toCommonParamValue(richAttribute, metadata)

        // then
        commonParamValue.int64s shouldContainExactly listOf(15)
    }

    @Test
    fun `should convert bool field to CommonParamValue`() {
        // given
        val boolValue = MdmAttributeValues(randomId(), listOf(MdmAttributeValue(bool = true)))
        val richAttribute =
            SimpleRichAttribute(listOf(mdmAttribute(dataType = MdmAttributeDataType.BOOLEAN)), mapOf())

        // when
        val commonParamValue = boolValue.toCommonParamValue(richAttribute, metadata)

        // then
        commonParamValue.booleans shouldContainExactly listOf(true)
    }
}
