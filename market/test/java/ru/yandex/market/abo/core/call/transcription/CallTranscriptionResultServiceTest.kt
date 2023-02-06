package ru.yandex.market.abo.core.call.transcription

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.api.entity.call.transcription.CallTranscriptionResultDTO
import java.time.LocalDateTime

/**
 * @author zilzilok
 */
class CallTranscriptionResultServiceTest @Autowired constructor(
    private val callTranscriptionResultService: CallTranscriptionResultService
) : EmptyTest() {

    @Test
    fun saveResultTest() {
        callTranscriptionResultService.saveResult(
            CallTranscriptionResultDTO(
                "03d17e5a-9e1b-bac1-1b60-8247c7f00000",
                1L,
                "e03938rprtdkft4118c9",
                "Привет Леха",
                "Привет Ваня",
                LocalDateTime.now()
            )
        )
    }
}
