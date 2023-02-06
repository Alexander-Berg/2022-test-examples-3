package ru.yandex.market.logistics.les.objectmapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jms.support.converter.MessageConversionException
import org.springframework.jms.support.converter.MessageConverter
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.base.EventPayload
import ru.yandex.market.logistics.les.objectmapper.testmodel.*
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent


class ObjectMapperDeserializationTest : AbstractContextualTest() {

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var messageConverter: MessageConverter

    @Test
    fun testBothDoesntFailOnUnknownProperties() {
        val clientPayload =
            messageConverter.getClientPayload<SimplePayload>(
                "objectmapper/deserialization/unknown_property.json"
            )
        val serverPayload =
            objectMapper.getAppPayload<SimplePayload>(
                "objectmapper/deserialization/unknown_property.json"
            )

        assertThat(clientPayload).isEqualTo(SIMPLE_PAYLOAD)
        assertThat(serverPayload).isEqualTo(SIMPLE_PAYLOAD)
    }

    @Test
    fun testClientDoesntFailOnUnknownEnum() {
        val clientPayload =
            messageConverter.getClientPayload<SimplePayload>(
                "objectmapper/deserialization/unknown_enum.json"
            )

        assertThat(clientPayload).isEqualTo(SIMPLE_PAYLOAD)
    }

    @Test
    fun testAppFailOnUnknownEnum() {
        assertThatThrownBy {
            objectMapper.getAppPayload<SimplePayload>(
                "objectmapper/deserialization/unknown_enum.json"
            )
        }
            .isInstanceOf(InvalidFormatException::class.java)
            .hasMessageContainingAll("Cannot deserialize value", "not one of the values accepted for Enum class")
    }

    @Test
    fun testClientFailsOnNoDefaultConstructor() {
        assertThatThrownBy {
            messageConverter.getClientPayload<NoDefaultConstructorPayload>(
                "objectmapper/deserialization/no_default_constructor.json"
            )
        }
            .isInstanceOf(MessageConversionException::class.java)
            .hasMessageContainingAll("Cannot construct instance", "no delegate- or property-based Creator")
    }

    @Test
    fun testClientDoesntFailOnNoDefaultConstructorWithAnnotations() {
        val clientPayload =
            messageConverter.getClientPayload<NoDefaultConstructorWithAnnotationsPayload>(
                "objectmapper/deserialization/no_default_constructor_with_annotations.json"
            )

        assertThat(clientPayload).isEqualTo(NO_DEFAULT_CONSTRUCTOR_PAYLOAD_WITH_ANNOTATIONS)
    }

    @Test
    fun testAppDoesntFailOnNoDefaultConstructor() {
        val clientPayload =
            objectMapper.getAppPayload<NoDefaultConstructorPayload>(
                "objectmapper/deserialization/no_default_constructor.json"
            )

        assertThat(clientPayload).isEqualTo(NO_DEFAULT_CONSTRUCTOR_PAYLOAD)
    }

    @Test
    fun testAppDoesntFailOnNoDefaultConstructorWithAnnotations() {
        val appPayload =
            objectMapper.getAppPayload<NoDefaultConstructorWithAnnotationsPayload>(
                "objectmapper/deserialization/no_default_constructor_with_annotations.json"
            )

        assertThat(appPayload).isEqualTo(NO_DEFAULT_CONSTRUCTOR_PAYLOAD_WITH_ANNOTATIONS)
    }

    @Test
    fun testBothCantReadSnakeCase() {
        val clientPayload =
            messageConverter.getClientPayload<SimplePayload>(
                "objectmapper/deserialization/snake_case.json"
            )
        val appPayload = objectMapper.getAppPayload<SimplePayload>(
            "objectmapper/deserialization/snake_case.json"
        )

        assertThat(clientPayload).isEqualTo(SIMPLE_PAYLOAD_EMPTY)
        assertThat(appPayload).isEqualTo(SIMPLE_PAYLOAD_EMPTY)
    }

