package ru.yandex.market.mapi.mock

import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import ru.yandex.market.mapi.client.passport.BlackboxClient
import ru.yandex.market.mapi.core.UserPassportInfo
import java.util.function.Supplier

@Service
open class BlackboxMocker {
    @Autowired
    lateinit var blackboxClient: BlackboxClient

    fun mockOauth() {
        whenever(blackboxClient.checkOauth(any(), any())).thenReturn(
            Supplier {
                UserPassportInfo(
                    true, null,
                    login = "mock-login",
                    111,
                    "user_ticket_value"
                )
            }
        )
    }
}
