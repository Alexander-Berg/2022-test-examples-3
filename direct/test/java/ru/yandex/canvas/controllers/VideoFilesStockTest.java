package ru.yandex.canvas.controllers;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.video.files.MovieAndVideoSourceFactory;
import ru.yandex.canvas.service.video.files.StockMoviesService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class VideoFilesStockTest {
    @Autowired
    private MockMvc mockMvc;


    @TestConfiguration
    public static class TestConf {

        @Bean
        public StockMoviesService stockMoviesService(MovieAndVideoSourceFactory movieAndVideoSourceFactory) {
            return new StockMoviesService(null, movieAndVideoSourceFactory);
        }

    }

    @Test
    public void correctAnswerTest() throws Exception {
        mockMvc.perform(get("/video/files/stock?user_id=136822543&client_id=10&type=video&offset=0&limit=20&name=&semantic_search=&sort_order=desc")
                .header(HttpHeaders.AUTHORIZATION, "direct-secret")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(e -> System.err.println("!!! " + e.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.total", Matchers.is(1037)))
                .andExpect(status().is(200));
    }

    @Test
    public void audioTest() throws Exception {
        mockMvc.perform(get("/video/files/stock?user_id=136822543&client_id=10&type=audio&offset=0&limit=5&name=&semantic_search=&sort_order=desc")
                .header(HttpHeaders.AUTHORIZATION, "direct-secret")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON)
        )
                .andExpect(e -> System.err.println("!!! " + e.getResponse().getContentAsString()))
                .andExpect(jsonPath("$.total", Matchers.is(10)))
                .andExpect(jsonPath("$.items.length()", Matchers.is(5)))
                .andExpect(status().is(200));
    }
}
