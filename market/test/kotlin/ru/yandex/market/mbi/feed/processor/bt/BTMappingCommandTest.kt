package ru.yandex.market.mbi.feed.processor.bt

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.common.util.terminal.CommandInvocation
import ru.yandex.common.util.terminal.Terminal
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.bt.mapping.BTMappingCommand
import java.io.PrintWriter

internal class BTMappingCommandTest : FunctionalTest() {
    @Autowired
    private lateinit var btMappingCommand: BTMappingCommand

    private val terminal: Terminal = mock()
    private val writer: PrintWriter = mock()

    @BeforeEach
    fun mockTerminal() {
        whenever(terminal.writer).thenReturn(writer)
    }

    @Test
    @DbUnitDataSet(after = ["BTMappingCommandTest.singleMapping.csv"])
    fun `test SET command`() {
        btMappingCommand.execute(
            CommandInvocation(
                "bt-mapping",
                arrayOf("set", "1001", "1002", "1003"), mapOf()
            ),
            terminal
        )
    }

    @Test
    @DbUnitDataSet(before = ["BTMappingCommandTest.singleMapping.csv"], after = ["BTMappingCommandTest.empty.csv"])
    fun `test DELETE command`() {
        btMappingCommand.execute(
            CommandInvocation(
                "bt-mapping",
                arrayOf("delete", "1001"), mapOf()
            ),
            terminal
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["BTMappingCommandTest.singleMapping.csv"],
        after = ["BTMappingCommandTest.singleMapping.csv"]
    )
    fun `test GET command`() {
        btMappingCommand.execute(
            CommandInvocation(
                "bt-mapping",
                arrayOf("get", "1001"), mapOf()
            ),
            terminal
        )
        verify(writer).println("prod_partner_id=1001, testing_partner_id=1002, testing_business_id=1003")
    }
}
