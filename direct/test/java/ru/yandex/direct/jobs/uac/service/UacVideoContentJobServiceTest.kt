package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.canvas.client.CanvasClient
import ru.yandex.direct.canvas.client.model.video.AdditionResponse
import ru.yandex.direct.canvas.client.model.video.VideoUploadResponse
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.CanvasClientStub
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.jobs.uac.repository.AbstractUacRepositoryJobTest
import ru.yandex.direct.utils.JsonUtils


@JobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UacVideoContentJobServiceTest: AbstractUacRepositoryJobTest() {
    @Autowired
    private lateinit var canvasClient: CanvasClient

    @Autowired
    private lateinit var uacVideoContentJobService: UacVideoContentJobService

    @Autowired
    private lateinit var steps: Steps


    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo

    private val videoWithUpdateId = "video_with_update_id"
    private val videoWithoutUpdateId = "video_without_update_id"

    private val baseVideoUploadResponseRaw: Map<String, Any> = mapOf(
        "name" to "Video name",
        "preset_id" to 42,
        "thumbnail" to mapOf(
            "url" to "https://example.com/thumb400.jpeg",
            "width" to 400,
            "height" to 300,
            "preview" to mapOf(
                "url" to "https://example.com/preview.jpeg",
                "width" to 200,
                "height" to 150,
            ),
        ),
        "thumbnailUrl" to "https://example.com/thumb.jpeg",
        "mime_type" to "video/mp4",
        "url" to "https://example.com/video.mp4",
        "duration" to 102.0,
        "width" to 1200,
        "height" to 800,
        "formats" to listOf(
            mapOf(
                "type" to "video/mp4",
                "url" to "https://example.com/format.mp4",
                "delivery" to "progressive",
                "bitrate" to 954,
            )
        ),
    )

    private val updatedVideoUploadResponseRaw: Map<String, Any> = baseVideoUploadResponseRaw + mapOf(
        "id" to videoWithUpdateId,
        "status" to "ready",
        "create_early_creative" to true,
    )

    private val updatedVideoUploadResponse: VideoUploadResponse = JsonUtils.fromJson(
        JsonUtils.toJson(updatedVideoUploadResponseRaw),
        VideoUploadResponse::class.java
    )

    private val notUpdatedVideoUploadResponseRaw: Map<String, Any> = baseVideoUploadResponseRaw + mapOf(
        "id" to videoWithoutUpdateId,
        "status" to "converting",
        "create_early_creative" to false,
    )

    private val notUpdatedVideoUploadResponse: VideoUploadResponse = JsonUtils.fromJson(
        JsonUtils.toJson(notUpdatedVideoUploadResponseRaw),
        VideoUploadResponse::class.java
    )

    private val additionResponseRaw = mapOf(
        "creative_id" to 987654321,
        "vast" to "<vast>"
    )

    private val additionResponse: AdditionResponse = JsonUtils.fromJson(
        JsonUtils.toJson(additionResponseRaw),
        AdditionResponse::class.java
    )

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!
    }

    @Test
    fun updateMeta() {
        (canvasClient as CanvasClientStub).addCustomVideoUploadResponseWithId(
            updatedVideoUploadResponse, videoWithUpdateId
        )
        (canvasClient as CanvasClientStub).addCustomAdditionResponseWithVideoId(additionResponse, videoWithUpdateId)

        val meta = uacVideoContentJobService.createCreativeForContent(
            clientInfo.clientId!!.asLong(), getUacYdbContentWithVideoId(videoWithUpdateId)
        )

        assertThat(meta).isEqualTo(mapOf(
            "id" to videoWithUpdateId,
            "status" to "ready",
            "preset_id" to 13,
            "creative_id" to 987654321L,
            "vast" to "<vast>",
            "formats" to listOf(
                mapOf(
                    "type" to "video/mp4",
                    "url" to "https://example.com/format.mp4",
                    "delivery" to "progressive",
                    "bitrate" to 954,
                )
            ),
            "thumb" to mapOf(
                "url" to "https://example.com/thumb400.jpeg",
                "width" to 400,
                "height" to 300,
                "preview" to mapOf(
                    "url" to "https://example.com/preview.jpeg",
                    "width" to 200,
                    "height" to 150,
                ),
            ),
        ))
    }

    @Test
    fun notUpdateMeta() {
        (canvasClient as CanvasClientStub).addCustomVideoUploadResponseWithId(
            notUpdatedVideoUploadResponse, videoWithoutUpdateId
        )

        val meta = uacVideoContentJobService.createCreativeForContent(
            clientInfo.clientId!!.asLong(), getUacYdbContentWithVideoId(videoWithoutUpdateId)
        )

        assertThat(meta).isEqualTo(null)
    }

    private fun getUacYdbContentWithVideoId(videoId: String): UacYdbContent {
        return UacYdbContent(
            ownerId = null,
            type= MediaType.VIDEO,
            thumb = "thumb",
            sourceUrl = "sourceUrl",
            mdsUrl = null,
            videoDuration = 15,
            meta = mapOf<String, Any>(
                "id" to videoId,
                "status" to "converting",
                "preset_id" to 13,
            ),
            filename = null,
            accountId = null,
            directImageHash = null,
        )
    }
}
