package ru.yandex.canvas.controllers;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.CreativeDocumentBatch;
import ru.yandex.canvas.model.elements.ElementType;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.canvas.controllers.MockedTest.USER_ID;
import static ru.yandex.canvas.service.video.ButtonValidateTest.CLIENT_ID;
import static ru.yandex.canvas.service.video.ButtonValidateTest.OTHER_CLIENT_ID;


/**
 * @author skirsanov
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativesBatchesControllerTest {

    @Autowired
    private MockMvc mockMvc;


    private static JSONObject minValidBatch() throws JSONException {
        return new JSONObject()
                .put("name", "test_batch")
                .put("items", new JSONArray().put(new JSONObject()
                        .put("presetId", 1)
                        .put("data", new JSONObject()
                                .put("width", 300)
                                .put("height", 250)
                                .put("bundle", new JSONObject()
                                        .put("name", "media-banner_theme_pic-button-color-background")
                                        .put("version", 1))
                                .put("options", new JSONObject()
                                        .put("borderColor", "#FF0000") // RED
                                        .put("backgroundColor", "#0000FF")) // BLUE
                                .put("elements", new JSONArray())
                                .put("mediaSets", new JSONObject()))));
    }

    private static JSONObject element(final String type, final Map<String, String> options) throws JSONException {
        return new JSONObject()
                .put("type", type)
                .put("options", new JSONObject(options));
    }

    @Test
    public void testCreativeDataOptionsValidation() throws Exception {

        JSONObject batch = minValidBatch();

        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data").getJSONObject("options")
                .put("borderColor", "#AABBCZ")
                .put("backgroundColor", "#AABBCZ");

        this.mockMvc.perform(post("/creatives-batches").content(batch.toString())
                .param("client_id", String.valueOf(CLIENT_ID))
                .param("user_id", String.valueOf(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties", allOf(
                        hasEntry(is("items[0].data.options.borderColor"),
                                contains(is("Invalid color format value"))),
                        hasEntry(is("items[0].data.options.backgroundColor"),
                                contains(is("Invalid color format value")))
                )));
    }

    @Test
    public void testCreativeOkTest() throws Exception {

        JSONObject batch = minValidBatch();

     /*   batch.getJSONArray("items").getJSONObject(0).getJSONObject("data").getJSONObject("options")
                .put("borderColor", "#AABBCZ")
                .put("backgroundColor", "#AABBCZ");

      */

        ObjectMapper objectMapper = new ObjectMapper();

        final CreativeDocumentBatch[] firstResponse = new CreativeDocumentBatch[2];

        mockMvc.perform(post("/creatives-batches").content(batch.toString())
                .param("client_id", String.valueOf(CLIENT_ID))
                .param("user_id", String.valueOf(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is2xxSuccessful())
                .andDo(e -> firstResponse[0] = objectMapper.readValue(e.getResponse().getContentAsString(),
                        CreativeDocumentBatch.class))
                .andExpect(jsonPath("items[0].id", greaterThanOrEqualTo(0))
                );

        mockMvc.perform(post("/creatives-batches").content(batch.toString())
                .param("client_id", String.valueOf(OTHER_CLIENT_ID))
                .param("user_id", String.valueOf(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is2xxSuccessful())
                .andDo(e -> firstResponse[1] = objectMapper.readValue(e.getResponse().getContentAsString(),
                        CreativeDocumentBatch.class))
                .andExpect(jsonPath("items[0].id", greaterThanOrEqualTo(0))
                );

        mockMvc.perform(get("/direct/creatives")
                .param("ids", String.join(",", firstResponse[0].getItems().get(0).getId() + "",
                        firstResponse[1].getItems().get(0).getId() + ""))
                .param("client_id", String.valueOf(CLIENT_ID))
                .header(HttpHeaders.AUTHORIZATION, "empty_header")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].ok", is(true)))
                .andExpect(jsonPath("$.[1].ok", is(false)));
    }


    @Test
    public void testButtonElementValidation() throws Exception {
        for (String invalidContent : new String[]{" ", "\n", "\t", " \t"}) {

            final JSONObject batch = minValidBatch();

            batch.put("name", invalidContent);

            batch.getJSONArray("items").getJSONObject(0).getJSONObject("data").getJSONArray("elements")
                    .put(element(ElementType.BUTTON, ImmutableMap.of(
                            "content", invalidContent,
                            "color", "#AABBCC",
                            "backgroundColor", "#AABBEE")))
                    .put(element(ElementType.DESCRIPTION, ImmutableMap.of(
                            "content", invalidContent,
                            "color", "#AABBCC")))
                    .put(element(ElementType.AGE_RESTRICTION, ImmutableMap.of(
                            "value", invalidContent,
                            "color", "#AABBCC")));

            this.mockMvc.perform(post("/creatives-batches").content(batch.toString())
                    .param("client_id", String.valueOf(CLIENT_ID))
                    .param("user_id", String.valueOf(USER_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                    .andExpect(jsonPath("$.properties", allOf(
                            hasEntry(is("name"),
                                    contains(is("must not be blank"))),
                            hasEntry(is("items[0].data.elements[0].options.content"),
                                    contains(is("Value must be not empty"))),
                            hasEntry(is("items[0].data.elements[1].options.content"),
                                    contains(is("Value must be not empty"))),
                            hasEntry(is("items[0].data.elements[2].options.value"),
                                    contains(is("Value must be not empty")))
                    )));
        }
    }

    @Test
    public void testCreateBatchWithoutName() throws Exception {
        final JSONObject batch1 = minValidBatch();
        batch1.remove("name");

        final JSONObject batch2 = minValidBatch();
        batch2.put("name", "");

        for (JSONObject batch : new JSONObject[]{batch1, batch2}) {
            this.mockMvc.perform(post("/creatives-batches").content(batch.toString())
                    .param("client_id", String.valueOf(CLIENT_ID))
                    .param("user_id", String.valueOf(USER_ID))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                    .andExpect(jsonPath("$.properties",
                            hasEntry(is("name"), contains(is("must not be blank")))
                    ));
        }
    }

    @Test
    public void testCreativeDataWithoutMediaSetFiles() throws Exception {
        JSONObject batch = minValidBatch();

        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data").getJSONArray("elements")
                .put(new JSONObject()
                        .put("type", "image")
                        .put("mediaSet", "test_media_set")
                        .put("options", new JSONObject(ImmutableMap.of(
                                "width", 100,
                                "height", 200))))
                .put(element(ElementType.BUTTON, ImmutableMap.of("content", "magic_button", "color", "#DEADAB")));

        batch.getJSONArray("items").getJSONObject(0).getJSONObject("data")
                .put("mediaSets", new JSONObject().put("test_media_set",
                        new JSONObject().put("items", new JSONArray()
                                .put(new JSONObject().put("type", "image")
                                        .put("items", new JSONArray().put(new JSONObject()
                                                .put("width", 200)
                                                .put("height", 300)
                                                .put("alias", "ololo")
                                                .put("url",
                                                        "https://storage.mds.yandex"
                                                                + ".net/get-bstor/15932/0c885735-60ec-4270-ade0"
                                                                + "-2f2a2774ac08.jpeg")
                                        ))
                                ))
                ));

        this.mockMvc.perform(post("/creatives-batches").content(batch.toString())
                .param("client_id", String.valueOf(CLIENT_ID))
                .param("user_id", String.valueOf(USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties", hasEntry(is("items[0].data.elements[0]"),
                        containsInAnyOrder(is("Upload an image"), is("Error adding image, please try again")))));
    }
}
