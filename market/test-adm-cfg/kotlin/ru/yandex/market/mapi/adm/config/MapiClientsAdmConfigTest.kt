package ru.yandex.market.mapi.adm.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.AbstractAdmConfigTest
import ru.yandex.market.mapi.configprovider.ClientConfigProviderImpl
import kotlin.test.assertEquals

class MapiClientsAdmConfigTest : AbstractAdmConfigTest() {

    private lateinit var clientConfigProvider: ClientConfigProviderImpl

    @BeforeEach
    fun setup() {
        clientConfigProvider = ClientConfigProviderImpl(client)
    }

    @Test
    fun testFapiClientTimeout() {
        clientConfigProvider.refreshConfig()
        assertEquals(2000, clientConfigProvider.getFapiTimeout())
    }

    @Test
    fun testDefaultFapiClientTimeout() {
        assertEquals(1000, clientConfigProvider.getFapiTimeout())
    }
}
