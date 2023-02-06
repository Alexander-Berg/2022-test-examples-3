package ru.yandex.direct.jobs.uac.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.jobs.configuration.GrutJobsTest

@GrutJobsTest
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GrutUacContentJobServiceTest {
    @Autowired
    private lateinit var grutUacContentJobService: GrutUacContentJobService

    @Autowired
    private lateinit var grutSteps: GrutSteps


    @Test
    fun updateVideoMetaTest() {
        val clientId = grutSteps.createClient()
        val videoDuration = 5
        val videoAssetId = grutSteps.createDefaultVideoAsset(clientId, videoDuration = videoDuration)
        val newMeta = mapOf("test_meta_field_1" to "TEST", "test_meta_field_2" to null)
        grutUacContentJobService.updateVideoMeta(clientId.toString(), videoAssetId, newMeta)
        val content = grutUacContentJobService.getContent(videoAssetId)
        assertThat(content).isNotNull
        assertThat(content!!.meta).isEqualTo(newMeta)
        assertThat(content.videoDuration).isEqualTo(videoDuration)
    }
}
