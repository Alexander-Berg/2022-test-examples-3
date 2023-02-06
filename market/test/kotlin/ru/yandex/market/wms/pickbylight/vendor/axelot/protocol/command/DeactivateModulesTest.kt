package ru.yandex.market.wms.pickbylight.vendor.axelot.protocol.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.wms.pickbylight.utils.byteArrayFromReadableString
import java.util.stream.Stream

class DeactivateModulesTest {

    @ParameterizedTest
    @MethodSource("serializeTestData")
    fun convert(command: DeactivateModules, readableString: String) {
        val bytes = byteArrayFromReadableString(readableString)
        assertThat(command.serialize()).isEqualTo(bytes)
        assertThat(Command.deserialize(bytes)).isEqualTo(command)
    }

    companion object {
        @JvmStatic
        private fun serializeTestData() = Stream.of(
            arguments(
                DeactivateModules(listOf("1234")),
                "D1234"
            ),
            arguments(
                DeactivateModules(listOf("1234", "2000", "4321")),
                "D123420004321"
            ),
            arguments(
                DeactivateModules(listOf("1?3?")),
                "D1?3?"
            ),
        )
    }

}
