package ru.yandex.market.deepdive.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.configuration.IntegrationTestConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.deepdive.utils.Utils.extractFileContent;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = IntegrationTestConfiguration.class)
@AutoConfigureMockMvc(secure = false)
@PropertySource("classpath:integration.properties")
public class TestHttpReturnCodeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private String getJsonForTest(String fileName) {
        return extractFileContent("json/TestHttpReturnCodeControllerTest/" + fileName);
    }

    @Test
    public void returnOkCodeTest() throws Exception {
        String json = getJsonForTest("200.json");
        mockMvc.perform(put("/api/code").content(json)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(200))
                .andExpect(content().json(json));
    }

    @Test
    public void emptyBodyTest() throws Exception {
        mockMvc.perform(put("/api/code").content("{}")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(400));
    }

    @Test
    public void nullInBodyTest() throws Exception {
        String nullObject = this.getJsonForTest("null.json");
        mockMvc.perform(put("/api/code").content(nullObject)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(400));
    }

    @Test
    public void randCodesTest() throws Exception {
        String json = getJsonForTest("500.json");
        mockMvc.perform(put("/api/code").content(json)
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is(500))
                .andExpect(content().json(json));
    }

}
