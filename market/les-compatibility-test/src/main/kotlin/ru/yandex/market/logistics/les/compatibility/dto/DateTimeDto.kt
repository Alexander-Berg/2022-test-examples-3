package ru.yandex.market.logistics.les.compatibility.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.les.base.EntityKey
import ru.yandex.market.logistics.les.base.EventPayload
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

data class DateTimeDto @JsonCreator constructor(

    @JsonProperty("localDatetime")
    val localDatetime: LocalDateTime? = null,

    @JsonProperty("offsetDateTime")
    val offsetDateTime: OffsetDateTime? = null,

    @JsonProperty("zonedDateTime")
    val zonedDateTime: ZonedDateTime? = null
) : EventPayload {

    override fun getEntityKeys(): List<EntityKey> = emptyList()

    companion object {
        private val INSTANT = Instant.parse("2022-02-02T15:16:31Z")
        val TEST_OBJECT = DateTimeDto(
            LocalDateTime.ofInstant(INSTANT, DateTimeUtils.MOSCOW_ZONE),
            OffsetDateTime.ofInstant(INSTANT, DateTimeUtils.MOSCOW_ZONE),
            ZonedDateTime.ofInstant(INSTANT, DateTimeUtils.MOSCOW_ZONE)
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DateTimeDto

        if ((localDatetime == null) != (other.localDatetime == null)) return false
        if ((offsetDateTime == null) != (other.offsetDateTime == null)) return false
        if ((zonedDateTime == null) != (other.zonedDateTime == null)) return false

        if (localDatetime != null && !localDatetime.isEqual(other.localDatetime)) return false
        if (offsetDateTime != null && !offsetDateTime.isEqual(other.offsetDateTime)) return false
        if (zonedDateTime != null && !zonedDateTime.isEqual(other.zonedDateTime)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = localDatetime?.hashCode() ?: 0
        result = 31 * result + (offsetDateTime?.hashCode() ?: 0)
        result = 31 * result + (zonedDateTime?.hashCode() ?: 0)
        return result
    }
}
