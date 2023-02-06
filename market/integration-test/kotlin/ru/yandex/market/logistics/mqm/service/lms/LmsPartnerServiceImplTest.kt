package ru.yandex.market.logistics.mqm.service.lms

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.stubbing.Answer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.utils.clearCache
import java.util.Optional

@DisplayName("Проверка сервиса LmsPartnerServiceImpl")
class LmsPartnerServiceImplTest: AbstractContextualTest() {
    @Autowired
    lateinit var lmsClient: LMSClient

    @Autowired
    lateinit var partnerService: LmsPartnerService

    @Autowired
    @Qualifier("caffeineCacheManager")
    lateinit var caffeineCacheManager: CacheManager

    @Test
    @DisplayName("Проверка, что вызов getPartnerLegalInfo через кеш работает")
    fun cashingGetPartnerLegalInfoTest() {
        clearCache(caffeineCacheManager)

        var numberOfInvacation = 0
        val answer = Answer {
            numberOfInvacation++
            val partnerId = it.getArgument<Long>(0)
            Optional.of(LegalInfoResponse(1L, partnerId, "Inc. $partnerId", null, null, null, null, null, null, null,
                null, null, null, null))
        }
        whenever(lmsClient.getPartnerLegalInfo(any())).thenAnswer(answer)
        partnerService.getPartnerLegalInfoOrNull(333L)?.incorporation shouldBe "Inc. 333"
        partnerService.getPartnerLegalInfoOrNull(333L)?.incorporation shouldBe "Inc. 333"

        numberOfInvacation shouldBe 1
    }
}


