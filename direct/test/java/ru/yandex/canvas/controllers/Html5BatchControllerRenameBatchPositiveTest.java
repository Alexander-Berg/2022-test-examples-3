package ru.yandex.canvas.controllers;

import com.mongodb.client.result.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
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
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.html5.Html5BatchesService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;


@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class Html5BatchControllerRenameBatchPositiveTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private Html5BatchesService batchesService;

    @Autowired
    private SessionParams sessionParams;

    private String uri = "/html5/batch";

    private String batchId = "aaa";
    private long clientId = 123L;
    private String newName = StringUtils.repeat("N", 255);

    private MultiValueMap<String, String> requiredRenameBatchParams = new LinkedMultiValueMap<String, String>() {{
        add("client_id", String.valueOf(clientId));
        add("user_id", "456");
    }};


    @Before
    public void prepareService() throws Exception {
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_BANNER)).thenReturn(true);
        when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_BANNER);

        given(batchesService.updateBatchName(batchId, clientId, HTML5_CPM_BANNER, newName))
                .willReturn(UpdateResult.acknowledged(1, 1L, null));

        mvc.perform(patch(uri + "/" + batchId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + newName + "\"}")
                .params(requiredRenameBatchParams))
                .andExpect(status().isOk());
    }

    @After
    public void after() {
        Mockito.reset(sessionParams);
    }

    @Test
    public void serviceCalledOk() {
        verify(batchesService).updateBatchName(any(), anyLong(), any(), any()); // anyLong since (long clientId) is primivite, so any() will cause NPE
    }

    @Test
    public void parametersPassed() {
        verify(batchesService).updateBatchName(eq(batchId), eq(clientId), eq(HTML5_CPM_BANNER), eq(newName));
    }
}
