package ru.yandex.market.volva.web.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.serializer.VolvaJsonUtils;
import ru.yandex.market.volva.service.YtReaderService;
import ru.yandex.market.volva.yt.YtEdge;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * @author dzvyagin
 */
@WebMvcTest
@AutoConfigureMockMvc(secure = false)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GluesController.class, GluesControllerTest.TestConf.class})
public class GluesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private YtReaderService readerService;


    @Test
    public void getAdjacencyList() throws Exception {

        Node n1 = new Node("puid-1", IdType.PUID);
        Node n2 = new Node("uuid-2", IdType.UUID);
        Node n3 = new Node("puid-3", IdType.PUID);
        Node n4 = new Node("yandexuid-4", IdType.YANDEXUID);
        Node n5 = new Node("puid-5", IdType.PUID);
        Node n6 = new Node("uuid-6", IdType.UUID);
        when(readerService.getGluedEdges(anyList(), anyCollection()))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        YtEdge.fromNodes(n1, n2, "1"),
                        YtEdge.fromNodes(n2, n3, "2"),
                        YtEdge.fromNodes(n3, n4, "3"),
                        YtEdge.fromNodes(n4, n5, "4"),
                        YtEdge.fromNodes(n5, n6, "5"),
                        YtEdge.fromNodes(n1, n6, "6")
                )));
        String expected = "{\"adjacencyList\":[" +
                "{\"node\":{\"id\":\"puid-5\",\"idType\":\"PUID\"},\"adjacentNodes\":[{\"id\":\"uuid-6\",\"idType\":\"UUID\"},{\"id\":\"yandexuid-4\",\"idType\":\"YANDEXUID\"}]}," +
                "{\"node\":{\"id\":\"uuid-2\",\"idType\":\"UUID\"},\"adjacentNodes\":[{\"id\":\"puid-1\",\"idType\":\"PUID\"},{\"id\":\"puid-3\",\"idType\":\"PUID\"}]}," +
                "{\"node\":{\"id\":\"puid-1\",\"idType\":\"PUID\"},\"adjacentNodes\":[{\"id\":\"uuid-2\",\"idType\":\"UUID\"},{\"id\":\"uuid-6\",\"idType\":\"UUID\"}]}," +
                "{\"node\":{\"id\":\"uuid-6\",\"idType\":\"UUID\"},\"adjacentNodes\":[{\"id\":\"puid-5\",\"idType\":\"PUID\"},{\"id\":\"puid-1\",\"idType\":\"PUID\"}]}," +
                "{\"node\":{\"id\":\"yandexuid-4\",\"idType\":\"YANDEXUID\"},\"adjacentNodes\":[{\"id\":\"puid-5\",\"idType\":\"PUID\"},{\"id\":\"puid-3\",\"idType\":\"PUID\"}]}," +
                "{\"node\":{\"id\":\"puid-3\",\"idType\":\"PUID\"},\"adjacentNodes\":[{\"id\":\"uuid-2\",\"idType\":\"UUID\"},{\"id\":\"yandexuid-4\",\"idType\":\"YANDEXUID\"}]}" +
                "]}";
        mockMvc.perform(asyncDispatch(
                mockMvc.perform(
                        get("/glues/adjacencyList?id=puid-1&type=PUID")
                                .header("Content-Type", "application/json"))
                        .andReturn()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected));
    }

    @Configuration
    public static class TestConf {

        @Bean
        public ObjectMapper objectMapper() {
            return VolvaJsonUtils.OBJECT_MAPPER;
        }
    }
}
