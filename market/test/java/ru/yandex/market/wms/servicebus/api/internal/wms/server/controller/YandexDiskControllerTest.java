package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.startrek.dto.YandexDiskSpaceDto;
import ru.yandex.market.wms.servicebus.service.YandexDiskService;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class YandexDiskControllerTest extends IntegrationTest {

    @MockBean
    @Autowired
    private YandexDiskService yandexDiskService;

    @Test
    public void removeDirs() throws Exception {
        //given
        when(yandexDiskService.removeShippedAnomalyDirectories())
                .thenReturn(newArrayList("dir1", "dir2", "dir3"));

        //when
        MvcResult mvcResult = mockMvc.perform(
                        post("/yadisk/clear")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        //then
        assertThat("", mvcResult.getResponse().getContentAsString(),
                equalTo("[\"dir1\",\"dir2\",\"dir3\"]"));
    }

    @Test
    public void diskInfoSpace() throws Exception {
        //given
        YandexDiskSpaceDto spaceInfo = YandexDiskSpaceDto.builder()
                .totalSpace(1000_999L)
                .usedSpace(200_000L)
                .build();
        when(yandexDiskService.getSpaceInfo())
                .thenReturn(spaceInfo);

        //when
        MvcResult mvcResult = mockMvc.perform(
                        get("/yadisk/free-space")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        //then
        assertThat("", mvcResult.getResponse().getContentAsString(),
                equalTo("{\"totalSpace\":1000999,\"usedSpace\":200000}"));
    }


}
