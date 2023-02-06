package ru.yandex.market.mbi.orderservice.common

import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import ru.yandex.common.util.terminal.Command
import ru.yandex.common.util.terminal.CommandExecutor
import ru.yandex.common.util.terminal.CommandParser
import ru.yandex.common.util.terminal.Terminal
import ru.yandex.market.common.test.junit.JupiterDbUnitTest
import ru.yandex.market.mbi.orderservice.common.config.CommonFunctionalTestConfig
import java.nio.charset.StandardCharsets

@SpringJUnitConfig(classes = [CommonFunctionalTestConfig::class])
@ActiveProfiles(profiles = ["functionalTest", "development"])
class FunctionalTest : JupiterDbUnitTest()

fun commandInvocationTest(command: Command, commandInvoke: String) {
    val commandExecutor = CommandExecutor()
    commandExecutor.registerCommand(command)
    commandExecutor.setCommandParser(CommandParser())

    val input = commandInvoke
        .trimIndent()
        .byteInputStream(StandardCharsets.UTF_8)

    val terminal = object : Terminal(input, System.out) {
        override fun onStart() {
            "Start command ${command::class.java}"
        }

        override fun onClose() {
            "End command ${command::class.java}"
        }
    }

    commandExecutor.executeCommand(
        input,
        terminal
    )


}
