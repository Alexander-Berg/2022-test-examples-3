package ru.yandex.market.doctor.config

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.Cypress
import ru.yandex.inside.yt.kosher.tables.YtTables
import ru.yandex.inside.yt.kosher.transactions.YtTransactions

@TestConfiguration
open class TestYtConfig {
    @Bean
    open fun ytClientMock(): Yt {
        return mock {
            doReturn(ytCypressMock()).`when`(it).cypress()
            doReturn(ytTablesMock()).`when`(it).tables()
            doReturn(ytTransactionsMock()).`when`(it).transactions()
        }
    }

    @Bean
    open fun ytCypressMock(): Cypress {
        return mock()
    }

    @Bean
    open fun ytTablesMock(): YtTables {
        return mock()
    }

    @Bean
    open fun ytTransactionsMock(): YtTransactions {
        return mock()
    }
}
