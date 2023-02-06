package ru.yandex.canvas.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.canvas.config.CanvasTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class Html5BatchControllerCreateBatchNegativeTest {
    @Autowired
    private MockMvc mvc;

    private String uri = "/html5/batch";

    private MultiValueMap<String, String> requiredCreateBatchParams = new LinkedMultiValueMap<>() {{
        add("client_id", "123");
    }};

    private String okContent = "{\"name\": \"Test batch name\", \"sources\" : [{\"id\": \"aa\"}, {\"id\": \"not_found\"}, {\"id\": \"bb\"}]}";
    private String noNameContent = "{\"sources\" : [{\"id\": \"aa\"}, {\"id\": \"not_found\"}, {\"id\": \"bb\"}]}";
    private String noSourceIdContent = "{\"name\": \"Test batch name\", \"sources\" : [{\"id\": \"aa\"}, {\"name\": \"some name\"}, {\"id\": \"bb\"}]}";

    @Test
    public void clientIdRequired() throws Exception {
        mvc.perform(post(uri).params(
                new LinkedMultiValueMap<>() {{
                    add("user_id", "456");
                }}).content(okContent)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().is(400))
                .andExpect(status().reason("Required request parameter 'client_id' for method parameter type Long is not present"));

    }

    @Test
    public void batchNameRequired() throws Exception {
        mvc.perform(post(uri)
                .params(requiredCreateBatchParams)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(noNameContent)
        ).andExpect(status().is(400));
    }

    @Test
    public void sourceIdRequired() throws Exception {
        mvc.perform(post(uri)
                .params(requiredCreateBatchParams)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(noSourceIdContent)
        ).andExpect(status().is(400));
    }
}
