package ru.yandex.market.mapi.client.fapi.util

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.client.fapi.enums.DeliveryPartnerType
import ru.yandex.market.mapi.client.fapi.enums.DeliveryTypes
import ru.yandex.market.mapi.client.fapi.enums.OrderDeliveryFeature
import ru.yandex.market.mapi.client.fapi.enums.OrderStatusEnum
import ru.yandex.market.mapi.client.fapi.enums.OrderSubStatusEnum
import ru.yandex.market.mapi.client.fapi.enums.ServiceColor
import ru.yandex.market.mapi.client.fapi.model.FapiOrder
import ru.yandex.market.mapi.core.AbstractNonSpringTest
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.03.2022
 */
class OrderUtilsTest : AbstractNonSpringTest(){
    companion object {
        // мока на 1647341610 - это 15 марта 2022 13:53
        val CURR_TIME = Instant.ofEpochSecond(1647341610)
        val CURR_DAY = Instant.ofEpochSecond(1647341610).truncatedTo(ChronoUnit.DAYS)

        val OLD_DAY_TIME = CURR_TIME.minus(5, ChronoUnit.DAYS)
        val PREV_DAY_TIME = CURR_TIME.minus(1, ChronoUnit.DAYS)
        val CURR_TO_DATE = "15-03-2022"
        val PREV_DAY_TO_DATE = "14-03-2022"
        val NEXT_DAY_TO_DATE = "16-03-2022"
    }

    @Test
    fun isDelayed() {
        assertEquals(
            false,
            testOrder { order ->
                order.status = OrderStatusEnum.DELIVERY
            }.isProbablyDelayed()
        )

        // order expected yesterday, but still not received (created many days ago)
        assertEquals(
            true,
            testOrder { order ->
                order.status = OrderStatusEnum.DELIVERY
                order.substatus = OrderSubStatusEnum.DELIVERY_SERVICE_RECEIVED
                order.delivery = testDelivery { delivery ->
                    delivery.toDate = PREV_DAY_TO_DATE
                }
            }.isProbablyDelayed()
        )

        // same, but received
        assertEquals(
            false,
            testOrder { order ->
                order.status = OrderStatusEnum.DELIVERY
                order.substatus = OrderSubStatusEnum.USER_RECEIVED
                order.delivery = testDelivery { delivery ->
                    delivery.toDate = PREV_DAY_TO_DATE
                }
            }.isProbablyDelayed()
        )

        // order expected today (created many days ago)
        assertEquals(
            false,
            testOrder { order ->
                order.status = OrderStatusEnum.DELIVERY
                order.substatus = OrderSubStatusEnum.DELIVERY_SERVICE_RECEIVED
                order.delivery = testDelivery { delivery ->
                    delivery.toDate = CURR_TO_DATE
                }
            }.isProbablyDelayed()
        )

        assertEquals(
            true,
            testOrder { order ->
                order.status = OrderStatusEnum.PROCESSING
                order.delivery = testDelivery { delivery ->
                    delivery.toDate = CURR_TO_DATE
                }
            }.isProbablyDelayed()
        )

        // ignore last_mile
        assertEquals(
            false,
            testOrder { order ->
                order.status = OrderStatusEnum.PROCESSING
                order.substatus = OrderSubStatusEnum.READY_FOR_LAST_MILE
                order.delivery = testDelivery { delivery ->
                    delivery.toDate = CURR_TO_DATE
                }
            }.isProbablyDelayed()
        )

        // ignore express
        assertEquals(
            false,
            testOrder { order ->
                order.status = OrderStatusEnum.PROCESSING
                order.delivery = testDelivery { delivery ->
                    delivery.toDate = CURR_TO_DATE
                    delivery.features = listOf(OrderDeliveryFeature.EXPRESS_DELIVERY)
                }
            }.isProbablyDelayed()
        )

        // ignore same day
        assertEquals(
            false,
            testOrder { order ->
                order.status = OrderStatusEnum.PROCESSING
                order.delivery = testDelivery { delivery ->
                    delivery.toDate = CURR_TO_DATE
                }
                order.dates = FapiOrder.Dates().apply {
                    creation = CURR_TIME.toEpochMilli()
                }
            }.isProbablyDelayed()
        )
    }

    @Test
    fun testDsbs() {
        val dsbsOrder = testOrder { order ->
            order.status = OrderStatusEnum.DELIVERY
            order.isFulfilment = false
            order.rgb = ServiceColor.WHITE
            order.delivery = testDelivery { delivery ->
                delivery.deliveryPartnerType = DeliveryPartnerType.SHOP
            }
            order.properties = FapiOrder.Properties()
        }

        val dsbsEdaOrder = testOrder { order ->
            order.status = OrderStatusEnum.DELIVERY
            order.isFulfilment = false
            order.rgb = ServiceColor.WHITE
            order.delivery = testDelivery { delivery ->
                delivery.deliveryPartnerType = DeliveryPartnerType.SHOP
            }
            order.properties = FapiOrder.Properties().apply {
                isEda = "true"
            }
        }

        // both dsbs
        assertEquals(true, dsbsOrder.isDsbs())
        assertEquals(true, dsbsEdaOrder.isDsbs())

        // eda is not dsbs-pure
        assertEquals(true, dsbsOrder.isPureDsbs())
        assertEquals(false, dsbsEdaOrder.isPureDsbs())
    }

