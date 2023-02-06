package ru.yandex.market.pers.tvm

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class TvmInitializationTest {
    @Test
    fun testInit() {
        val tvmId = 2011722 // grade dev
        val tvmKey = "bUlutxMvh8iwkHLXblcAwA"
        val clientTvmId = 2011276
        val tvmClient = TvmLoader.simpleTvmClient(tvmId, tvmKey, intArrayOf(clientTvmId))
        assertNotNull(tvmClient)

        // should create ticket
        val ticket = tvmClient.getServiceTicketFor(clientTvmId)
        assertNotNull(ticket)

        // try to close, should work fine
        tvmClient.close()
    }
}