package ru.yandex.market.wms.pickbylight.vendor.axelot.protocol.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.market.wms.pickbylight.utils.byteArrayFromReadableString

class InitializationTest {

    @Test
    fun convert() {
        val command = Initialization()
        val bytes = byteArrayFromReadableString("Z")
        assertThat(command.serialize()).isEqualTo(bytes)
        assertThat(Command.deserialize(bytes)).isEqualTo(command)
    }

}
