package ru.yandex.market.abo.bpmn.task.call.transcription.repo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.abo.bpmn.AbstractFunctionalTest
import ru.yandex.market.common.test.db.DbUnitDataSet
import java.time.LocalDateTime

/**
 * @author zilzilok
 */
class CallTranscriptionResultRepoTest @Autowired constructor(
    val callTranscriptionResultRepo: CallTranscriptionResultRepo
) : AbstractFunctionalTest() {

    @Test
    @DbUnitDataSet(
        before = ["CallTranscriptionResultRepoTest.empty.csv"],
        after = ["CallTranscriptionResultRepoTest.init.csv"]
    )
    fun `repo save`() {
        callTranscriptionResultRepo.init(
            recordId = RECORD_ID,
            orderId = ORDER_ID,
            operationId = OPERATION_ID,
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["CallTranscriptionResultRepoTest.init.csv"],
        after = ["CallTranscriptionResultRepoTest.transcripted.csv"]
    )
    fun `repo update speech text`() {
        callTranscriptionResultRepo.updateSpeechTextByOperationId(
            operationId = OPERATION_ID,
            firstSpeechText = FIRST_SPEECH_TEXT,
            secondSpeechText = SECOND_SPEECH_TEXT,
            transcriptionTime = TRANSCRIPTION_TIME
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["CallTranscriptionResultRepoTest.transcripted.csv"],
        after = ["CallTranscriptionResultRepoTest.empty.csv"]
    )
    fun `repo delete`() {
        callTranscriptionResultRepo.deleteByRecordId(RECORD_ID)
    }

    @Test
    @DbUnitDataSet(before = ["CallTranscriptionResultRepoTest.transcripted.csv"])
    fun `repo find`() {
        val result = callTranscriptionResultRepo.findByRecordId(RECORD_ID)!!

        assertEquals(RECORD_ID, result.recordId)
        assertEquals(ORDER_ID, result.orderId)
        assertEquals(OPERATION_ID, result.operationId)
        assertEquals(FIRST_SPEECH_TEXT, result.firstSpeechText)
        assertEquals(SECOND_SPEECH_TEXT, result.secondSpeechText)
        assertEquals(TRANSCRIPTION_TIME, result.transcriptionTime)
    }

    companion object {
        private const val RECORD_ID = "03d17e5a-9e1b-bac1-1b60-8247c7f00000"
        private const val ORDER_ID = 1L
        private const val OPERATION_ID = "e03938rprtdkft4118c9"
        private const val FIRST_SPEECH_TEXT = "Привет Леха"
        private const val SECOND_SPEECH_TEXT = "Привет Ваня"
        private val TRANSCRIPTION_TIME = LocalDateTime.parse("2001-01-26T12:00:00")
    }
}
