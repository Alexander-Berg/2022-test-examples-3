package ru.yandex.market.wms.picking.modules.controller;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.transportation.client.TransportationClient;
import ru.yandex.market.wms.transportation.core.model.To;
import ru.yandex.market.wms.transportation.core.model.response.Resource;
import ru.yandex.market.wms.transportation.core.model.response.TransportOrderResourceContent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingMoveContainerControllerTest extends IntegrationTest {

    @Autowired
    @MockBean
    private TransportationClient transportationClient;

    @BeforeEach
    void reset() {
        Mockito.reset(transportationClient);
    }

    @Test
    @DatabaseSetup("/controller/move-container/immutable-state.xml")
    public void shouldSuccessMoveContainer() throws Exception {
        Resource<TransportOrderResourceContent> resource = Resource.of(TransportOrderResourceContent.builder()
                .id("1")
                .assignee("somebody")
                .movement(TransportOrderResourceContent.Movement.builder()
                        .from(TransportOrderResourceContent.Movement.From.builder().loc("PACK_01").build())
                        .to(To.Direct.builder().loc("SORT_01").build())
                        .build())
                .build());

        when(transportationClient.createTransportOrder(any())).thenReturn(ResponseEntity.of(Optional.of(resource)));

        mockMvc.perform(post("/move-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/move-container/1/request.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("controller/move-container/1/response.json")))
                .andReturn();

        verify(transportationClient, times(1)).createTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/controller/move-container/immutable-state.xml")
    public void shouldNotMoveContainerIfFromLocDoesNotExists() throws Exception {
        mockMvc.perform(post("/move-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/move-container/2/request.json")))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andReturn();

        verify(transportationClient, times(0)).createTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/controller/move-container/immutable-state.xml")
    public void shouldNotMoveContainerIfPickDetailDoesNotExists() throws Exception {
        mockMvc.perform(post("/move-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/move-container/3/request.json")))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn();

        verify(transportationClient, times(0)).createTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/controller/move-container/4/before.xml")
    public void shouldNotMoveContainerIfPickDetailStatusLessPacked() throws Exception {
        mockMvc.perform(post("/move-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/move-container/4/request.json")))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn();

        verify(transportationClient, times(0)).createTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/controller/move-container/immutable-state.xml")
    public void shouldNotMoveIfContainerIsBlank() throws Exception {
        mockMvc.perform(post("/move-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/move-container/5/request.json")))
                .andExpect(status().is4xxClientError())
                .andReturn();

        verify(transportationClient, never()).createTransportOrder(any());
    }

    @Test
    @DatabaseSetup("/controller/move-container/immutable-state.xml")
    public void shouldNotMoveIfRouteUnreachable() throws Exception {
        mockMvc.perform(post("/move-container")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("controller/move-container/6/request.json")))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andReturn();

        verify(transportationClient, never()).createTransportOrder(any());
    }
}
