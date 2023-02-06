package ru.yandex.market.logistics.les.entity.ydb

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.base.EventVersion
import ru.yandex.market.logistics.les.model.InternalEvent
import ru.yandex.market.logistics.les.service.StringCipherUtil
import java.time.Instant

internal class EventDaoConverterTest : AbstractContextualTest() {

    companion object {
        const val FAKE_ENCRYPTED_PAYLOAD = "fake encrypted string"

        val INTERNAL_EVENT = InternalEvent(
            rawEvent = "test event content to be encrypted",
            source = "test event source",
            eventId = "test event id",
            timestamp = 42,
            eventType = "test event type",
            version = EventVersion.VERSION_2,
            sensitive = false,
        )

        val SENSITIVE_INTERNAL_EVENT = INTERNAL_EVENT.copy(sensitive = true)

        val EVENT_DAO = EventDao(
            id = null,
            eventId = INTERNAL_EVENT.eventId,
            eventType = INTERNAL_EVENT.eventType,
            source = INTERNAL_EVENT.source,
            payload = """{"payload":"${INTERNAL_EVENT.rawEvent}","format":"PLAIN"}""",
            description = null,
            timestamp = Instant.ofEpochMilli(INTERNAL_EVENT.timestamp)
        )

        val EVENT_DAO_WITH_ENCRYPTED_PAYLOAD = EVENT_DAO.copy(
            payload = """{"payload":"$FAKE_ENCRYPTED_PAYLOAD","format":"ENCRYPTED"}"""
        )
    }

    @Autowired
    lateinit var stringCipherUtil: StringCipherUtil

    @Autowired
    lateinit var eventDaoConverter: EventDaoConverter

    @Test
    fun fromInternalEventNoEncryption() {
        val eventDao = eventDaoConverter.fromInternalEvent(INTERNAL_EVENT)

        assertThat(eventDao).isEqualTo(EVENT_DAO)
    }

    @Test
    fun fromInternalEventWithEncryption() {
        doReturn(FAKE_ENCRYPTED_PAYLOAD).whenever(stringCipherUtil).encrypt(eq(INTERNAL_EVENT.rawEvent))

        val eventDao = eventDaoConverter.fromInternalEvent(SENSITIVE_INTERNAL_EVENT)

        assertThat(eventDao).isEqualTo(EVENT_DAO_WITH_ENCRYPTED_PAYLOAD)
    }
}
