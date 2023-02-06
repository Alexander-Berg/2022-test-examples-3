package ru.yandex.market.contentmapping.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.contentmapping.auth.TvmTicketManager
import ru.yandex.passport.tvmauth.*
import ru.yandex.passport.tvmauth.roles.Roles

@Configuration
open class TestTvmConfig {
    @Bean
    open fun tvmTicketManager(): TvmTicketManager {
        val tm = Mockito.mock(TvmTicketManager::class.java)
        Mockito.`when`(tm.getTvmTicket(Mockito.anyInt())).thenReturn("")
        return tm
    }

    @Bean
    open fun nativeTvmClient(): TvmClient {
        return object: TvmClient {
            override fun close() {
            }

            override fun getStatus(): ClientStatus {
                TODO("Not yet implemented")
            }

            override fun getServiceTicketFor(alias: String?): String {
                TODO("Not yet implemented")
            }

            override fun getServiceTicketFor(tvmId: Int): String {
                TODO("Not yet implemented")
            }

            override fun checkServiceTicket(ticketBody: String?): CheckedServiceTicket {
                TODO("Not yet implemented")
            }

            override fun checkUserTicket(ticketBody: String?): CheckedUserTicket {
                TODO("Not yet implemented")
            }

            override fun checkUserTicket(ticketBody: String?, overridedBbEnv: BlackboxEnv?): CheckedUserTicket {
                TODO("Not yet implemented")
            }

            override fun getRoles(): Roles {
                TODO("Not yet implemented")
            }
        }
    }
}
