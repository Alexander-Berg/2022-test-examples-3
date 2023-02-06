package ru.yandex.market.mapi.configprovider

import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mapi.AbstractMapiTest
import ru.yandex.market.mapi.core.contract.ClientConfigProvider
import kotlin.test.assertEquals

class MapiExp3Test : AbstractMapiTest() {

    @Autowired
    lateinit var clientConfigProvider: ClientConfigProvider

    /**
     * Could be run from IDE
     */
    @Test
    fun testGetFapiTimeout() {
        whenever(clientConfigProvider.getFapiTimeout()).then { 2000L }
        assertEquals(2000, clientConfigProvider.getFapiTimeout())
    }
}
