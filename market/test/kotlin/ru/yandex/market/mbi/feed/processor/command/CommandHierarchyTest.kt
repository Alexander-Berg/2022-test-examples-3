package ru.yandex.market.mbi.feed.processor.command

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reflections.Reflections
import ru.yandex.common.util.terminal.AbstractCommand
import ru.yandex.common.util.terminal.CommandExecutor
import ru.yandex.market.mbi.feed.processor.FeedProcessor
import ru.yandex.market.mbi.feed.processor.FunctionalTest

/**
 * Тест, которые проверяет базовый класс всех tms-команд в feed-processor.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class CommandHierarchyTest : FunctionalTest() {

    /**
     * Если тест упал, значит ты сделал(а) tms команду, которая не унаследована от класса [AbstractTmsCommand].
     * Все команды внутри feed-processor должны быть наследниками [AbstractTmsCommand].
     * [AbstractTmsCommand] сам регистрирует команду в [CommandExecutor].
     */
    @Test
    fun `all tms commands are subtypes of AbstractTmsCommand`() {
        val reflections = Reflections(FeedProcessor::class.java.packageName)
        val invalidCommands = reflections.getSubTypesOf(AbstractCommand::class.java)
            .filter { !it.kotlin.isAbstract }
            .filter { !it.isInterface }
            .filter { !AbstractTmsCommand::class.java.isAssignableFrom(it) }

        assertThat(invalidCommands)
            .withFailMessage(
                """
                All tms commands must be assignable from AbstractTmsCommand.
                These command aren't: $invalidCommands
                """.trimIndent()
            )
            .isEmpty()
    }
}
