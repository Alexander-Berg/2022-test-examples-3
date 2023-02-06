package ru.yandex.market.wms.navigator;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
import ru.yandex.market.wms.navigator.config.IntegrationTestConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                IntegrationTestConfig.class,
                SpringApplicationConfig.class
        }
)
@ContextConfiguration(
        initializers = WmsNavigatorZonkyInitializer.class
)
@Transactional
@AutoConfigureMockMvc
@TestPropertySource("/test.properties")
public class BaseWmsNavigatorTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected MvcResult getJson(String url, Object... params) throws Exception {
        return mockMvc.perform(get(String.format(url, params))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().isOk())
                .andReturn();
    }

    protected MvcResult get404Json(String url, Object... params) throws Exception {
        return mockMvc.perform(get(String.format(url, params))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().isNotFound())
                .andReturn();
    }

    protected MvcResult postJson(String url, Object body) throws Exception {
        return mockMvc.perform(
                        post(url)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(body)))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().is2xxSuccessful())
                .andReturn();
    }

    protected <T> T postJson(String url, Object body, Class<T> tClass) throws Exception {
        var mvcResult = postJson(url, body);
        return readJson(mvcResult, tClass);
    }

    protected <T> List<T> postJsonList(String url, Object body, Class<T> tClass) throws Exception {
        var mvcResult = postJson(url, body);
        return readJsonList(mvcResult, tClass);
    }

    protected MvcResult post400Json(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    protected MvcResult post400Json(String url, Object body, String expectedSubstring) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedSubstring)))
                .andReturn();
    }

    protected MvcResult post500Json(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().isInternalServerError())
                .andReturn();
    }

    protected MvcResult post500Json(String url, Object body, String expectedSubstring) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andDo(MockMvcResultHandlers.log())
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(expectedSubstring)))
                .andReturn();
    }

    protected <T> T readJson(MvcResult content, Class<T> cls) throws IOException {
        return objectMapper.readValue(content.getResponse().getContentAsString(), cls);
    }

    protected <T> List<T> readJsonList(MvcResult content, Class<T> cls) throws IOException {
        return objectMapper.readValue(content.getResponse().getContentAsString(), new TypeReference<List<T>>() {
            @Override
            public Type getType() {
                return new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[]{cls};
                    }

                    @Override
                    public Type getRawType() {
                        return List.class;
                    }

                    @Override
                    public Type getOwnerType() {
                        return null;
                    }
                };
            }
        });
    }

    protected <T> T readJson(MvcResult result, TypeReference<T> cls) throws IOException {
        return objectMapper.readValue(result.getResponse().getContentAsString(), cls);
    }
}
