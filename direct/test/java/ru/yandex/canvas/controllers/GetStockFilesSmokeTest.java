package ru.yandex.canvas.controllers;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetStockFilesSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void correctAnswerTest() throws Exception {

        mockMvc.perform(get("/video/files/stock")
                .param("user_id", "12")
                .param("client_id", "13")
                .param("offset", "0")
                .param("type", "video")
                .param("limit", "20")
                .param("group_id", "7")
                .param("name", "")
                .param("sort_order", "desc")
                .param("semantic_search", "")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total", Matchers.is(32)))
                .andExpect(status().is(200));
    }

    @Test
    public void noGroupIdTest() throws Exception {

        mockMvc.perform(get("/video/files/stock")
                .param("user_id", "12")
                .param("client_id", "13")
                .param("offset", "0")
                .param("type", "video")
                .param("limit", "20")
                .param("name", "")
                .param("semantic_search", "")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total", Matchers.is(1037)))
                .andExpect(status().is(200));

    }

    @Test
    public void audioTest() throws Exception {

        mockMvc.perform(get("/video/files/stock")
                .param("user_id", "12")
                .param("client_id", "13")
                .param("offset", "2")
                .param("type", "audio")
                .param("limit", "20")
                .param("name", "")
                .param("semantic_search", "")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total", Matchers.is(10)))
                .andExpect(status().is(200));
    }

    @Test
    public void videoTest() throws Exception {

        mockMvc.perform(get("/video/files/stock")
                .param("user_id", "12")
                .param("client_id", "13")
                .param("category_id", "3939145730")
                .param("offset", "0")
                .param("type", "video")
                .param("limit", "20")
                .param("name", "")
                .param("semantic_search", "")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.total", Matchers.is(36)))
                .andExpect(status().is(200));
    }

}