    @Test
    fun testClickAndCollect() {
        assertEquals(
            true,
            testOrder { order ->
                order.status = OrderStatusEnum.DELIVERY
                order.isFulfilment = false
                order.rgb = ServiceColor.BLUE
                order.delivery = testDelivery { delivery ->
                    delivery.deliveryPartnerType = DeliveryPartnerType.SHOP
                }
            }.isClickAndCollect()
        )

        assertEquals(
            false,
            testOrder { order ->
                order.status = OrderStatusEnum.DELIVERY
                order.isFulfilment = true
                order.rgb = ServiceColor.BLUE
                order.delivery = testDelivery { delivery ->
                    delivery.deliveryPartnerType = DeliveryPartnerType.SHOP
                }
            }.isClickAndCollect()
        )
    }

    @Test
    fun testGetDaySchedule() {
        val schedule1 = testSchedule(days = testDays(from = 1, to = 2))
        val schedule2 = testSchedule(days = testDays(from = 4, to = 7))
        val outlet = testOutlet(schedule = arrayOf(schedule1, schedule2))

        for (day in 1..2) {
            assertEquals(schedule1, outlet.getDaySchedule(day))
        }
        assertNull(outlet.getDaySchedule(3))
        for (day in 4..7) {
            assertEquals(schedule2, outlet.getDaySchedule(day))
        }
    }

    @Test
    fun testGetNearestDaySchedule() {
        val schedule1 = testSchedule(days = testDays(from = 2, to = 3))
        val schedule2 = testSchedule(days = testDays(from = 4, to = 6))
        val outlet = testOutlet(schedule = arrayOf(schedule1, schedule2))
        for (day in 1..3) {
            assertEquals(schedule1, outlet.getNearestDaySchedule(day))
        }
        for (day in 4..6) {
            assertEquals(schedule2, outlet.getNearestDaySchedule(day))
        }
        assertEquals(schedule1, outlet.getNearestDaySchedule(7))
    }

    @Test
    fun testGetOutletAddressText() {
        val address = FapiOrder.OutletAddress().apply {
            this.fullAddress = "Москва, Митинская улица, д. 27"
            this.country = ""
            this.region = ""
            this.locality = "Москва"
            this.street = "Митинская улица"
            this.km = ""
            this.building = "27"
            this.block = ""
        }
        val outlet = testOutlet(address = address)
        assertEquals("Митинская улица, 27", outlet.getOutletAddressText())
    }

    @Test
    fun testFormatOutletScheduleSimple() {
        val outlet = testOutlet(
            schedule = arrayOf(
                testSchedule(days = testDays(from = 1, to = 5), time = testTime(from = "08:00", to = "20:00")),
                testSchedule(days = testDays(from = 6, to = 7), time = testTime(from = "09:00", to = "19:00"))
            )
        )
        assertEquals(
            "пн-пт 08:00-20:00\n" +
                "сб-вс 09:00-19:00", outlet.formatOutletSchedule())
    }

    @Test
    fun testFormatOutletScheduleWithBreaks() {
        val outlet = testOutlet(
            schedule = arrayOf(
                testSchedule(
                    days = testDays(from = 1, to = 5),
                    time = testTime(from = "08:00", to = "20:00"),
                    breaks = listOf(testTime(from = "13:00", to = "14:00"))
                ),
                testSchedule(
                    days = testDays(from = 6, to = 7),
                    time = testTime(from = "09:00", to = "19:00"),
                    breaks = listOf(
                        testTime(from = "12:00", to = "13:00"),
                        testTime(from = "16:00", to = "17:00")
                    )
                )
            )
        )
        assertEquals(
            "пн-пт 08:00-20:00; перерыв 13:00-14:00\n" +
            "сб-вс 09:00-19:00; перерыв 12:00-13:00,16:00-17:00", outlet.formatOutletSchedule())
    }

    @Test
    fun testFormatOutletStorageLimitDateWithTime() {
        val outlet = testOutlet(
            schedule = arrayOf(
                testSchedule(
                    days = testDays(from = 1, to = 5),
                    time = testTime(from = "09:00", to = "21:00")
                ),
                testSchedule(
                    days = testDays(from = 6, to = 7),
                    time = testTime(from = "10:00", to = "19:00")
                )
            ))
        assertEquals("До 21:00 понедельника, 11 апреля", outlet.formatOutletStorageLimitDate("2022-04-11"))
        assertEquals("До 21:00 вторника, 12 апреля", outlet.formatOutletStorageLimitDate("2022-04-12"))
        assertEquals("До 21:00 среды, 13 апреля", outlet.formatOutletStorageLimitDate("2022-04-13"))
        assertEquals("До 21:00 четверга, 14 апреля", outlet.formatOutletStorageLimitDate("2022-04-14"))
        assertEquals("До 21:00 пятницы, 15 апреля", outlet.formatOutletStorageLimitDate("2022-04-15"))
        assertEquals("До 19:00 субботы, 16 апреля", outlet.formatOutletStorageLimitDate("2022-04-16"))
        assertEquals("До 19:00 воскресенья, 17 апреля", outlet.formatOutletStorageLimitDate("2022-04-17"))
    }

