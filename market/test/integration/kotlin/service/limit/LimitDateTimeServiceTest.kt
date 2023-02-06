package ru.yandex.market.logistics.calendaring.service.limit

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.model.dto.WarehouseDTO
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.time.LocalDate

class LimitDateTimeServiceTest(
    @Autowired private val limitDateTimeService: LimitDateTimeService
) : AbstractContextualTest() {

    /**
     * now = 2021-05-11 12:00
     */
    @Test
    fun minDayIsNowPlusOneDayForOutboundForFulfillment(){
        val quotaDatesForSlotDate = limitDateTimeService.getQuotaDatesForSlotDate(
            slotDate = LocalDate.of(2021, 5, 30),
            bookingType = BookingType.WITHDRAW,
            quotaFrom = LocalDate.of(2021, 5, 11),
            warehouse = WarehouseDTO(1, "test", true, PartnerType.FULFILLMENT)
        )

        assertions().assertThat(quotaDatesForSlotDate).doesNotContain(LocalDate.of(2021,5,11))
        assertions().assertThat(quotaDatesForSlotDate).contains(LocalDate.of(2021,5,12))

        assertions().assertThat(quotaDatesForSlotDate).contains(LocalDate.of(2021,5,28))
        assertions().assertThat(quotaDatesForSlotDate).doesNotContain(LocalDate.of(2021,5,29))

    }


    /**
     * now = 2021-05-11 12:00
     * CALENDARING_WITHDRAW_FULFILLMENT_MIN_DAYS_FROM_NOW = 2
     */
    @Test
    @DatabaseSetup("classpath:fixtures/service/limit/limit-date-time/set-days-before-now.xml")
    fun minDayIsNowPlusParamFromPropertyForOutboundForFulfillment(){
        val quotaDatesForSlotDate = limitDateTimeService.getQuotaDatesForSlotDate(
            slotDate = LocalDate.of(2021, 5, 30),
            bookingType = BookingType.WITHDRAW,
            quotaFrom = LocalDate.of(2021, 5, 11),
            warehouse = WarehouseDTO(1, "test", true, PartnerType.FULFILLMENT)
        )

        assertions().assertThat(quotaDatesForSlotDate).doesNotContain(LocalDate.of(2021,5,12))
        assertions().assertThat(quotaDatesForSlotDate).contains(LocalDate.of(2021,5,13))

        assertions().assertThat(quotaDatesForSlotDate).contains(LocalDate.of(2021,5,28))
        assertions().assertThat(quotaDatesForSlotDate).doesNotContain(LocalDate.of(2021,5,29))

    }

    @Test
    fun quotaDateIsSameAsSlotDateFulfillment(){
        val quotaDatesForSlotDate = limitDateTimeService.getQuotaDatesForSlotDate(
            slotDate = LocalDate.of(2021, 5, 30),
            bookingType = BookingType.SUPPLY,
            quotaFrom = LocalDate.of(2021, 5, 11),
            warehouse = WarehouseDTO(1, "test", true, PartnerType.FULFILLMENT)
        )

        assertions().assertThat(quotaDatesForSlotDate).hasSize(1)
        assertions().assertThat(quotaDatesForSlotDate[0]).isEqualTo(LocalDate.of(2021,5,30))

    }

    @Test
    fun allRangeForDS(){
        val quotaDatesForSlotDate = limitDateTimeService.getQuotaDatesForSlotDate(
            slotDate = LocalDate.of(2021, 5, 30),
            bookingType = BookingType.WITHDRAW,
            quotaFrom = LocalDate.of(2021, 5, 11),
            warehouse = WarehouseDTO(1, "test", true, PartnerType.DISTRIBUTION_CENTER)
        )

        assertions().assertThat(quotaDatesForSlotDate).doesNotContain(LocalDate.of(2021,5,10))
        assertions().assertThat(quotaDatesForSlotDate).contains(LocalDate.of(2021,5,11))

        assertions().assertThat(quotaDatesForSlotDate).contains(LocalDate.of(2021,5,30))
        assertions().assertThat(quotaDatesForSlotDate).doesNotContain(LocalDate.of(2021,5,31))

    }

    @Test
    fun allRangeForSC(){
        val quotaDatesForSlotDate = limitDateTimeService.getQuotaDatesForSlotDate(
            slotDate = LocalDate.of(2021, 5, 30),
            bookingType = BookingType.WITHDRAW,
            quotaFrom = LocalDate.of(2021, 5, 11),
            warehouse = WarehouseDTO(1, "test", true, PartnerType.SORTING_CENTER)
        )

        assertions().assertThat(quotaDatesForSlotDate).doesNotContain(LocalDate.of(2021,5,10))
        assertions().assertThat(quotaDatesForSlotDate).contains(LocalDate.of(2021,5,11))

        assertions().assertThat(quotaDatesForSlotDate).contains(LocalDate.of(2021,5,30))
        assertions().assertThat(quotaDatesForSlotDate).doesNotContain(LocalDate.of(2021,5,31))

    }

}
