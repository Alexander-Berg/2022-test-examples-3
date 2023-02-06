package ru.yandex.canvas.service.video;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.ControllerTestConfiguration;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.video.presets.VideoPreset;
import ru.yandex.canvas.service.video.presets.configs.AdditionConfig;
import ru.yandex.canvas.service.video.presets.configs.options.FileOptionConfig;

import static org.junit.Assert.assertEquals;
import static ru.yandex.canvas.service.video.presets.configs.ConfigType.ADDITION;

@TestPropertySource(value = "classpath:application.yml")
@ActiveProfiles("unittests")
@RunWith(SpringJUnit4ClassRunner.class)
@Import({ControllerTestConfiguration.class})
public class PackshotPresetTest {

    @Autowired
    VideoPresetsService videoPresetsService;

    @Configuration
    public static class TestConf {
        @MockBean
        private DirectService directService;

        @MockBean
        VideoLimitsService videoLimitsService;

        @Autowired
        private AuthRequestParams authRequestParams;

        @Bean
        @Primary
        VideoPresetsService videoPresetsService(VideoLimitsService videoLimitsService) {
            return new VideoPresetsService(videoLimitsService, directService, authRequestParams);
        }
    }

    @Test
    public void checkPreset() throws JsonProcessingException {
        VideoPreset cpmPreset = videoPresetsService.getPreset(6L);

        AdditionConfig config =
                (AdditionConfig) cpmPreset.getConfig().getConfigs().values().stream().filter(e -> e.getConfigType() == ADDITION)
                        .findFirst().orElse(null);

        FileOptionConfig fileOptionConfig = (FileOptionConfig) config.getOptionConfigs().get(2);

        assertEquals(fileOptionConfig.getIsCroppable(), true);
        assertEquals(fileOptionConfig.getPackshot(), true);
    }

}
