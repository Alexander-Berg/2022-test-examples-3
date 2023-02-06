package ru.yandex.canvas.controllers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.html5.Batch;
import ru.yandex.canvas.model.html5.Creative;
import ru.yandex.canvas.service.SessionParams;
import ru.yandex.canvas.service.html5.Html5BatchesService;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.service.SessionParams.Html5Tag.HTML5_CPM_BANNER;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class Html5BatchControllerListBatchesTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    private Html5BatchesService batchesService;

    @MockBean
    private SessionParams sessionParams;

    private MultiValueMap<String, String> requiredBatchesGetParams = new LinkedMultiValueMap<String, String>() {{
        add("client_id", "123");
    }};

    private MultiValueMap<String, String> allBatchesGetParams = new LinkedMultiValueMap<>(requiredBatchesGetParams);

    private Batch someBatch = new Batch().setCreatives(singletonList(new Creative()));
    private Batch someOtherBatch = new Batch().setCreatives(singletonList(new Creative()));

    @Before
    public void init() {
        someBatch.setId("aaaa");
        someBatch.setName("Test batch");

        someOtherBatch.setId("bbbbb");
        someOtherBatch.setName("Other test batch");
        allBatchesGetParams.add("limit", "25");
        allBatchesGetParams.add("offset", "50");
        allBatchesGetParams.add("archive", "true");
        allBatchesGetParams.add("sort_order", "asc");
        allBatchesGetParams.add("sizes", "240x400, 160x600");
        allBatchesGetParams.add("name", "Test creative");

        when(sessionParams.sessionIs(SessionParams.SessionTag.CPM_BANNER)).thenReturn(true);
        when(sessionParams.getSessionType()).thenReturn(SessionParams.SessionTag.CPM_BANNER);
        when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();
        //when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

    }

    @After
    public void after() {
        Mockito.reset(sessionParams);
    }

    private BDDMockito.BDDMyOngoingStubbing<List<Batch>> serviceGetBatchesAnyParams() {
        return given(batchesService.getBatches(any(), any(Integer.class), any(), any(), any(), any(), any(), any()));
    }

    private BDDMockito.BDDMyOngoingStubbing<Long> serviceGetBatchesCountAnyParams() {
        return given(batchesService.getBatchesTotalCount(any(), any(), any()));
    }

    @Test
    public void defaultRequestParamValues() throws Exception {
        serviceGetBatchesAnyParams().willReturn(Collections.emptyList());

        //when(sessionParams.getHtml5SessionTag()).thenReturn(HTML5_CPM_BANNER);

        mvc.perform(get("/html5/batches").params(requiredBatchesGetParams)).andExpect(status().isOk());

        //when(sessionParams.getHtml5SessionTag()).thenCallRealMethod();

        verify(batchesService).getBatches(123L, 50, 0, Sort.Direction.DESC, false, List.of("240x400", "320x480",
                "480x320", "300x300", "728x90", "320x100", "240x600", "300x500", "336x280",
                "300x600", "300x250", "970x250", "1000x120", "320x50",  "160x600"), "",
                HTML5_CPM_BANNER);
    }

    @Test
    public void allRequestParamsPassesOk() throws Exception {
        serviceGetBatchesAnyParams().willReturn(Collections.emptyList());

        ArrayList<String> expectedSizes = new ArrayList<>();
        expectedSizes.add("240x400");
        expectedSizes.add("160x600");
        mvc.perform(get("/html5/batches").params(allBatchesGetParams)).andExpect(status().isOk());

        verify(batchesService).getBatches(123L, 25, 50, Sort.Direction.ASC,
                true, expectedSizes, "Test creative", HTML5_CPM_BANNER);
    }

    @Test
    public void getBatchesWithBadSizes() throws Exception {
        serviceGetBatchesAnyParams().willReturn(Collections.emptyList());

        mvc.perform(get("/html5/batches")
                .param("limit", "25")
                .param("offset", "50")
                .param("archive", "true")
                .param("sort_order", "asc")
                .param("sizes", "301x251")
                .param("user_id", "456")
                .param("client_id", "123")
                .param("name", "Test creative"))
                .andExpect(status().is(400));
    }

    @Test
    public void serviceResponsePassesOK() throws Exception {
        int offset = 20;
        int limit = 30;

        List<Batch> batchList = Arrays.asList(someBatch, someOtherBatch);
        int total = batchList.size();
        int newOffset = total;

        serviceGetBatchesAnyParams().willReturn(batchList);
        serviceGetBatchesCountAnyParams().willReturn(2L);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(requiredBatchesGetParams);
        params.set("offset", String.valueOf(offset));
        params.set("limit", String.valueOf(limit));

        String itemsJson = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(batchList);
        String expectedJson = String.format("{ \"items\": %s, \"new_offset\": %d, \"total\": %d }",
                itemsJson, newOffset, total);

        mvc.perform(get("/html5/batches").params(params))
                .andExpect(status().isOk())
                .andExpect(content().json(expectedJson));
    }

    @Test
    public void clientIdRequired() throws Exception {
        mvc.perform(get("/html5/batches").params(
                new LinkedMultiValueMap<String, String>() {{
                    add("user_id", "456");
                }}
        )).andExpect(status().is(400))
                .andExpect(status().reason("Required request parameter 'client_id' for method parameter type Long is not present"));
    }

}
