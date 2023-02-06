package ru.yandex.travel.api;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.travel.api.factory.TestController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class NamingStrategyConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper camelCaseMapper =
            new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);

    private ObjectMapper snakeCaseMapper =
            new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    @Test
    public void testCreateOrderInvalidRequestData() throws Exception {
        TestController.TestRequest request = new TestController.TestRequest();
        request.setInputField(123);
        MvcResult res1 = mockMvc.perform(
                post(TestController.TEST_ENDPOINT)
                        .content(snakeCaseMapper.writeValueAsString(request))
                        .contentType("application/json"))
                .andExpect(status().is(200))
                .andReturn();

        TestController.TestResponse response = null;
        String content = res1.getResponse().getContentAsString();
        try {
            response = snakeCaseMapper.readerFor(TestController.TestResponse.class).readValue(content);
        } catch (IOException e) {
            fail("Unable to parse with snake caseCaseMapper: " + content);
        }
        assertEquals(123, response.getOutputField());
    }

    @Test
    public void testCamelCaseParsing() throws Exception {
        TestController.TestRequest request = new TestController.TestRequest();
        request.setInputField(123);
        MvcResult res1 = mockMvc.perform(
                post(TestController.TEST_ENDPOINT)
                        .content(camelCaseMapper.writeValueAsString(request))
                        .contentType("application/json")
                        .header("X-Ya-UseCamelCase", "true"))
                .andExpect(status().is(200))
                .andReturn();

        TestController.TestResponse response = null;
        String content = res1.getResponse().getContentAsString();
        try {
            response = camelCaseMapper.readerFor(TestController.TestResponse.class).readValue(content);
        } catch (IOException e) {
            fail("Unable to parse with snake camelCaseMapper: " + content);
        }
        assertEquals(123, response.getOutputField());
    }
}
