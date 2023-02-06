package ru.yandex.market.mdm.lib.converters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmEnumOption

class MdmEnumOptionProtoConverterTest {

    @Test
    fun `should map pojo to proto and back`() {
        // given
        val mdmEnumOption = mdmEnumOption()

        // when
        val proto = mdmEnumOption.toProto()
        val pojo = proto.toPojo()

        // then
        pojo shouldBe mdmEnumOption
    }
}


