package ru.yandex.market.mbo.cms.api.model.page;

import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.core.models.ViewTemplatePageBuilder;

/**
 * @author ayratgdl
 * @date 13.03.18
 */
@Issue("MBO-14811")
@Issue("MBO-14938")
public class CmsPageRequestConverterTest {
    private static final Long BRAND_ID = 101L;
    private static final Long PRODUCT_ID = 201L;
    private static final int VIDEO_WIDTH = 800;
    private static final int VIDEO_HEIGHT = 500;

    private CmsSchema schema;

    @Before
    public void setUp() {
        Map<String, DocumentDescription> descriptions = new HashMap<>();
        schema = new CmsSchema();
        String namespace = "abc";
        schema.setNamespace(namespace);
        descriptions.put("product", new DocumentDescription(namespace, "product"));
        schema.getDocuments().putAll(descriptions);
    }

    @Test
    public void convertEmptyProductPage() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);
        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }

    @Test
    public void convertProductPageWithLinks() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage
                .addLink(new CmsPage.Link("brandId").addId(BRAND_ID))
                .addLink(new CmsPage.Link("productId").addId(PRODUCT_ID));

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_BRAND_ID")
                .parameter("BRAND_ID", BRAND_ID.toString())
                .endWidget()
                .beginWidget("PAGE_LINK", "PAGE_LINK_PRODUCT")
                .parameter("MODEL_ID", PRODUCT_ID.toString())
                .endWidget()
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);

        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }

    @Test
    public void convertProductPageWithRichTextWidget() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.RichTextWidget().setText("<b>Bold text</b>")
                        )
                )
        );

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .beginWidget("COLUMN", "COLUMN_SCREEN")
                .beginWidget("WIDGETS", "WIDGET_TEXT")
                .parameter("RICH_TEXT", "<b>Bold text</b>")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);

        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }

    @Test
    public void convertProductPageWithRichTextWidgetWithoutText() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.RichTextWidget()
                        )
                )
        );

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .beginWidget("COLUMN", "COLUMN_SCREEN")
                .beginWidget("WIDGETS", "WIDGET_TEXT")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);

        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }

    @Test
    public void convertProductPageWithImageWidget() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.ImageWidget().setUrl("http://img.example.com/image.png")
                        )
                )
        );

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .beginWidget("COLUMN", "COLUMN_SCREEN")
                .beginWidget("WIDGETS", "WIDGET_IMAGE_LINK")
                .beginWidget("IMAGES", "IMAGE_S")
                .parameter("IMAGE_URL", "http://img.example.com/image.png")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);

        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }

    @Test
    public void convertProductPageWithImageWidgetWithoutUrl() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.ImageWidget()
                        )
                )
        );

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .beginWidget("COLUMN", "COLUMN_SCREEN")
                .beginWidget("WIDGETS", "WIDGET_IMAGE_LINK")
                .beginWidget("IMAGES", "IMAGE_S")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);

        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }

    @Test
    public void convertProductPageWithVideoWidget() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.VideoWidget()
                                        .setUrl("https://video.example.com/example-video")
                                        .setWidth(VIDEO_WIDTH)
                                        .setHeight(VIDEO_HEIGHT)
                        )
                )
        );

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .beginWidget("COLUMN", "COLUMN_SCREEN")
                .beginWidget("WIDGETS", "W_VIDEO")
                .parameter("VIDEO_URL", "https://video.example.com/example-video")
                .parameter("VIDEO_WIDTH", VIDEO_WIDTH)
                .parameter("VIDEO_HEIGHT", VIDEO_HEIGHT)
                .parameter("VIDEO_HOSTING", "video.example.com")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);

        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }

    @Test
    public void convertProductPageWithVideoWidgetWithoutUrl() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.VideoWidget()
                                        .setWidth(VIDEO_WIDTH)
                                        .setHeight(VIDEO_HEIGHT)
                        )
                )
        );

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .beginWidget("COLUMN", "COLUMN_SCREEN")
                .beginWidget("WIDGETS", "W_VIDEO")
                .parameter("VIDEO_WIDTH", VIDEO_WIDTH)
                .parameter("VIDEO_HEIGHT", VIDEO_HEIGHT)
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);

        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }

    @Test(expected = ConversionException.class)
    public void convertPageWithVideoWidgetWithWrongUrl() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.VideoWidget()
                                        .setUrl("-")
                                        .setWidth(VIDEO_WIDTH)
                                        .setHeight(VIDEO_HEIGHT)
                        )
                )
        );

        CmsPageRequestConverter.convert(outerPage, schema);
    }

    @Test
    @Issue("MBO-15156")
    public void convertPageWithRowWithTwoColumns() throws ConversionException {
        CmsPage outerPage = new CmsPage();
        outerPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName");
        outerPage.addRow(
                new CmsPage.Row()
                        .addColumn(
                                new CmsPage.Column().addWidget(new CmsPage.RichTextWidget().setText("<b>Column 1</b>"))
                        )
                        .addColumn(
                                new CmsPage.Column().addWidget(new CmsPage.RichTextWidget().setText("<b>Column 2</b>"))
                        )
        );

        Page expectedInnerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
                .parameter("HAS_CONTEXT_PARAMS", "true")
                .parameter("TYPE", "product")
                .beginWidget("LINKS", "PAGE_LINKS")
                .beginWidget("PAGE_LINK", "PAGE_LINK_DOMAIN")
                .parameter("DOMAIN", "ru")
                .endWidget()
                .endWidget()
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_2_320")
                .beginWidget("COLUMN1", "COLUMN_320")
                .beginWidget("WIDGETS", "WIDGET_TEXT")
                .parameter("RICH_TEXT", "<b>Column 1</b>")
                .endWidget()
                .endWidget()
                .beginWidget("COLUMN2", "COLUMN_320")
                .beginWidget("WIDGETS", "WIDGET_TEXT")
                .parameter("RICH_TEXT", "<b>Column 2</b>")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();

        Page actualInnerPage = CmsPageRequestConverter.convert(outerPage, schema);

        Assert.assertTrue(expectedInnerPage.equalsForTest(actualInnerPage));
    }
}
