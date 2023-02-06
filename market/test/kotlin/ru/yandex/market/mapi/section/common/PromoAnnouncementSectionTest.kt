package ru.yandex.market.mapi.section.common

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.response.announces.ResolveAnnounceConfigResponse
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.promoannouncement.PromoAnnouncementScrollboxAssembler
import ru.yandex.market.mapi.section.common.promoannouncement.PromoAnnouncementScrollboxSection

class PromoAnnouncementSectionTest : AbstractSectionTest() {
    private val assembler = PromoAnnouncementScrollboxAssembler()

    @Test
    fun testAssemblyNotStarted() {
        assembler.testAssembly(
            fileMap = mapOf(ResolveAnnounceConfigResponse.RESOLVER to "/section/common/product/announcesResponseNotStarted.json"),
            expected = "/section/common/product/announcesResultNotStarted.json",
            section = PromoAnnouncementScrollboxSection(),
        )
    }

    @Test
    fun testAssemblyStarted() {
        assembler.testAssembly(
            fileMap = mapOf(ResolveAnnounceConfigResponse.RESOLVER to "/section/common/product/announcesResponseStarted.json"),
            expected = "/section/common/product/announcesResultStarted.json",
            section = PromoAnnouncementScrollboxSection(),
        )
    }

    @Test
    fun testAssemblyEnded() {
        assembler.testAssembly(
            fileMap = mapOf(ResolveAnnounceConfigResponse.RESOLVER to "/section/common/product/announcesResponseEnded.json"),
            expected = "/section/common/product/announcesResultEnded.json",
            section = PromoAnnouncementScrollboxSection(),
        )
    }
}