    @Test
    fun testClientReadInstantAsNumber() {
        val clientPayload =
            messageConverter.getClientPayload<InstantPayload>(
                "objectmapper/deserialization/instant_as_number.json"
            )

        assertThat(clientPayload).isEqualTo(INSTANT_PAYLOAD)
    }

    @Test
    fun testClientReadInstantAsFormattedString() {
        val clientPayload =
            messageConverter.getClientPayload<InstantPayload>(
                "objectmapper/deserialization/instant_as_formatted_string.json"
            )

        assertThat(clientPayload).isEqualTo(INSTANT_PAYLOAD)
    }

    @Test
    fun testAppReadInstantAsNumber() {
        val appPayload =
            objectMapper.getAppPayload<InstantPayload>(
                "objectmapper/deserialization/instant_as_number.json"
            )

        assertThat(appPayload).isNotEqualTo(INSTANT_PAYLOAD)
    }

    @Test
    fun testAppReadInstantAsFormattedString() {
        val appPayload =
            objectMapper.getAppPayload<InstantPayload>(
                "objectmapper/deserialization/instant_as_formatted_string.json"
            )

        assertThat(appPayload).isEqualTo(INSTANT_PAYLOAD)
    }

    @Test
    fun testPrivateFieldNotBeingSet() {
        val appPayload =
            objectMapper.getAppPayload<PrivateFieldPayload>(
                "objectmapper/deserialization/private_field.json"
            )
        val clientPayload =
            messageConverter.getClientPayload<PrivateFieldPayload>(
                "objectmapper/deserialization/private_field.json"
            )

        assertThat(appPayload).isEqualTo(PRIVATE_FIELD_PAYLOAD_EMPTY)
        assertThat(clientPayload).isEqualTo(PRIVATE_FIELD_PAYLOAD_EMPTY)
    }

    @Test
    fun testPrivateFieldWithAnnotationIsBeingSet() {
        val appPayload =
            objectMapper.getAppPayload<PrivateFieldWithAnnotationPayload>(
                "objectmapper/deserialization/private_field_with_annotation.json"
            )
        val clientPayload =
            messageConverter.getClientPayload<PrivateFieldWithAnnotationPayload>(
                "objectmapper/deserialization/private_field_with_annotation.json"
            )

        assertThat(appPayload).isEqualTo(PRIVATE_FIELD_WITH_ANNOTATION_PAYLOAD)
        assertThat(clientPayload).isEqualTo(PRIVATE_FIELD_WITH_ANNOTATION_PAYLOAD)
    }

    @Test
    fun testAppSetsEmptyOptional() {
        val appPayload =
            objectMapper.getAppPayload<OptionalEventPayload>(
                "objectmapper/deserialization/optional.json"
            )

        assertThat(appPayload).isEqualTo(OPTIONAL_EVENT_PAYLOAD_EMPTY)
    }

    @Test
    fun testClientSetsNullOptional() {
        val clientPayload =
            messageConverter.getClientPayload<OptionalEventPayload>(
                "objectmapper/deserialization/optional.json"
            )

        assertThat(clientPayload).isEqualTo(OPTIONAL_EVENT_PAYLOAD_NULL)
    }

    @Test
    fun testAppNonNullableListContainsNulls() {
        val appPayload = objectMapper.getAppPayload<ListPayload>(
            "objectmapper/deserialization/list.json"
        )
        assertThat(appPayload.list).containsExactly("1", "2", null)
    }

    @Test
    fun testClientNonNullableListContainsNulls() {
        val clientPayload = messageConverter.getClientPayload<ListPayload>(
            "objectmapper/deserialization/list.json"
        )
        assertThat(clientPayload.list).containsExactly("1", "2", null)
    }
}

private inline fun <reified T : EventPayload> MessageConverter.getClientPayload(path: String): T {
    val extractedContent = extractFileContent(path).trimIndent()
    val event = this.fromMessage(EventMessage(extractedContent)) as Event
    return event.payload as T
}

private inline fun <reified T : EventPayload> ObjectMapper.getAppPayload(path: String): T {
    val extractedContent = extractFileContent(path).trimIndent()
    val event = this.readValue<Event>(extractedContent)
    return event.payload as T
}
