package ru.yandex.market.wms.pickbylight.vendor.axelot.protocol.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.market.wms.pickbylight.utils.byteArrayFromReadableString
import ru.yandex.market.wms.pickbylight.vendor.axelot.protocol.command.ActivateModules.DisplayStatus
import java.util.stream.Stream

class ActivateModulesTest {

    @ParameterizedTest
    @MethodSource("serializeTestData")
    fun convert(command: ActivateModules, readableString: String) {
        val bytes = byteArrayFromReadableString(readableString)
        assertThat(command.serialize()).isEqualTo(bytes)
        assertThat(Command.deserialize(bytes)).isEqualTo(command)
    }

    companion object {
        @JvmStatic
        private fun serializeTestData() = Stream.of(
            arguments(
                ActivateModules(
                    addresses = listOf("1234"),
                ),
                "PP50500001234_____"
            ),
            arguments(
                ActivateModules(
                    addresses = listOf("1234", "4321"),
                ),
                "PP50500001234_____4321_____"
            ),
            arguments(
                ActivateModules(
                    addresses = listOf("1234"),
                    status = DisplayStatus(red = DisplayStatus.State.ON),
                ),
                "PP5050000m1<32><11><11>1234_____"
            ),
            arguments(
                ActivateModules(
                    addresses = listOf("1234", "2222"),
                    status = DisplayStatus(red = DisplayStatus.State.ON, green = DisplayStatus.State.FLASH),
                ),
                "PP5050000m1<32><31><11>1234_____2222_____"
            ),
            arguments(
                ActivateModules(
                    addresses = listOf("1234", "2222", "3333"),
                    status = DisplayStatus(red = DisplayStatus.State.ON, green = DisplayStatus.State.FAST_FLASH),
                    statusAfterConfirmed = DisplayStatus(green = DisplayStatus.State.ON, blue = DisplayStatus.State.ON)
                ),
                "PP5050000m1<32><41><11>m2<31><22><11>1234_____2222_____3333_____"
            ),
        )
    }

}
