package ru.yandex.market.logistics.calendaring.service.limit

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import ru.yandex.market.logistics.calendaring.base.SoftAssertionSupport
import ru.yandex.market.logistics.calendaring.service.datetime.DateTimeService
import ru.yandex.market.logistics.calendaring.service.system.SystemPropertyService
import ru.yandex.market.logistics.calendaring.service.system.WarehousePropertyService
import ru.yandex.market.logistics.calendaring.service.system.keys.SystemPropertyIntegerKey
import java.time.LocalDate

class QuotaDatesForOutboundUnitTest : SoftAssertionSupport()  {


    private val dateTimeService = mock(DateTimeService::class.java)
    private val systemPropertyService = mock(SystemPropertyService::class.java)
    private val warehousePropertyService = mock(WarehousePropertyService::class.java)

    private val quotaDatesForOutboundFulfillment =
        QuotaDatesForOutboundFulfillment(dateTimeService, systemPropertyService, warehousePropertyService)

    private val quotaDatesForOutboundSortCenter =
        QuotaDatesForOutboundSortCenter(dateTimeService, systemPropertyService)

    private val quotaDatesForOutboundDistributionCenter =
        QuotaDatesForOutboundDistributionCenter(dateTimeService, systemPropertyService)

    @BeforeEach
    fun init() {
        Mockito.`when`(dateTimeService.localDateNow()).thenReturn(LocalDate.of(2021, 5, 11))
        Mockito.`when`(systemPropertyService.getProperty(SystemPropertyIntegerKey.MAX_QUOTA_RANGE)).thenReturn(30)
        Mockito.`when`(
            warehousePropertyService.getProperty(
                1L,
                SystemPropertyIntegerKey.CALENDARING_WITHDRAW_FULFILLMENT_MIN_DAYS_FROM_NOW
            )
        ).thenReturn(1)

    }

    @Test
    fun getQuotaDatesForOutboundFFWhenGreaterMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundFulfillment.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 12), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(30)

    }

    @Test
    fun getQuotaDatesForOutboundFFWhenEqualsThanMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundFulfillment.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 11), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(29)

    }

    @Test
    fun getQuotaDatesForOutboundFFWhenLessThanMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundFulfillment.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 10), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(28)
    }


    @Test
    fun getQuotaDatesForOutboundSCWhenGreaterMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundSortCenter.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 10), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(30)
    }

    @Test
    fun getQuotaDatesForOutboundSCWhenEqualsThanMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundSortCenter.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 9), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(30)
    }

    @Test
    fun getQuotaDatesForOutboundSCWhenLessThanMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundSortCenter.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 8), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(29)
    }


    @Test
    fun getQuotaDatesForOutboundDCWhenGreaterMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundDistributionCenter.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 10), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(30)
    }

    @Test
    fun getQuotaDatesForOutboundDCWhenEqualsThanMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundDistributionCenter.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 9), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(30)
    }

    @Test
    fun getQuotaDatesForOutboundDCWhenLessThanMaxQuotaRange() {

        val quotaDatesForOutbound =
            quotaDatesForOutboundDistributionCenter.getQuotaDatesForOutbound(LocalDate.of(2021, 6, 8), null, 1L)
        softly.assertThat(quotaDatesForOutbound.size).isEqualTo(29)
    }

}
