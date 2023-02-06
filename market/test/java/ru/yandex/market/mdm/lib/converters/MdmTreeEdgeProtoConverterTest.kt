package ru.yandex.market.mdm.lib.converters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmTreeEdge

class MdmTreeEdgeProtoConverterTest {

    @Test
    fun `should map pojo to proto and back`() {
        // given
        val mdmTreeEdge = mdmTreeEdge()

        // when
        val proto = mdmTreeEdge.toProto()
        val pojo = proto.toPojo()

        // then
        pojo shouldBe mdmTreeEdge
    }
}
