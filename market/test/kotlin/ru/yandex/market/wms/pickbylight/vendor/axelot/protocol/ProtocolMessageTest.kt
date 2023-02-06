package ru.yandex.market.wms.pickbylight.vendor.axelot.protocol

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.yandex.market.wms.pickbylight.utils.byteArrayFromReadableString
import ru.yandex.market.wms.pickbylight.vendor.axelot.protocol.command.ModuleReport

class ProtocolMessageTest {


    @Test
    fun convert() {
        val message = ProtocolMessage(789, ModuleReport("1234"))
        val readableString = "<2>7890007t123400<3>"

        val bytes = byteArrayFromReadableString(readableString)
        Assertions.assertThat(message.serialize()).isEqualTo(bytes)
        Assertions.assertThat(ProtocolMessage.deserialize(bytes)).isEqualTo(message)
    }
}
