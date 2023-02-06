package ru.yandex.canvas.controllers;

import java.util.Collections;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.CreativeDocument;
import ru.yandex.canvas.service.CreativesService;
import ru.yandex.canvas.service.DirectService;

import static java.util.stream.Collectors.joining;
import static net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author skirsanov
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreativesService creativesService;

    @Autowired
    private DirectService directService;

    /**
     * Checks that GET /direct/creatives does not accept more than 50 creative ids
     */

    @Test
    public void testGetCreativesValidation() throws Exception {
        String ids = IntStream.range(1, 52).mapToObj(String::valueOf).collect(joining(",")); // 1,2,3,...,51

        mockMvc.perform(get("/direct/creatives")
                .param("ids", ids)
                .param("client_id", "1")
                .header(HttpHeaders.AUTHORIZATION, "empty_header")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid request")));
    }

    @Test
    public void testGetCreativesErrorResponseFormat() throws Exception {
        mockMvc.perform(get("/direct/creatives")
                .param("ids", "1")
                .param("client_id", "1")
                .header(HttpHeaders.AUTHORIZATION, "empty_header")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]", hasEntry("creativeId", 1)))
                .andExpect(jsonPath("$[0]", hasEntry("ok", false)))
                .andExpect(jsonPath("$[0]", hasEntry("message", "Not Found")));
    }

    @Test
    public void testGetCreatives() throws Exception {

        CreativeDocument creativeDocument = new CreativeDocument();
        creativeDocument.setId(117375L);
        creativeDocument.setName(null);
        creativeDocument
                .setPreviewURL("https://canvas-preview.host/creatives/1073741864/preview?isCompactPreview=true");
        creativeDocument.setAvailable(true);

        CreativeData.Options options = new CreativeData.Options();
        options.setIsAdaptive(true);
        CreativeData creativeData = new CreativeData().withOptions(options);
        creativeData.setHeight(250);
        creativeData.setWidth(300);
        creativeData.setElements(Collections.emptyList());
        creativeData.setMediaSets(Collections.emptyMap());
        creativeDocument.setData(creativeData);

        Mockito.doNothing().when(directService).checkToken(Mockito.anyString());

        when(creativesService.getCreatives(anyCollection(), anyLong())).thenReturn(
                Collections.singletonList(creativeDocument)
        );

        mockMvc.perform(get("/direct/creatives")
                .header(HttpHeaders.AUTHORIZATION, "empty_header")
                .param("ids", "117375,2387863")
                .param("client_id", "1"))
                .andExpect(result -> {
                    System.err.println(
                            "!!!" + result.getResponse().getContentAsString() + " " + result.getResponse().getStatus());
                })
                .andExpect(json().isEqualTo("[{\"creativeId\":117375,\"ok\":true,\"creative\":{\"width\":300,"
                        + "\"height\":250,\"duration\":null,\"creative_id\":117375,\"creative_type\":null,"
                        + "\"creative_name\":null,\"preview_url\":null,\"live_preview_url\":\"https://canvas.preview.host/creatives/117375/preview?isCompactPreview=true\","
                        + "\"archive_url\":null,\"composed_from\":null,\"stock_creative_id\":null,\"yabs_data\":null,"
                        + "\"preset_id\":null,\"moderation_info\":{\"content_id\":null,\"html\":{},\"images\":[],"
                        + "\"texts\":null,\"videos\":null,\"sounds\":null,\"aspects\":null,\"bgrcolor\":null}," +
                        "\"additional_data\":null,"
                        + "\"expanded_preview_url\":null,"
                        + "\"has_packshot\":null, \"is_adaptive\":true, \"is_brand_lift\":null}},"
                        + "{\"creativeId\":2387863,"
                        + "\"ok\":false,\"message\":\"Not Found\"}]"))
                .andExpect(status().is(200));

    }

    @Test
    public void testGetPresets() throws Exception {
        mockMvc.perform(get("/direct/presets")
                .header(HttpHeaders.AUTHORIZATION, "empty_header"))
                .andExpect(status().is(200))
                .andExpect(result -> System.err.println("!!!" + result.getResponse().getContentAsString()))
                .andExpect(json().node("videoPresets").isArray())
                .andExpect(json().node("canvasPresets").isArray())
        ;
    }
}
