package ru.yandex.direct.core.entity.uac.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.slf4j.Logger
import ru.yandex.direct.canvas.client.CanvasClient
import ru.yandex.direct.canvas.client.model.exception.CanvasClientException
import ru.yandex.direct.canvas.client.model.html5.Html5SourceResponse
import ru.yandex.direct.canvas.client.model.html5.Html5Tag
import ru.yandex.direct.canvas.client.model.video.UacVideoCreativeType
import ru.yandex.direct.canvas.client.model.video.VideoUploadResponse
import ru.yandex.direct.core.entity.uac.model.CreativeType
import ru.yandex.direct.core.entity.uac.model.FileData
import ru.yandex.direct.core.entity.uac.validation.CanvasValidationDefectParams
import ru.yandex.direct.core.entity.uac.validation.canvasDefect
import ru.yandex.direct.result.ResultState
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers
import java.util.Locale

class UacCanvasServiceTest {
    private val clientId: Long = 123
    private val locale = Locale("ru")
    private val data = FileData("test", ByteArray(0))
    private val url = "https://yandex.ru"

    private val errorMessage = "Error"
    private val errorValidationMessage = "Wrong format"

    @Mock
    lateinit var canvasClient: CanvasClient

    @Mock
    lateinit var logger: Logger

    @InjectMocks
    lateinit var uacCanvasService: UacCanvasService

    @Before
    fun init() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun uploadVideo_validData_returnsSuccessfulResponse() {
        val result: VideoUploadResponse = VideoUploadResponse().withId("456")

        whenever(canvasClient.createVideoFromFile(
            eq(clientId), any(), eq("test"), eq(UacVideoCreativeType.CPM), eq(locale)
        )).doReturn(result)

        val uploadResponse = uacCanvasService.uploadVideo(clientId, data, CreativeType.CPM, locale)

        SoftAssertions.assertSoftly {
            it.assertThat(uploadResponse.state).isEqualTo(ResultState.SUCCESSFUL)
            it.assertThat(uploadResponse.result).isEqualTo(result)
        }
    }

    @Test
    fun uploadVideo_throwsCanvasClientException_returnsCanvasDefect() {
        whenever(canvasClient.createVideoFromFile(
            eq(clientId), any(), eq("test"), eq(UacVideoCreativeType.CPM), eq(locale)
        )).doThrow(CanvasClientException(errorMessage, listOf(errorValidationMessage)))

        val uploadResponse = uacCanvasService.uploadVideo(clientId, data, CreativeType.CPM, locale)

        SoftAssertions.assertSoftly {
            it.assertThat(uploadResponse.state).isEqualTo(ResultState.BROKEN)
            it.assertThat(uploadResponse.validationResult).`is`(Conditions.matchedBy(Matchers.hasDefectDefinitionWith<Any>(
                Matchers.matchesWith(canvasDefect(CanvasValidationDefectParams(listOf(errorValidationMessage)))))))
        }
    }

    @Test
    fun uploadVideoByUrl_validData_returnsSuccessfulResponse() {
        val result: VideoUploadResponse = VideoUploadResponse().withId("456")

        whenever(canvasClient.createVideoFromUrl(
            eq(clientId), any(),  eq(UacVideoCreativeType.CPM), eq(locale)
        )).doReturn(result)

        val uploadResponse = uacCanvasService.uploadVideoByUrl(clientId, url, CreativeType.CPM, locale)

        SoftAssertions.assertSoftly {
            it.assertThat(uploadResponse.state).isEqualTo(ResultState.SUCCESSFUL)
            it.assertThat(uploadResponse.result).isEqualTo(result)
        }
    }

    @Test
    fun uploadVideoByUrl_throwsCanvasClientException_returnsCanvasDefect() {
        whenever(canvasClient.createVideoFromUrl(
            eq(clientId), any(), eq(UacVideoCreativeType.CPM), eq(locale)
        )).doThrow(CanvasClientException(errorMessage, listOf(errorValidationMessage)))

        val uploadResponse = uacCanvasService.uploadVideoByUrl(clientId, url, CreativeType.CPM, locale)

        SoftAssertions.assertSoftly {
            it.assertThat(uploadResponse.state).isEqualTo(ResultState.BROKEN)
            it.assertThat(uploadResponse.validationResult).`is`(Conditions.matchedBy(Matchers.hasDefectDefinitionWith<Any>(
                Matchers.matchesWith(canvasDefect(CanvasValidationDefectParams(listOf(errorValidationMessage)))))))
        }
    }

    @Test
    fun uploadHtml5_validData_returnsSuccessfulResponse() {
        val result: Html5SourceResponse = Html5SourceResponse().withId("456")

        whenever(canvasClient.uploadHtml5(
            eq(clientId), any(), eq("test"), eq(Html5Tag.PLAYABLE), eq(locale)
        )).doReturn(result)

        val uploadResponse = uacCanvasService.uploadHtml5(clientId, data, locale, Html5Tag.PLAYABLE)

        SoftAssertions.assertSoftly {
            it.assertThat(uploadResponse.state).isEqualTo(ResultState.SUCCESSFUL)
            it.assertThat(uploadResponse.result).isEqualTo(result)
        }
    }

    @Test
    fun uploadHtml5_validData_withGenerator_returnsSuccessfulResponse() {
        val result: Html5SourceResponse = Html5SourceResponse().withId("456")

        whenever(canvasClient.uploadHtml5(
            eq(clientId), any(), eq("test"), eq(Html5Tag.GENERATOR), eq(locale)
        )).doReturn(result)

        val uploadResponse = uacCanvasService.uploadHtml5(clientId, data, locale, Html5Tag.GENERATOR)

        SoftAssertions.assertSoftly {
            it.assertThat(uploadResponse.state).isEqualTo(ResultState.SUCCESSFUL)
            it.assertThat(uploadResponse.result).isEqualTo(result)
        }
    }

    @Test
    fun uploadHtml5_throwsCanvasClientException_returnsCanvasDefect() {
        whenever(canvasClient.uploadHtml5(
            eq(clientId), any(), eq("test"), eq(Html5Tag.PLAYABLE), eq(locale)
        )).doThrow(CanvasClientException(errorMessage, listOf(errorValidationMessage)))

        val uploadResponse = uacCanvasService.uploadHtml5(clientId, data, locale, Html5Tag.PLAYABLE)

        SoftAssertions.assertSoftly {
            it.assertThat(uploadResponse.state).isEqualTo(ResultState.BROKEN)
            it.assertThat(uploadResponse.validationResult).`is`(Conditions.matchedBy(Matchers.hasDefectDefinitionWith<Any>(
                Matchers.matchesWith(canvasDefect(CanvasValidationDefectParams(listOf(errorValidationMessage)))))))
        }
    }
}
