package ru.yandex.market.partner.notification.service.mustache.template_renderer;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class MustacheRendererTest {
    private ObjectMapper mapper;
    private MustacheRenderer renderer;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        renderer = new MustacheRenderer();
    }

    @Test
    public void renderFromJsonObjectAsXmlList() throws IOException {
        var values = mapper.readValue(
                "{\n" +
                        "  \"names\": {\n" +
                        "    \"name\": [\n" +
                        "      {\n" +
                        "        \"value\": \"World\",\n" +
                        "        \"exclamation\": \"!\"\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"value\": \"Universe\",\n" +
                        "        \"exclamation\": \"!!!\"\n" +
                        "      }\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}",
                JsonNode.class
        );

        assertThat(
                renderer.render(
                        "{{#names.name}}Hello, {{value}}{{exclamation}}\n{{/names.name}}",
                        values
                ),
                equalTo("Hello, World!\nHello, Universe!!!")
        );
    }

    @Test
    public void renderFromJsonObjectAsXmlListOneElement() throws IOException {
        var values = mapper.readValue(
                "{\n" +
                        "  \"names\": {\n" +
                        "    \"name\": {\n" +
                        "      \"value\": \"World\",\n" +
                        "      \"exclamation\": \"!\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}",
                JsonNode.class
        );

        assertThat(
                renderer.render(
                        "{{#names.name}}Hello, {{value}}{{exclamation}}\n{{/names.name}}",
                        values
                ),
                equalTo("Hello, World!")
        );
    }

    @Test
    public void renderFromJsonArray() throws IOException {
        var values = mapper.readValue("[\"Universe\", \"World\"]", JsonNode.class);

        assertThat(
                renderer.render(
                        "{{#.}}Hello, {{.}}!\n{{/.}}",
                        values
                ),
                equalTo("Hello, Universe!\nHello, World!")
        );
    }
}