    @Test
    fun testFormatOutletStorageLimitDateWithoutTime() {
        val outlet = testOutlet(
            schedule = arrayOf(
                testSchedule(
                    days = testDays(from = 1, to = 7)
                )
            ))
        assertEquals("До понедельника, 11 апреля, включительно", outlet.formatOutletStorageLimitDate("2022-04-11"))
        assertEquals("До вторника, 12 апреля, включительно", outlet.formatOutletStorageLimitDate("2022-04-12"))
        assertEquals("До среды, 13 апреля, включительно", outlet.formatOutletStorageLimitDate("2022-04-13"))
        assertEquals("До четверга, 14 апреля, включительно", outlet.formatOutletStorageLimitDate("2022-04-14"))
        assertEquals("До пятницы, 15 апреля, включительно", outlet.formatOutletStorageLimitDate("2022-04-15"))
        assertEquals("До субботы, 16 апреля, включительно", outlet.formatOutletStorageLimitDate("2022-04-16"))
        assertEquals("До воскресенья, 17 апреля, включительно", outlet.formatOutletStorageLimitDate("2022-04-17"))
    }

    @Test
    fun testFormatOutletWorkingTimeToday() {
        val outlet = testOutlet(
            schedule = arrayOf(
                testSchedule(
                    days = testDays(from = 1, to = 5),
                    time = testTime(from = "09:00", to = "21:00")
                )
            ))
        // 15 марта - вторник
        assertEquals("Сегодня работает до 21:00", outlet.formatOutletWorkingTime())
    }

    @Test
    fun testFormatOutletWorkingTimeNextDay() {
        val outlet = testOutlet(
            schedule = arrayOf(
                testSchedule(
                    days = testDays(from = 3, to = 7),
                    time = testTime(from = "09:00", to = "21:00")
                )
            ))
        // 15 марта - вторник
        assertEquals("Сегодня закрыт. Завтра работает с 9:00 до 21:00", outlet.formatOutletWorkingTime())
    }

    @Test
    fun testFormatOutletWorkingTimeOtherDay() {
        val outlet = testOutlet(
            schedule = arrayOf(
                testSchedule(
                    days = testDays(from = 5, to = 7),
                    time = testTime(from = "10:00", to = "19:00")
                )
            ))
        // 15 марта - вторник
        assertEquals("Сегодня закрыт. В пятницу работает с 10:00 до 19:00", outlet.formatOutletWorkingTime())
    }

    private fun testOrder(
        customizer: (FapiOrder) -> Unit = {}
    ): FapiOrder {
        return FapiOrder().also {
            it.id = 1
            it.itemIds = emptyList()
            it.status = OrderStatusEnum.DELIVERED
            it.substatus = null
            it.isFulfilment = true
            it.rgb = ServiceColor.WHITE
            it.dates = FapiOrder.Dates().apply {
                creation = OLD_DAY_TIME.toEpochMilli()
            }

            customizer.invoke(it)
        }
    }

    fun testDelivery(customizer: (FapiOrder.Delivery) -> Unit = {}): FapiOrder.Delivery {
        return FapiOrder.Delivery().also {
            it.type = DeliveryTypes.DELIVERY
            it.deliveryPartnerType = DeliveryPartnerType.YANDEX_MARKET
            it.toDate = CURR_TO_DATE
            it.userReceived = false
            it.features = emptyList()

            customizer.invoke(it)
        }
    }

    fun testOutlet(
        address: FapiOrder.OutletAddress? = null,
        vararg schedule: FapiOrder.OutletSchedule
    ): FapiOrder.Outlet {
        return FapiOrder.Outlet().apply {
            this.schedule = schedule.asList()
            this.address = address
        }
    }

    fun testSchedule(
        days: FapiOrder.OutletSchedule.Days? = null,
        time: FapiOrder.OutletSchedule.Time? = null,
        breaks: List<FapiOrder.OutletSchedule.Time>? = null
    ): FapiOrder.OutletSchedule {
        return FapiOrder.OutletSchedule().apply {
            this.days = days
            this.time = time
            this.breaks = breaks
        }
    }

    fun testDays(
        from: Int? = null,
        to: Int? = null
    ): FapiOrder.OutletSchedule.Days {
        return FapiOrder.OutletSchedule.Days().apply {
            this.from = from
            this.to = to
        }
    }

    fun testTime(
        from: String? = null,
        to: String? = null
    ) : FapiOrder.OutletSchedule.Time {
        return FapiOrder.OutletSchedule.Time().apply {
            this.to = to
            this.from = from
        }
    }
}
