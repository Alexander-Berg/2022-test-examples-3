package ru.yandex.market.mbo.cms.api.model.page;

import java.io.IOException;
import java.io.StringReader;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.common.validation.json.JsonSchemaValidator;
import ru.yandex.market.mbo.common.validation.json.JsonValidationException;

/**
 * @author ayratgdl
 * @date 12.03.18
 */
@Issue("MBO-14811")
@Issue("MBO-14938")
public class CmsPageJsonReaderTest {
    private static final Long PRODUCT_1 = 101L;
    private static final Long BRAND_1 = 201L;
    private static final int VIDEO_WIDTH = 800;
    private static final int VIDEO_HEIGHT = 500;

    private static final String PAGE_JSON_SCHEMA =
            "/mbo-cms-api/schemas/editor/cms-page.schema.json";

    private JsonSchemaValidator validator;

    @Before
    public void setUp() {
        validator = JsonSchemaValidator.createFromClasspath(PAGE_JSON_SCHEMA);
    }

    @Test
    public void readEmptyJson() throws IOException {
        CmsPage cmsPage = CmsPageJsonReader.read(new StringReader("{}"));
        Assert.assertEquals(new CmsPage(), cmsPage);
    }

    @Test
    public void readMetadata() throws IOException, JsonValidationException {
        String json = "{\n" +
                "  \"metadata\": {\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\"\n" +
                "  },\n" +
                "  \"links\": {},\n" +
                "  \"rows\": []\n" +
                "}";
        validator.validate(json);

        CmsPage actualCmsPage = CmsPageJsonReader.read(new StringReader(json));

        CmsPage expectedCmsPage = new CmsPage();
        expectedCmsPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");

        Assert.assertEquals(expectedCmsPage, actualCmsPage);
    }

    @Test
    public void readLinks() throws IOException, JsonValidationException {
        String json = "{\n" +
                "  \"metadata\": {\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\"\n" +
                "  },\n" +
                "  \"links\": {\n" +
                "    \"productId\": [101],\n" +
                "    \"brandId\": [201]\n" +
                "  },\n" +
                "  \"rows\": []\n" +
                "}";
        validator.validate(json);

        CmsPage actualCmsPage = CmsPageJsonReader.read(new StringReader(json));

        CmsPage expectedCmsPage = new CmsPage();
        expectedCmsPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        expectedCmsPage
                .addLink(new CmsPage.Link("productId").addId(PRODUCT_1))
                .addLink(new CmsPage.Link("brandId").addId(BRAND_1));

        Assert.assertEquals(expectedCmsPage, actualCmsPage);
    }

    @Test
    public void readRows() throws IOException, JsonValidationException {
        String json = "{\n" +
                "  \"metadata\": {\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\"\n" +
                "  },\n" +
                "  \"links\": {\n" +
                "    \"productId\": [101],\n" +
                "    \"brandId\": [201]\n" +
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
        validator.validate(json);

        CmsPage actualCmsPage = CmsPageJsonReader.read(new StringReader(json));

        CmsPage expectedCmsPage = new CmsPage();
        expectedCmsPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        expectedCmsPage
                .addLink(new CmsPage.Link("productId").addId(PRODUCT_1))
                .addLink(new CmsPage.Link("brandId").addId(BRAND_1));
        expectedCmsPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.RichTextWidget().setText("<b>Bold text</b>")
                        )
                )
        );

        Assert.assertEquals(expectedCmsPage, actualCmsPage);
    }

    @Test
    public void readVideoWidget() throws IOException, JsonValidationException {
        String json = "{\n" +
                "  \"metadata\": {\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\"\n" +
                "  },\n" +
                "  \"links\": {},\n" +
                "  \"rows\": [\n" +
                "    {\n" +
                "      \"entity\": \"row\",\n" +
                "      \"columns\": [\n" +
                "        {\n" +
                "          \"entity\": \"column\",\n" +
                "          \"widgets\": [\n" +
                "            {\n" +
                "              \"entity\": \"widget\",\n" +
                "              \"type\": \"Video\",\n" +
                "              \"url\": \"https://video.example.com/example-video\",\n" +
                "              \"width\": 800,\n" +
                "              \"height\": 500\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        validator.validate(json);

        CmsPage actualCmsPage = CmsPageJsonReader.read(new StringReader(json));

        CmsPage expectedCmsPage = new CmsPage();
        expectedCmsPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        expectedCmsPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.VideoWidget()
                                        .setUrl("https://video.example.com/example-video")
                                        .setWidth(VIDEO_WIDTH)
                                        .setHeight(VIDEO_HEIGHT)
                        )
                )
        );

        Assert.assertEquals(expectedCmsPage, actualCmsPage);
    }
}
