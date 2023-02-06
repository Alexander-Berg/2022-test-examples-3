package ru.yandex.market.abo.bpmn.task.call.transcription

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.yandex.market.abo.bpmn.task.call.transcription.model.FIRST_CHANNEL_TAG
import ru.yandex.market.abo.bpmn.task.call.transcription.model.SECOND_CHANNEL_TAG
import ru.yandex.market.abo.bpmn.task.call.transcription.model.getSpeechTextByChannelTag
import yandex.cloud.operation.openapi.client.model.Operation

/**
 * @author zilzilok
 */
class CallTranscriptionResultTest {

    @Test
    fun `get speech text`() {
        val speechTextMap = getSpeechTextByChannelTag(getOperation("operation.json"))
        assertEquals("Привет Леха", speechTextMap[FIRST_CHANNEL_TAG])
        assertEquals("Привет Ваня", speechTextMap[SECOND_CHANNEL_TAG])
    }

    @Test
    fun `get speech text with null result`() {
        val speechTextMap = getSpeechTextByChannelTag(getOperation("null_result_operation.json"))
        assertEquals(null, speechTextMap[FIRST_CHANNEL_TAG])
        assertEquals(null, speechTextMap[SECOND_CHANNEL_TAG])
    }

    private fun getOperation(fileName: String): Operation {
        return MAPPER.readValue(
            javaClass.getResourceAsStream(fileName),
            Operation::class.java
        )
    }

    companion object {
        private val MAPPER = ObjectMapper()
            .registerModule(JavaTimeModule())
    }
}
