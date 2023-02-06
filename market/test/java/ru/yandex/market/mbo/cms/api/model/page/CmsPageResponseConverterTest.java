package ru.yandex.market.mbo.cms.api.model.page;

import java.util.Date;
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
 * @date 20.03.18
 */
@Issue("MBO-14938")
public class CmsPageResponseConverterTest {
    private static final int VIDEO_WIDTH = 800;
    private static final int VIDEO_HEIGHT = 500;
    public static final Long PAGE_ID = 101L;
    public static final Long REVISION_ID_1 = 201L;
    public static final Date REVISION_DATE_1 = new Date(1000);
    public static final Long REVISION_ID_2 = 202L;
    public static final Date REVISION_DATE_2 = new Date(2000);

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
    public void convertProductPageWithVideoWidget() throws ConversionException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageName("PageName")
                .beginRootWidget("PRODUCT_CONTEXT")
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

        CmsPage expectedOuterPage = new CmsPage();
        expectedOuterPage.getMetadata()
                .setPageType("product")
                .setPageName("PageName")
                .setPublished(false);
        expectedOuterPage.addRow(
                new CmsPage.Row().addColumn(
                        new CmsPage.Column().addWidget(
                                new CmsPage.VideoWidget()
                                        .setUrl("https://video.example.com/example-video")
                                        .setWidth(VIDEO_WIDTH)
                                        .setHeight(VIDEO_HEIGHT)
                        )
                )
        );

        CmsPage actualOuterPage = CmsPageResponseConverter.convert(innerPage);

        Assert.assertEquals(expectedOuterPage, actualOuterPage);
    }

    @Test
    @Issue("MBO-15059")
    public void isPublishedFalseWhenRevisionIsNotPublished() throws ConversionException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE_ID)
                .revisionId(REVISION_ID_2)
                .pageName("PageName")
                .latestRevisionId(REVISION_ID_2)
                .latestRevisionDate(REVISION_DATE_2)
                .publishedRevisionId(REVISION_ID_1)
                .publishedRevisionDate(REVISION_DATE_1)
                .build();

        CmsPage actualOuterPage = CmsPageResponseConverter.convert(innerPage);

        CmsPage expectedOuterPage = new CmsPage();
        expectedOuterPage.getMetadata()
                .setPageType("product")
                .setPageId(PAGE_ID)
                .setRevisionId(REVISION_ID_2)
                .setPageName("PageName")
                .setPublished(false);
        Assert.assertEquals(expectedOuterPage, actualOuterPage);
    }

    @Test
    @Issue("MBO-15059")
    public void isPublishedTrueWhenRevisionIsPublished() throws ConversionException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE_ID)
                .revisionId(REVISION_ID_1)
                .pageName("PageName")
                .latestRevisionId(REVISION_ID_2)
                .latestRevisionDate(REVISION_DATE_2)
                .publishedRevisionId(REVISION_ID_1)
                .publishedRevisionDate(REVISION_DATE_1)
                .build();

        CmsPage actualOuterPage = CmsPageResponseConverter.convert(innerPage);

        CmsPage expectedOuterPage = new CmsPage();
        expectedOuterPage.getMetadata()
                .setPageType("product")
                .setPageId(PAGE_ID)
                .setRevisionId(REVISION_ID_1)
                .setPageName("PageName")
                .setPublished(true);
        Assert.assertEquals(expectedOuterPage, actualOuterPage);
    }
}
