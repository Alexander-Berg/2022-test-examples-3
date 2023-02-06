package ru.yandex.canvas.controllers;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.ffmpeg.FfmpegResolutionsResponse;
import ru.yandex.canvas.service.video.DirectFfmpegResolutions;
import ru.yandex.canvas.service.video.FfmpegResolution;
import ru.yandex.canvas.service.video.Ratio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FfmpegResolutionsDirectControllerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetFfmpegResolutions() throws Exception {
        String content = mockMvc.perform(get("/ffmpeg-resolutions")
                .header(HttpHeaders.AUTHORIZATION, "empty_header")
                .param("type", "OUTDOOR_TYPE"))
                .andExpect(status().is(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        TypeReference<List<FfmpegResolutionsResponse>> typeReference = new TypeReference<>() {
        };
        List<FfmpegResolutionsResponse> result = MAPPER.readValue(content, typeReference);

        Set<FfmpegResolution> allResolutions = DirectFfmpegResolutions.OUTDOOR.getAllResolutions();
        int expectedSize = allResolutions.size();
        assertThat(result).hasSize(expectedSize);

        result.forEach(responseItem -> {
            FfmpegResolution expectedFfmpegResolution = allResolutions.stream()
                    .filter(ffmpegResolution -> ffmpegResolution.getSuffix().equals(responseItem.getSuffix()))
                    .findAny()
                    .orElseThrow();

            Ratio actualRatio = new Ratio(responseItem.getRatioWidth(), responseItem.getRatioHeight());
            assertThat(responseItem.getResolutionWidth()).isEqualTo(expectedFfmpegResolution.getWidth());
            assertThat(responseItem.getResolutionHeight()).isEqualTo(expectedFfmpegResolution.getHeight());
            assertThat(actualRatio.toString()).isEqualTo(expectedFfmpegResolution.getRatio());
        });
    }
}
