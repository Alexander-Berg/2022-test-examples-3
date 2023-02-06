package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamResponse
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.service.lms.LmsPartnerService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

@Deprecated("Заменен на BaseRecipientPlanFactProcessorTest")
@ExtendWith(MockitoExtension::class)
abstract class AbstractRecipientPlanFactProcessorTest {

    @Mock
    protected lateinit var lmsPartnerService: LmsPartnerService

    @Mock
    protected lateinit var logService: LogService

    @AfterEach
    fun tearDown() = verifyNoMoreInteractions(lmsPartnerService)

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        Assertions.assertThat(getProcessor().isEligible(createValidSegment())).isTrue
    }

    @Test
    @DisplayName("Расчет ожидаемого времени для передачи на последнюю милю")
    fun calculateExpectedDatetime() {
        whenever(lmsPartnerService.getPartnerExternalParam(1L, getParamType()))
            .thenReturn(PartnerExternalParamResponse.newBuilder().value("15:00:00").build())

        val expectedDeadline = ZonedDateTime.of(
            LocalDate.of(2021, 1, 2),
            LocalTime.of(15, 0),
            DateTimeUtils.MOSCOW_ZONE
        )
            .toInstant()

        Assertions.assertThat(getProcessor().calculateExpectedDatetime(createValidSegment()))
            .isEqualTo(expectedDeadline)
    }

    @Test
    @DisplayName("Расчет ожидаемого времени для передачи на последнюю милю (без LMS параметра)")
    fun calculateExpectedDatetimeWithoutLmsParam() {
        whenever(lmsPartnerService.getPartnerExternalParam(1L, getParamType()))
            .thenThrow(HttpTemplateException::class.java)

        val expectedDeadline = ZonedDateTime.of(
            LocalDate.of(2021, 1, 2),
            LocalTime.MAX,
            DateTimeUtils.MOSCOW_ZONE
        )
            .toInstant()

        Assertions.assertThat(getProcessor().calculateExpectedDatetime(createValidSegment()))
            .isEqualTo(expectedDeadline)
    }

    protected abstract fun createValidSegment(): WaybillSegment

    protected abstract fun getProcessor(): AbstractRecipientPlanFactProcessor

    protected abstract fun getParamType(): PartnerExternalParamType
}
