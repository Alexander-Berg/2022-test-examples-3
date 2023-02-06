package ru.yandex.market.logistics.les.objectmapper

import com.amazon.sqs.javamessaging.message.SQSTextMessage
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.support.converter.MessageConverter
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.base.EventPayload
import ru.yandex.market.logistics.les.mapper.component.AesCipherUtil
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent

class ObjectMapperSerializationTest : AbstractContextualTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var messageConverter: MessageConverter

    @Autowired
    private lateinit var aesCipherUtil: AesCipherUtil

    @BeforeEach
    fun setUpFixedIv() {
        doReturn(ByteArray(16) { i -> i.toByte() })
            .whenever(aesCipherUtil)
            .generateIv()
    }

    @Test
    fun testAppNullProperty() {
        assertAppSerialization(SIMPLE_PAYLOAD_EMPTY, "objectmapper/serialization/app/null_field.json")
    }

    @Test
    fun testClientNullProperty() {
        assertClientSerialization(SIMPLE_PAYLOAD_EMPTY, "objectmapper/serialization/client/null_field.json")
    }

    @Test
    fun testAppNoDefaultConstructor() {
        assertAppSerialization(
            NO_DEFAULT_CONSTRUCTOR_PAYLOAD,
            "objectmapper/serialization/app/no_default_constructor.json"
        )
    }

    @Test
    fun testClientNoDefaultConstructor() {
        assertClientSerialization(
            NO_DEFAULT_CONSTRUCTOR_PAYLOAD,
            "objectmapper/serialization/client/no_default_constructor.json"
        )
    }

    @Test
    fun testAppNoDefaultConstructorWithAnnotations() {
        assertAppSerialization(
            NO_DEFAULT_CONSTRUCTOR_PAYLOAD_WITH_ANNOTATIONS,
            "objectmapper/serialization/app/no_default_constructor_with_annotations.json"
        )
    }

    @Test
    fun testClientNoDefaultConstructorWithAnnotations() {
        assertClientSerialization(
            NO_DEFAULT_CONSTRUCTOR_PAYLOAD_WITH_ANNOTATIONS,
            "objectmapper/serialization/client/no_default_constructor_with_annotations.json"
        )
    }

    @Test
    fun testAppInstant() {
        assertAppSerialization(INSTANT_PAYLOAD, "objectmapper/serialization/app/instant.json")
    }

    @Test
    fun testClientInstant() {
        assertClientSerialization(INSTANT_PAYLOAD,"objectmapper/serialization/client/instant.json")
    }

    @Test
    fun testAppPrivateField() {
        assertAppSerialization(PRIVATE_FIELD_PAYLOAD,"objectmapper/serialization/app/private_field.json")
    }

    @Test
    fun testClientPrivateField() {
        assertClientSerialization(PRIVATE_FIELD_PAYLOAD, "objectmapper/serialization/client/private_field.json")
    }

    @Test
    fun testAppPrivateFieldWithAnnotations() {
        assertAppSerialization(
            PRIVATE_FIELD_WITH_ANNOTATION_PAYLOAD,
            "objectmapper/serialization/app/private_field_with_annotation.json"
        )
    }

    @Test
    fun testClientPrivateFieldWithAnnotations() {
        assertClientSerialization(
            PRIVATE_FIELD_WITH_ANNOTATION_PAYLOAD,
            "objectmapper/serialization/client/private_field_with_annotation.json"
        )
    }

    @Test
    fun testClientSetsEmptyOptionalAsObject() {
        assertClientSerialization(OPTIONAL_EVENT_PAYLOAD_EMPTY, "objectmapper/serialization/client/optional.json")
    }

    @Test
    fun testAppSetsEmptyOptionalAsNull() {
        assertAppSerialization(OPTIONAL_EVENT_PAYLOAD_EMPTY,"objectmapper/serialization/app/optional.json")
    }

    @Test
    fun testClientWritesList() {
        assertClientSerialization(LIST_PAYLOAD, "objectmapper/serialization/client/list.json")
    }

    @Test
    fun testAppWritesList() {
        assertAppSerialization(LIST_PAYLOAD, "objectmapper/serialization/app/list.json")
    }


    @Test
    fun testAppCompositeEncrypted() {
        assertAppSerialization(COMPOSITE_ENCRYPTED, "objectmapper/serialization/app/composite_encrypted.json")
    }

    @Test
    fun testClientCompositeEncrypted() {
        assertClientSerialization(COMPOSITE_ENCRYPTED, "objectmapper/serialization/client/composite_encrypted.json")
    }

    private fun assertAppSerialization(payload: EventPayload, path: String) {
        assertSerialization(objectMapper::writeAppPayload, payload, path)
    }

    private fun assertClientSerialization(payload: EventPayload, path: String) {
        assertSerialization(messageConverter::writeClientPayload, payload, path)
    }

    private fun assertSerialization(mapper: (o: EventPayload) -> String, obj: EventPayload, path: String) {
        val serialized = mapper(obj)
        val expectedJson = extractFileContent(path)
        JSONAssert.assertEquals(expectedJson, serialized, true)
    }
}

private fun ObjectMapper.writeAppPayload(payload: EventPayload): String {
    return this.writeValueAsString(
        getEvent(payload)
    )
}

private fun MessageConverter.writeClientPayload(payload: EventPayload): String {
    val message = this.toMessage(
        getEvent(payload),
        sessionMock()
    ) as SQSTextMessage
    return message.text
}

private fun <T : EventPayload> getEvent(payload: T) = Event(
    payload = payload,
    source = null,
    eventId = null,
    timestamp = null,
    eventType = null,
    description = null,
)
