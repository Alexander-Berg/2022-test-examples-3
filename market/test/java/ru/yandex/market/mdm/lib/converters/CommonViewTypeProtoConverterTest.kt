package ru.yandex.market.mdm.lib.converters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.commonViewType

class CommonViewTypeProtoConverterTest {

    @Test
    fun `should map pojo to proto and back`() {
        // given
        val commonViewType = commonViewType()

        // when
        val proto = commonViewType.toProto()
        val pojo = proto.toPojo()

        // then
        pojo shouldBe commonViewType
    }
}
