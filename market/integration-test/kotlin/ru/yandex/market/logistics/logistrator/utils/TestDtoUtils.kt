package ru.yandex.market.logistics.logistrator.utils

import ru.yandex.market.logistics.logistrator.queue.payload.RequestIdPayload
import ru.yandex.market.logistics.management.entity.response.core.Address
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse
import java.math.BigDecimal
import java.time.LocalTime

const val PARTNER_ID = 123L

const val PARTNER_RELATION_ID = 456L

const val LOGISTICS_POINT_ID = 789L

const val RUSSIA_LOCATION = 225

const val REQUEST_ID = 101L

const val USER_LOGIN_HEADER = "X-User-Login"

const val TOP_CUBIC_BIG_FLOPPA_DOMAIN_LOGIN = "top-cubic-big-floppa"
const val TOP_CUBIC_BIG_FLOPPA_PASSPORT_LOGIN = "yndx-top-cubic-big-floppa"

val REQUEST_ID_PAYLOAD = RequestIdPayload(REQUEST_ID)

fun createLmsAddress() = createLmsAddress("улица имени Парсела, 1")

fun createLmsAddress(shortAddressString: String?): Address = Address.newBuilder()
    .locationId(213)
    .country("Россия")
    .region("Москва")
    .settlement("г. Москва")
    .street("улица имени Парсела")
    .house("1")
    .postCode("101000")
    .latitude(BigDecimal.valueOf(55.752004))
    .longitude(BigDecimal.valueOf(37.617734))
    .addressString("Россия, 101000, г. Москва, улица имени Парсела, 1")
    .shortAddressString(shortAddressString)
    .exactLocationId(213)
    .build()

fun createLmsSchedule(from: Int = 10, to: Int = 19, days: IntArray = (1..7).toList().toIntArray()) =
    days.toList().map { createLmsScheduleDayResponse(it, from, to) }.toSet()

fun createLmsScheduleDayResponse(day: Int, from: Int, to: Int) =
    ScheduleDayResponse(null, day, LocalTime.of(from, 0), LocalTime.of(to, 0), true)
