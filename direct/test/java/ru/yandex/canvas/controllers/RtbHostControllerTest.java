package ru.yandex.canvas.controllers;

import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;

import static java.util.stream.Collectors.joining;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author skirsanov
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RtbHostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Checks that POST /rtbHost/uploadCreatives does not accept more than 50 creative ids
     */
    @Test
    public void testUploadCreativesValidation() throws Exception {
        String ids = IntStream.range(1, 52).mapToObj(String::valueOf).collect(joining(",")); // 1,2,3,...,51

        this.mockMvc.perform(post("/rtbHost/uploadCreatives")
                .content("{\"ids\":[" + ids + "]}")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "empty_header")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
