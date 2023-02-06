package ru.yandex.market.hc.web;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.hc.annotation.WebControllerTest;
import ru.yandex.market.hc.entity.DegradationConfig;
import ru.yandex.market.hc.entity.DegradationModes;
import ru.yandex.market.hc.service.DegradationConfigService;
import ru.yandex.market.hc.service.RateLimitingService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by aproskriakov on 10/26/21
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebControllerTest(classes = ConfigurationController.class)
public class ConfigurationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    DegradationModes degradationModes;

    @MockBean
    DegradationConfigService degradationConfigService;

    @MockBean
    RateLimitingService rateLimitingService;

    @Test
    public void testBlankKey() throws Exception {
        mockMvc.perform(get("/configuration"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testInvalidDegradationModes() throws Exception {
        String dc = "{\n" +
                "    \"updatePeriod\": 120,\n" +
                "    \"degradationModes\": [\n" +
                "        200 \n" +
                "    ]\n" +
                "}";

        mockMvc.perform(post("/configuration?key=test")
                .content(dc)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testOk() throws Exception {
        String dc = "{\n" +
                "    \"updatePeriod\": 120,\n" +
                "    \"degradationModes\": [\n" +
                "        20 \n" +
                "    ]\n" +
                "}";
        when(degradationModes.keySet()).thenReturn(Collections.singleton("test"));

        mockMvc.perform(post("/configuration?key=test")
                .content(dc)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());
    }


    @Test
    public void testGetDegradationConfig() throws Exception {
        String key = "expectedKey";
        DegradationConfig degradationConfig = DegradationConfig.builder()
                .degradationModes(Collections.singleton(30))
                .updatePeriod(120)
                .build();
        when(degradationModes.keySet()).thenReturn(Collections.singleton(key));
        String expectedRes = "{\"updatePeriod\":120,\"degradationModes\":[30],\"manualDegradationMode\":null}";
        when(degradationConfigService.getOrDefault(key)).thenReturn(degradationConfig);

        mockMvc.perform(get("/configuration?key="+key)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedRes));
    }

    @Test
    public void testGetKeys() throws Exception {
        String expectedRes = "[\"anothersomeaddres:8080/anotherpath\",\"someaddres:8080/path\"]";
        Set<String> expectedSet = new HashSet<>();
        expectedSet.add("someaddres:8080/path");
        expectedSet.add("anothersomeaddres:8080/anotherpath");
        when(degradationModes.keySet()).thenReturn(expectedSet);
        System.out.println(expectedSet.toString());

        mockMvc.perform(get("/configuration/keys")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedRes));
    }
}
