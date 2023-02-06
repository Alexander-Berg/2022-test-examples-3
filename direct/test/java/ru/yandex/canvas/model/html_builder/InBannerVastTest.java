package ru.yandex.canvas.model.html_builder;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.model.CreativeDocument;
import ru.yandex.canvas.model.video.files.InBannerVideo;
import ru.yandex.canvas.repository.video.VideoFilesRepository;
import ru.yandex.canvas.service.video.InBannerVideoFilesService;
import ru.yandex.canvas.steps.ResourceHelpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class InBannerVastTest {

    public static final String CORRECT_VIDEO_ADDITION_VAST_XML =
            "/ru/yandex/canvas/controllers/correctInBannerVast.xml";

    @Autowired
    InBannerHtmlCreativeWriter inBannerHtmlCreativeWriter;

    @Autowired
    InBannerVideoFilesService inBannerVideoFilesService;

    @TestConfiguration
    public static class TestConf {
        @MockBean
        InBannerVideoFilesService inBannerVideoFilesService;


        @Bean
        public InBannerHtmlCreativeWriter inBannerHtmlCreativeWriter() {
            return new InBannerHtmlCreativeWriter(inBannerVideoFilesService);
        }
    }

    @Test
    public void vastTest() throws IOException {

        InBannerVideo inBannerVideoStub = mock(InBannerVideo.class);
        when(inBannerVideoFilesService.lookupMovie(any(CreativeDocument.class),
                any(VideoFilesRepository.QueryBuilder.class))).thenReturn(inBannerVideoStub);

        String xml = inBannerHtmlCreativeWriter.getVast(mock(CreativeDocument.class), 1212L);
        String expectedXml = ResourceHelpers.getResource(CORRECT_VIDEO_ADDITION_VAST_XML);

        assertThat(xml).isEqualTo(expectedXml);

    }

}
