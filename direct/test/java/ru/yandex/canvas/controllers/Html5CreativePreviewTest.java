package ru.yandex.canvas.controllers;

import java.io.IOException;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.html5.Batch;
import ru.yandex.canvas.model.html5.Creative;
import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.repository.html5.BatchesRepository;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.html5.Html5SourcesService;
import ru.yandex.canvas.steps.ResourceHelpers;
import ru.yandex.direct.test.utils.matcher.RegexMatcher;

import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class Html5CreativePreviewTest {

    private static final String ZIP_ARCHIVE = "/ru/yandex/canvas/controllers/html5CreativePreviewTest/zip_archive.zip";
    private static final String VALID_DATA_JSON =
            "/ru/yandex/canvas/controllers/html5CreativePreviewTest/creativePreviewWithValidData.json";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionParams sessionParams;

    @MockBean
    private BatchesRepository batchesRepository;

    @MockBean
    private Html5SourcesService html5SourcesService;

    @MockBean
    private DirectService directService;

    @Test
    public void getCreativePreviewWithValidData() throws Exception {
        Batch batch = testBatch();
        when(batchesRepository.getBatchesByBatchIdsIncludeArchived(ImmutableList.of("abcdefg")))
                .thenReturn(ImmutableList.of(batch));

        mockMvc.perform(post("/html5/creative/abcdefg/45/preview")
                .content("{ \"nonce\":\"nonce\", \"data\":{\"click_url\": { \"clickUrl1\": \"http://f.url/\"}} }")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(json().isEqualTo(ResourceHelpers.getResource(VALID_DATA_JSON)))
                .andExpect(status().is(200));
    }

    @Test
    public void getCreativePreviewWithInvalidJson() throws Exception {

        mockMvc.perform(post("/html5/creative/abcdefg/45/preview")
                .content("{ \"nonce\":\"nonce\", \"data\": null }")
                .contentType(MediaType.APPLICATION_JSON)
                .param("client_id", String.valueOf(1L))
                .param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string("Value must be not empty"))
                .andExpect(status().is(400));
    }

    @Test
    public void getCreativePreviewWithNoData() throws Exception {

        mockMvc.perform(post("/html5/creative/abcdefg/45/preview")
                .content("{}")
                .contentType(MediaType.APPLICATION_JSON)
                .param("client_id", String.valueOf(1L))
                .param("user_id", String.valueOf(2L))
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string("Value must be not empty"))
                .andExpect(status().is(400));
    }

    @Test
    public void getCreativePreview_cpmYndxFrontpage_noAbuseLinkInResponse() throws Exception {
        Batch batch = testBatch();
        when(batchesRepository.getBatchesByBatchIdsIncludeArchived(ImmutableList.of("abcdefg")))
                .thenReturn(ImmutableList.of(batch));

        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_YNDX_FRONTPAGE);
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_YNDX_FRONTPAGE)).thenReturn(true);

        mockMvc.perform(post("/html5/creative/abcdefg/45/preview")
                .content("{ \"nonce\":\"nonce\", \"data\":{\"click_url\": { \"clickUrl1\": \"http://f.url/\"}} }")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").value(RegexMatcher.matches("(?s).*\"abuseLink\":null.*")))
                .andExpect(status().is(200));

    }

    private static Batch testBatch() throws IOException {
        Batch batch = new Batch();
        batch.setId("abcdefg");
        batch.setName("Batch1");
        batch.setArchive(false);
        batch.setAvailable(true);
        batch.setClientId(12313L);

        Creative creative = new Creative();
        creative.setId(45L);
        creative.setName("Creative");
        creative.setArchiveUrl("https://archive.url/");
        creative.setBasePath("https://base.path/");
        creative.setHeight(320);
        creative.setWidth(480);
        creative.setPreviewUrl("https://preview.url");
        creative.setSourceImageInfo(null);

        Source source = Mockito.mock(Source.class);

        when(source.getWidth()).thenReturn(320);
        when(source.getHeight()).thenReturn(480);

        byte[] zipArchive = IOUtils.toByteArray(Html5CreativePreviewTest.class.getResourceAsStream(ZIP_ARCHIVE));
        when(source.getArchiveContent()).thenReturn(zipArchive);
        when(source.unzipArchiveContent()).thenCallRealMethod();

        when(source.getHtmlFilename()).thenReturn("test_arc/index.html");

        creative.setSource(source);
        batch.setCreatives(ImmutableList.of(creative));

        return batch;
    }
}
