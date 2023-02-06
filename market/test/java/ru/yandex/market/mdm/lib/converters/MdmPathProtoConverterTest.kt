package ru.yandex.market.mdm.lib.converters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment

class MdmPathProtoConverterTest {
    @Test
    fun `test convert pogo to proto and back`() {
        // given
        val pogo = MdmPath(listOf(
            MdmPathSegment(3, MdmMetaType.MDM_ENTITY_TYPE),
            MdmPathSegment(2, MdmMetaType.MDM_ATTR),
            MdmPathSegment(12123321, MdmMetaType.MDM_BOOL)
        ))

        // when
        val convertedToAndBack = pogo.toProto().toPojo()

        //then
        convertedToAndBack shouldBe pogo
    }

    @Test
    fun `test convert proto to pogo and back`() {
        // given
        val proto = MdmBase.MdmPath.newBuilder()
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(123)
                .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(1234)
                .setType(MdmBase.MdmMetaType.MDM_ATTR))
            .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                .setMdmId(12345)
                .setType(MdmBase.MdmMetaType.MDM_ENUM_OPTION))
            .build()

        // when
        val convertedToAndBack = proto.toPojo().toProto()

        //then
        convertedToAndBack shouldBe proto
    }
}
