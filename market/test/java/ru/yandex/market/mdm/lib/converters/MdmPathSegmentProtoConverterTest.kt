package ru.yandex.market.mdm.lib.converters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment

class MdmPathSegmentProtoConverterTest {
    @Test
    fun `test convert pogo to proto and back`() {
        // given
        val pogo = MdmPathSegment(213L, MdmMetaType.MDM_ATTR)

        // when
        val convertedToAndBack = pogo.toProto().toPojo()

        //then
        convertedToAndBack shouldBe pogo
    }

    @Test
    fun `test convert proto to pogo and back`() {
        // given
        val proto = MdmBase.MdmPath.MdmPathSegment.newBuilder()
            .setMdmId(1L)
            .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE)
            .build()

        // when
        val convertedToAndBack = proto.toPojo().toProto()

        //then
        convertedToAndBack shouldBe proto
    }
}
