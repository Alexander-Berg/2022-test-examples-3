package ru.yandex.market.mdm.lib.converters

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.Test
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType

class MdmMetaTypeProtoConverterTest {

    @Test
    fun `when pogo converted to proto it should keep its name`() {
        // given
        val pojoValues = MdmMetaType.values().toList()
        val pojoNames = pojoValues.map { it.name }

        // when
        val protoValues = pojoValues.map { it.toProto() }
        val protoNames = protoValues.map { it.name }

        // then
        pojoNames shouldContainExactly protoNames
    }

    @Test
    fun `when proto converted to pojo it should keep its name`() {
        // given
        val protoValues = MdmBase.MdmMetaType.values().filter { it != MdmBase.MdmMetaType.UNRECOGNIZED }
        val protoNames = protoValues.map { it.name }

        // when
        val pojoValues = protoValues.map { it.toPojo() }
        val pojoNames = pojoValues.map { it.name }

        // then
        pojoNames shouldContainExactly protoNames
    }

    @Test
    fun `when proto unrecognized should throw an exception`() {
        val exception = shouldThrowExactly<IllegalArgumentException> { MdmBase.MdmMetaType.UNRECOGNIZED.toPojo() }
        exception shouldHaveMessage "Not expected meta type for pojo conversion: UNRECOGNIZED."
    }
}
