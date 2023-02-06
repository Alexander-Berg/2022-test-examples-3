package ru.yandex.canvas.controllers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.validation.presetbased.elements.NotWhitespaceValidator;
import ru.yandex.canvas.model.validation.presetbased.elements.TextLengthValidator;
import ru.yandex.canvas.model.validation.presetbased.elements.ValidColorValidator;
import ru.yandex.canvas.model.validation.presetbased.elements.ValidUnicodeSymbolsValidator;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checking element preset-based validators:
 * <li>{@link TextLengthValidator}</li>
 * <li>{@link NotWhitespaceValidator}</li>
 * <li>{@link ValidColorValidator}</li>
 * <li>{@link ValidUnicodeSymbolsValidator}</li>
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativesBatchesControllerElementValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testInvalidUnicodeFails() throws Exception {
        var batch = minValidBatch(1);
        addElement(batch, descriptionElement("qwe \uD83C\uDF74 qwe", "#FFFFFF"));

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0].options.content"),
                                contains(is("Invalid symbols")))));
    }

    @Test
    public void testWhitespaceContentFails() throws Exception {
        var batch = minValidBatch(1);
        addElement(batch, descriptionElement("               ", "#FFFFFF"));

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0].options.content"),
                                contains(is("Value must be not empty")))));
    }

    @Test
    public void testSizeLessThanMinimum() throws Exception {
        var batch = minValidBatch(1);
        addElement(batch, ageRestrictionElement("", "#ffffff"));

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0].options.value"),
                                contains(is("Choose a value")))));
    }

    @Test
    public void testSizeMoreThanMaximum() throws Exception {
        var batch = minValidBatch(1);
        addElement(batch, ageRestrictionElement("aaaaaaaaa", "#ffffff"));

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0].options.value"),
                                contains(is("Choose a value")))));
    }

    @Test
    public void testInvalidLegalColor() throws Exception {
        var batch = minValidBatch(1);
        addElement(batch, legalElement("#F", "#F"));

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0].options.color"),
                                contains(is("Invalid color format value")))))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0].options.iconColor"),
                                contains(is("Invalid color format value")))));
    }

    @Test
    public void testInvalidButtonColor() throws Exception {
        var batch = minValidBatch(1);
        addElement(batch, buttonElement("#F", "#F"));

        createAndExpectBadRequest(batch)
                .andExpect(jsonPath("$.message", is("Invalid batch creative request")))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0].options.color"),
                                contains(is("Invalid color format value")))))
                .andExpect(jsonPath("$.properties",
                        hasEntry(is("items[0].data.elements[0].options.backgroundColor"),
                                contains(is("Invalid color format value")))));
    }

    private ResultActions createAndExpectBadRequest(JSONObject batch) throws Exception {
        return createBatch(batch)
                .andExpect(e -> System.err.println("!!!!" + e.getResponse().getStatus()))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    private ResultActions createBatch(JSONObject batch) throws Exception {
        return this.mockMvc.perform(post("/creatives-batches").content(batch.toString())
                .param("client_id", String.valueOf(MockedTest.CLIENT_ID))
                .param("user_id", String.valueOf(MockedTest.USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));
    }

    private void addElement(JSONObject batch, JSONObject element) {
        batch.getJSONArray("items").getJSONObject(0)
                .getJSONObject("data").getJSONArray("elements").put(element);
    }

    private JSONObject minValidBatch(int presetId) {
        return new JSONObject()
                .put("name", "test_batch")
                .put("items", new JSONArray()
                        .put(new JSONObject()
                                .put("presetId", presetId)
                                .put("data", new JSONObject()
                                        .put("width", 300)
                                        .put("height", 250)
                                        .put("bundle", new JSONObject()
                                                .put("name", "media-banner_theme_divided")
                                                .put("version", 1))
                                        .put("options", new JSONObject()
                                                .put("borderColor", "#FF0000") // RED
                                                .put("backgroundColor", "#0000FF")) // BLUE
                                        .put("elements", new JSONArray())
                                        .put("mediaSets", new JSONObject()))));
    }

    private JSONObject descriptionElement(String content, String color) {
        return new JSONObject()
                .put("type", "description")
                .put("available", true)
                .put("options", new JSONObject()
                        .put("color", color)
                        .put("content", content));
    }

    private JSONObject ageRestrictionElement(String value, String color) {
        return new JSONObject()
                .put("type", "ageRestriction")
                .put("available", true)
                .put("options", new JSONObject()
                        .put("value", value)
                        .put("color", color));
    }

    private JSONObject legalElement(String color, String iconColor) {
        return new JSONObject()
                .put("type", "legal")
                .put("available", true)
                .put("options", new JSONObject()
                        .put("color", color)
                        .put("iconColor", iconColor)
                        .put("content", "Sample content"));
    }

    private JSONObject buttonElement(String color, String backgroundColor) {
        return new JSONObject()
                .put("type", "button")
                .put("available", true)
                .put("options", new JSONObject()
                        .put("color", color)
                        .put("backgroundColor", backgroundColor)
                        .put("content", "Sample content"));
    }
}
