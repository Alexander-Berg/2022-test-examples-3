package ru.yandex.market.mdm.lib.converters

import com.google.protobuf.Any
import com.google.protobuf.ByteString
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.lib.model.mdm.MdmBooleanExternalReferenceDetails
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalReference
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalSystem
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion

class MdmExternalReferenceProtoConverterTest {
    @Test
    fun `test convert pogo to proto and back`() {
        // given
        val pogo = MdmExternalReference(
            mdmId = 123,
            mdmPath = MdmPath(
                listOf(
                    MdmPathSegment(3, MdmMetaType.MDM_ENTITY_TYPE),
                    MdmPathSegment(2, MdmMetaType.MDM_ATTR),
                    MdmPathSegment(12123321, MdmMetaType.MDM_BOOL)
                )
            ),
            externalSystem = MdmExternalSystem.MBO,
            externalId = 70856654,
            version = MdmVersion(
                from = 313131L,
                to = 313132L
            ),
            mdmBooleanExternalReferenceDetails = MdmBooleanExternalReferenceDetails(true)
        )
        // when
        val convertedToAndBack = pogo.toProto().toPojo()

        //then
        convertedToAndBack shouldBe pogo
    }

    @Test
    fun `test convert proto to pogo and back`() {
        // given
        val proto = MdmBase.MdmExternalReference.newBuilder()
            .setMdmId(12345L)
            .setPath(MdmBase.MdmPath.newBuilder()
                .addSegments(
                    MdmBase.MdmPath.MdmPathSegment.newBuilder()
                        .setMdmId(1)
                        .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE))
                .addSegments(
                    MdmBase.MdmPath.MdmPathSegment.newBuilder()
                        .setMdmId(2)
                        .setType(MdmBase.MdmMetaType.MDM_ATTR)))
            .setExternalSystem(MdmBase.MdmExternalSystem.OLD_MDM)
            .setExternalId(1879)
            .setAttribute(MdmBase.MdmAttributeExtRefDetails.newBuilder()
                .setExternalName("Defence of Rorke's Drift")
                .setExternalTypeHint(MdmBase.MdmAttributeExternalReferenceTypeHint.SAME)
                .setAuxExternalData(
                    Any.newBuilder().setValue(ByteString.copyFromUtf8("Anglo-Zulu War"))))
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setFrom(123456789))
            .build()

        // when
        val convertedToAndBack = proto.toPojo().toProto()

        //then
        convertedToAndBack shouldBe proto
    }
}
