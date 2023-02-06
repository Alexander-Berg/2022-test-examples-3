package ru.yandex.canvas.controllers;

import java.util.Arrays;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.html5.Source;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.html5.Html5BatchesService;
import ru.yandex.canvas.service.html5.Html5SourcesService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_YNDX_FRONTPAGE;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class Html5BatchControllerCreateBatchPositiveTest {
    private static final String URI = "/html5/batch";
    private static String source1Id = "aa";
    private static String source2Id = "bb";
    private static Source source1;
    private static Source source2;

    @Autowired
    private MockMvc mvc;
    @MockBean
    private Html5SourcesService sourcesService;
    @MockBean
    private Html5BatchesService batchesService;
    @Autowired
    private SessionParams sessionParams;
    private String batchName = "Test batch name";
    private String okContent = "{\"name\": \"" + batchName + "\", "
            + "\"sources\" : [{\"id\": \"" + source1Id + "\"}, {\"id\": \"" + source2Id + "\"}]}";

    @BeforeClass
    public static void prepareData() {
        source1 = new Source();
        source1.setId(source1Id);

        source2 = new Source();
        source1.setId(source2Id);
    }

    public void prepareService(SessionParams.SessionTag productType) throws Exception {
        given(sourcesService.getSources(123L, source1Id, source2Id))
                .willReturn(Arrays.asList(source1, source2));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>() {{
            add("client_id", "123");
            add("user_id", "456");
        }};

        when(sessionParams.getSessionType()).thenReturn(productType);
        when(sessionParams.sessionIs(productType)).thenReturn(true);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

        mvc.perform(post(URI)
                .params(params)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(okContent)
        ).andExpect(status().is(201));
    }

    @After
    public void after() {
        Mockito.reset(sessionParams);
    }

    @Test
    public void serviceCalledOk() throws Exception {
        prepareService(SessionParams.SessionTag.CPM_GEOPRODUCT);
        verify(batchesService).createBatchFromSources(any(), any(), any(), any(), isNull());
    }

    @Test
    public void parametersPassed() throws Exception {
        prepareService(SessionParams.SessionTag.CPM_BANNER);
        verify(batchesService).createBatchFromSources(eq(123L), eq(batchName), anyList(), eq(HTML5_CPM_BANNER),
                isNull());
    }

    @Test
    public void onlyFoundSourcesUsedForBatchCreation() throws Exception {
        prepareService(SessionParams.SessionTag.CPM_BANNER);
        verify(batchesService).createBatchFromSources(any(), any(), eq(Arrays.asList(source1, source2)),
                eq(HTML5_CPM_BANNER), isNull());
    }

    @Test
    public void explicitProductTypeIsPassed_passedToService() throws Exception {
        prepareService(SessionParams.SessionTag.CPM_BANNER);
        verify(batchesService).createBatchFromSources(any(), any(), any(), eq(HTML5_CPM_BANNER), isNull());
    }

    @Test
    public void nonDefaultProductTypeIsPassed_passedToService() throws Exception {
        prepareService(SessionParams.SessionTag.CPM_YNDX_FRONTPAGE);
        verify(batchesService).createBatchFromSources(any(), any(), any(), eq(HTML5_CPM_YNDX_FRONTPAGE), isNull());
    }
}
