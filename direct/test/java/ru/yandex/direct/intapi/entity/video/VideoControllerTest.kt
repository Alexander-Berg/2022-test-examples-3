package ru.yandex.direct.intapi.entity.video

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.isNull
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.yandex.direct.canvas.client.CanvasClient
import ru.yandex.direct.canvas.client.model.video.AdditionResponse
import ru.yandex.direct.canvas.client.model.video.VideoUploadResponse
import ru.yandex.direct.intapi.configuration.IntApiTest
import ru.yandex.direct.utils.JsonUtils

@IntApiTest
class VideoControllerTest {

    private val controller: VideoController

    private val UPLOAD_CONTROLLER_PATH = "/video"

    private val mockMvc: MockMvc

    private val creativeIdfromVideoId = { videoId: String -> videoId.hashCode().toLong() }
    private val clientId = 1L
    private val expectedVideoId = "123"
    private val expectedCreativeId = creativeIdfromVideoId(expectedVideoId)
    private val expectedResponse = VideoController.Companion.CreationResponse(expectedCreativeId)

    init {
        val canvasClient = mock<CanvasClient> {
            on(mock.createVideoFromFile(anyLong(), any(), anyString(), isNull(), isNull())).thenAnswer { invocation ->
                VideoUploadResponse()
                    .withId(expectedVideoId)
                    .withPresetId(123)
                    .withClientId(invocation.getArgument(0))
            }
            on(mock.createDefaultAddition(anyLong(), anyLong(), anyString())).thenAnswer { invocation ->
                AdditionResponse().withCreativeId(
                    creativeIdfromVideoId(invocation.getArgument(2))
                )
            }
        }

        controller = VideoController(canvasClient)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun checkCreatingCreativeFromMp4File() {

        val file = MockMultipartFile("file", "original_file_name", "video/mp4", ByteArray(10) { it.toByte() })

        val requestBuilder = MockMvcRequestBuilders
            .multipart("$UPLOAD_CONTROLLER_PATH/create-creative-from-video-file")
            .file(file)
            .param("clientId", clientId.toString())

        val response = mockMvc
            .perform(requestBuilder)
            .andExpect(status().isOk)
            .andReturn()
            .response

        val createAdditionResponse =
            JsonUtils.fromJson(response.contentAsString, VideoController.Companion.CreationResponse::class.java)

        Assertions.assertEquals(createAdditionResponse, expectedResponse)
    }
}
