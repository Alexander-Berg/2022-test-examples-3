package ru.yandex.market.logistics.mqm.service.mbi

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.utils.clearCache
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.mbi.api.client.entity.partner.BusinessOwnerDTO


class MbiPartnerServiceImplTest: AbstractContextualTest() {

    @Autowired
    @Qualifier("caffeineCacheManager")
    lateinit var caffeineCacheManager: CacheManager

    @Autowired
    @Qualifier("mbiApiClientLogged")
    lateinit var mbiApiClient: MbiApiClient

    @Autowired
    lateinit var mbiPartnerService: MbiPartnerService

    @BeforeEach
    fun setUp() {
        clearCache(caffeineCacheManager)
    }

    @DisplayName("Проверка получения email представителя в первый раз и использование кэша в дальнейшем")
    @Test
    fun getOrderPreviousSegmentIds() {
        val testShopId = 11L
        val testEmails = setOf("b@mail.com", "a@mail.com")
        whenever(mbiApiClient.getPartnerSuperAdmin(testShopId))
            .thenReturn(BusinessOwnerDTO(1, 2, "test_login", testEmails))

        assertSoftly {
            mbiPartnerService.getBusinessOwnerEmails(testShopId) shouldBe testEmails.sorted()
            mbiPartnerService.getBusinessOwnerEmails(testShopId) shouldBe testEmails.sorted()
        }
        verify(mbiApiClient).getPartnerSuperAdmin(testShopId)
    }
}
