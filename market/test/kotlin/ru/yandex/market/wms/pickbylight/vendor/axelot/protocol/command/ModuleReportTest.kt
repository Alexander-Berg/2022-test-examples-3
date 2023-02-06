package ru.yandex.market.wms.pickbylight.vendor.axelot.protocol.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.wms.pickbylight.utils.byteArrayFromReadableString
import java.util.stream.Stream

class ModuleReportTest {

    @ParameterizedTest
    @MethodSource("serializeTestData")
    fun convert(command: ModuleReport, readableString: String) {
        val bytes = byteArrayFromReadableString(readableString)
        assertThat(command.serialize()).isEqualTo(bytes)
        assertThat(Command.deserialize(bytes)).isEqualTo(command)
    }

    companion object {
        @JvmStatic
        private fun serializeTestData() = Stream.of(
            arguments(
                ModuleReport("1234", ModuleReport.Status.OK),
                "t123400"
            ),
            arguments(
                ModuleReport("4321", ModuleReport.Status.NOK),
                "t432101"
            ),
        )
    }

}
