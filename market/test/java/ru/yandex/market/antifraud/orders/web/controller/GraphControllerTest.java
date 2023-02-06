package ru.yandex.market.antifraud.orders.web.controller;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.antifraud.orders.entity.GlueSource;
import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.service.GlueRequest;
import ru.yandex.market.antifraud.orders.service.GluesService;
import ru.yandex.market.antifraud.orders.test.annotations.WebLayerTest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebLayerTest(GraphController.class)
public class GraphControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GluesService gluesService;

    @Test
    public void getGlue() throws Exception {
        when(gluesService.getGluedIds(
            GlueRequest.builder()
                .source(GlueSource.FAST)
                .requestId(MarketUserId.fromUid(123L))
                .acceptType(GluesService.ResultType.WITH_UUID)
                .build()))
            .thenReturn(CompletableFuture.completedFuture(
                Set.of(
                    MarketUserId.fromUid(123L),
                    MarketUserId.fromUid(124L),
                    MarketUserId.fromYandexuid("yauid"),
                    MarketUserId.fromUuid("uuid")
                )
            ));
        String response = "{\"uids\":[123,124],\"yandexuids\":[\"yauid\"],\"uuids\":[\"uuid\"]}";
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(get("/glue/nodes/fast?uid=123&uidOnly=0")
                        .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8))
                    .andReturn()
            ))
            .andExpect(status().isOk())
            .andExpect(content().json(response));
    }
}