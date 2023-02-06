package ru.yandex.canvas.service.rtbhost.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.canvas.VideoConstants
import ru.yandex.canvas.config.CanvasTest
import ru.yandex.canvas.service.rtbhost.helpers.VideoMotionService.VideoMotion

@CanvasTest
class VideoMotionServiceTest {
    @Autowired
    private lateinit var videoMotionService: VideoMotionService

    @Test
    fun constructorDataFormatTest() {
        val dspCreativeExportEntry = videoMotionService.toImportDspCreativeEntry(
            VideoMotion(), ObjectMapper()
        )
        assertThat(dspCreativeExportEntry.constructorData).isNotNull
        val context = JsonPath.parse(dspCreativeExportEntry.constructorData)
        val template = context.read<String>("$.template")
        assertThat(template).isEqualTo(VideoConstants.VIDEO_MOTION_THEME)
        assertThat(context.read<String>("$.creative_parameters.Video.Theme")).isEqualTo(VideoConstants.VIDEO_MOTION_THEME)
        assertThat(context.read<String>("$.creative_parameters.Video.CreativeId")).isEqualTo("777")
    }
}
