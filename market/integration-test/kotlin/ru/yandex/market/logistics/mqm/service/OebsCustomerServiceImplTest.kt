package ru.yandex.market.logistics.mqm.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.logging.enums.OebsLogCode
import ru.yandex.market.logistics.mqm.utils.Logger
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.util.Optional

internal class OebsCustomerServiceImplTest : AbstractContextualTest(), Logger {
    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()

    @Autowired
    private lateinit var oebsCustomerService: OebsCustomerService

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    private lateinit var yqlJdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var lmsClient: LMSClient

    @Test
    @DisplayName("Успешно находим соответствия partnerId -> vendorId")
    fun getVendorIdsSuccessfulTest() {
        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<Pair<String, Long>>>()))
            .thenReturn(
                listOf(
                    "inn 1" to 555001,
                    "inn 2" to 555002,
                )
            )
        for (i in 1..4L) {
            whenever(lmsClient.getPartnerLegalInfo(eq(i))).thenReturn(Optional.of(createLegalInfoResponse(i)))
        }

        oebsCustomerService.getVendorIds(setOf(1, 2, 3, 4, 5, 6)) shouldBe mapOf(1L to 555001L, 2L to 555002L)
    }

    @Test
    @DisplayName("Обработка пустого списка partnerIds")
    fun emptyInputTest() {
        oebsCustomerService.getVendorIds(setOf()) shouldBe mapOf()
    }

    @Test
    @DisplayName("Обработка пустого возврата ИНН из LMS")
    fun emptyLmsResponseTest() {
        whenever(lmsClient.getPartnerLegalInfo(any())).thenReturn(Optional.ofNullable(null))
        oebsCustomerService.getVendorIds(setOf(1, 2, 3, 4, 5, 6)) shouldBe mapOf()
    }

    @Test
    @DisplayName("Обработка ошибок получения ИНН из LMS")
    fun lmsErrorTest() {
        whenever(lmsClient.getPartnerLegalInfo(eq(1))).thenReturn(Optional.of(createLegalInfoResponse(1)))
        whenever(lmsClient.getPartnerLegalInfo(eq(3))).thenThrow(RuntimeException("Something goes wrong"))
        whenever(lmsClient.getPartnerLegalInfo(eq(2))).thenReturn(Optional.of(createLegalInfoResponse(2)))
        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<Pair<String, Long>>>()))
            .thenReturn(
                listOf(
                    "inn 1" to 555001,
                    "inn 2" to 555002,
                )
            )

        val vendorIds = oebsCustomerService.getVendorIds(setOf(1, 2, 3, 4, 5, 6), ::logException)
        val log = backLogCaptor.results.toString()
        assertSoftly {
            vendorIds shouldBe mapOf(1L to 555001L, 2L to 555002L)
            log shouldContain "Cannot retrieve partnerLegalInfo from LMS"
        }
    }

    private fun logException(message: String, exception: Exception, extra: Pair<Any, Any>? = null) {
        val logger = backlogLogger()
        logger.withCode(OebsLogCode.OEBS_CUSTOMER_SERVICE)
        if (extra != null) {
            logger.withExtra(extra.first, extra.second)
        }
        logger.error(message, exception)
    }

    private fun createLegalInfoResponse(partnerId: Long) = LegalInfoResponse(
        partnerId, partnerId, null, null, null, null, "inn $partnerId", null, null, null,
        null, null, null, null)

}
