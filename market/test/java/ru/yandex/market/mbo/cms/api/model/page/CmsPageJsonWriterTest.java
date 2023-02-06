package ru.yandex.market.mbo.cms.api.model.page;

import java.io.IOException;
import java.io.StringWriter;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.common.validation.json.JsonSchemaValidator;
import ru.yandex.market.mbo.common.validation.json.JsonValidationException;

/**
 * @author ayratgdl
 * @date 20.03.18
 */
@Issue("MBO-14938")
public class CmsPageJsonWriterTest {
    private static final long PAGE_ID_1 = 101;
    private static final long REVISION_1 = 201;
    private static final long PRODUCT_ID = 301;
    private static final long BRAND_ID = 401;

    private static final String PAGE_JSON_SCHEMA =
            "/mbo-cms-api/schemas/editor/cms-page.schema.json";

    private JsonSchemaValidator validator;

    @Before
    public void setUp() {
        validator = JsonSchemaValidator.createFromClasspath(PAGE_JSON_SCHEMA);
    }

    @Test
    @Issue("MBO-15059")
    public void writeJson() throws IOException, JsonValidationException {
        CmsPage page = new CmsPage();
        page.getMetadata()
                .setPageId(PAGE_ID_1)
                .setRevisionId(REVISION_1)
                .setPageType("product")
                .setPageName("PageName")
                .setPublished(true);
        page
                .addLink(new CmsPage.Link("productId").addId(PRODUCT_ID))
                .addLink(new CmsPage.Link("brandId").addId(BRAND_ID));
        page.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.RichTextWidget()
                                        .setText("<b>Bold text</b>")
                        )
                )
        );

        String expectedJson = "{\n" +
                "  \"metadata\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"revisionId\": 201,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\",\n" +
                "    \"isPublished\": true\n" +
                "  },\n" +
                "  \"links\": {\n" +
                "    \"brandId\": [\n" +
                "      401\n" +
                "    ],\n" +
                "    \"productId\": [\n" +
                "      301\n" +
                "    ]\n" +
                "  },\n" +
                "  \"rows\": [\n" +
                "    {\n" +
                "      \"entity\": \"row\",\n" +
                "      \"columns\": [\n" +
                "        {\n" +
                "          \"entity\": \"column\",\n" +
                "          \"widgets\": [\n" +
                "            {\n" +
                "              \"entity\": \"widget\",\n" +
                "              \"type\": \"RichText\",\n" +
                "              \"html\": \"<b>Bold text</b>\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        validator.validate(expectedJson);

        StringWriter writer = new StringWriter();
        CmsPageJsonWriter.write(writer, page);
        String actualJson = writer.toString();

        Assert.assertEquals(expectedJson, actualJson);
    }
}
