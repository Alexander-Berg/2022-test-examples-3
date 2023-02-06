package ru.yandex.market.mdm.lib.converters

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEnumOption

class MdmAttributeProtoConverterTest {

    @Test
    fun `should map full mdm attribute with options to proto`() {
        // given
        val mdmEnumOption = mdmEnumOption()
        val mdmAttribute = mdmAttribute(
            options = listOf(mdmEnumOption)
        )

        // when
        val proto = mdmAttribute.toProto()

        // then
        assertSoftly {
            proto.optionsList shouldContain mdmAttribute.options.first().toProto()
        }
    }

    @Test
    fun `should map pojo to proto and back`() {
        // given
        val mdmEnumOption = mdmEnumOption()
        val mdmAttribute = mdmAttribute(
            options = listOf(mdmEnumOption),
        )

        // when
        val proto = mdmAttribute.toProto()
        val pojo = proto.toPojo()

        pojo shouldBe mdmAttribute
    }
}
