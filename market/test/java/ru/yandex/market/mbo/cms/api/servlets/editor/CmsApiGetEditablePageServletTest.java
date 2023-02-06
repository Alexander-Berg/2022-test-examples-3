package ru.yandex.market.mbo.cms.api.servlets.editor;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.mbo.cms.core.helpers.PermissionCheckHelper;
import ru.yandex.market.mbo.cms.core.models.ClientDescription;
import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.core.models.ViewTemplatePageBuilder;
import ru.yandex.market.mbo.cms.core.service.CmsService;
import ru.yandex.market.mbo.cms.core.service.CmsServiceDaoMock;
import ru.yandex.market.mbo.cms.core.service.TypografService;
import ru.yandex.market.mbo.cms.core.utils.CmsTemplateUtil;
import ru.yandex.market.mbo.common.validation.json.JsonSchemaValidator;
import ru.yandex.market.mbo.common.validation.json.JsonValidationException;

/**
 * @author ayratgdl
 * @date 01.03.18
 */
public class CmsApiGetEditablePageServletTest {
    private static final Long PAGE1 = 101L;
    private static final Long REVISION1 = 201L;
    private static final Long REVISION2 = 202L;
    private static final Date INSTANT1 = new Date(1000);
    private static final Date INSTANT2 = new Date(2000);
    private static final Long PRODUCT1 = 301L;
    private static final Long BRAND1 = 401L;
    private static final Long NID1 = 501L;
    private static final Long USER_ID = 601L;

    private static final String RESPONSE_SCHEMA_PATH =
            "/mbo-cms-api/schemas/editor/get-page-response.schema.json";
    private static final String PAGE_SCHEMA_PATH =
            "/mbo-cms-api/schemas/editor/cms-page.schema.json";
    private static final String PAGE_INFO_SCHEMA_PATH =
            "/mbo-cms-api/schemas/editor/cms-page-info.schema.json";

    private CmsApiGetEditablePageServlet servlet;
    private CmsService cmsService;
    private CmsServiceDaoMock cmsServiceDao;
    private MockHttpServletResponse response;
    private JsonSchemaValidator validator;
    private CmsSchema schema;

    @Before
    public void setUp() {
        cmsServiceDao = new CmsServiceDaoMock();
        cmsService = Mockito.spy(new CmsService(Mockito.mock(CmsTemplateUtil.class),
                Mockito.mock(TypografService.class)));
        cmsService.setCmsServiceDao(cmsServiceDao);
        cmsService.setPageDao(cmsServiceDao.getPageDao());

        servlet = new CmsApiGetEditablePageServlet();
        servlet.setCmsService(cmsService);
        servlet.setPermissionCheckHelper(Mockito.mock(PermissionCheckHelper.class));

        response = new MockHttpServletResponse();

        validator = JsonSchemaValidator.createFromClasspath(RESPONSE_SCHEMA_PATH, PAGE_SCHEMA_PATH,
                PAGE_INFO_SCHEMA_PATH);

        schema = new CmsSchema();
        String namespace = "abc";
        schema.setNamespace(namespace);

        Map<String, DocumentDescription> descriptions = new HashMap<>();
        DocumentDescription dv = new DocumentDescription(namespace, "product");
        dv.setExports(Collections.emptyList());
        descriptions.put("product", dv);

        ClientDescription clientDescriptionDesktop = new ClientDescription();
        clientDescriptionDesktop.setHostPreview("preview.example.org");

        Map<String, ClientDescription> clientDescriptions = new HashMap<>();
        clientDescriptions.put("market_desktop", clientDescriptionDesktop);

        schema.getClients().putAll(clientDescriptions);
        schema.getDocuments().putAll(descriptions);

        Mockito.doReturn(schema).when(cmsService).getSchema(namespace);
    }

    // Tests of handling of requests parameters

    @Test
    public void handleRequestWhereAbsentPageIdParameter()
            throws ServletException, IOException, JsonValidationException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"errorCode\": \"BAD_PARAMETERS_REQUEST\",\n" +
                "  \"message\": \"Absent required request parameter 'page-id'.\"\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    public void handleRequestWherePageIdParameterIsNotNumber()
            throws ServletException, IOException, JsonValidationException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", "notNumber");
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"errorCode\": \"BAD_PARAMETERS_REQUEST\",\n" +
                "  \"message\": \"Request parameter 'page-id' should be number. Actual value = 'notNumber'.\"\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    public void handleRequestWhereRevisionIdParameterIsNotNumber() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", "101");
        request.setParameter("revision-id", "notNumber");
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"errorCode\": \"BAD_PARAMETERS_REQUEST\",\n" +
                "  \"message\": \"Request parameter 'revision-id' should be number. Actual value = 'notNumber'.\"\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
    }

    // Tests of handling of case not found page

    @Test
    public void getAbsentPage() throws ServletException, IOException, JsonValidationException {
        addPageToCmsService(PAGE1, REVISION1, null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"errorCode\": \"NOT_FOUND\",\n" +
                "  \"message\": \"Not found page with id '101' and revision '201'.\"\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    // Tests convert page

    @Test
    @Issue("MBO-15059")
    public void getEmptyPage() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("page name")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"isReactPage\": true,\n" +
                "  \"pageInfo\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"page name\",\n" +
                "    \"latestRevision\": {\n" +
                "      \"revisionId\": 201,\n" +
                "      \"revisionDate\": 1000\n" +
                "    },\n" +
                "    \"publishedRevision\": null,\n" +
                "    \"desktopMarketUrl\": null,\n" +
                "    \"touchMarketUrl\": null,\n" +
                "    \"desktopPreviewUrl\": null,\n" +
                "    \"touchPreviewUrl\": null,\n" +
                "    \"brandId\": null,\n" +
                "    \"productId\": null\n" +
                "  },\n" +
                "  \"page\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageId\": 101,\n" +
                "      \"revisionId\": 201,\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"page name\",\n" +
                "      \"isPublished\": false\n" +
                "    },\n" +
                "    \"links\": {},\n" +
                "    \"rows\": []\n" +
                "  }\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    @Issue("MBO-15059")
    public void getEmptyPageWithFilledMetadataFields() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("PageName")
                .latestRevisionId(REVISION2)
                .latestRevisionDate(INSTANT2)
                .publishedRevisionId(REVISION1)
                .publishedRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"isReactPage\": true,\n" +
                "  \"pageInfo\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\",\n" +
                "    \"latestRevision\": {\n" +
                "      \"revisionId\": 202,\n" +
                "      \"revisionDate\": 2000\n" +
                "    },\n" +
                "    \"publishedRevision\": {\n" +
                "      \"revisionId\": 201,\n" +
                "      \"revisionDate\": 1000\n" +
                "    },\n" +
                "    \"desktopMarketUrl\": null,\n" +
                "    \"touchMarketUrl\": null,\n" +
                "    \"desktopPreviewUrl\": null,\n" +
                "    \"touchPreviewUrl\": null,\n" +
                "    \"brandId\": null,\n" +
                "    \"productId\": null\n" +
                "  },\n" +
                "  \"page\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageId\": 101,\n" +
                "      \"revisionId\": 201,\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"PageName\",\n" +
                "      \"isPublished\": true\n" +
                "    },\n" +
                "    \"links\": {},\n" +
                "    \"rows\": []\n" +
                "  }\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    @Issue("MBO-15059")
    public void getPageWithLinks() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("PageName")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .beginWidget("LINKS", "UNKNOWN")
                .beginWidget("PAGE_LINK", "PAGE_LINK_PRODUCT")
                .parameter("MODEL_ID", PRODUCT1)
                .endWidget()
                .beginWidget("PAGE_LINK", "PAGE_LINK_BRAND_ID")
                .parameter("BRAND_ID", BRAND1)
                .endWidget()
                .beginWidget("PAGE_LINK", "PAGE_LINK_NID")
                .parameter("NID", NID1)
                .endWidget()
                .endWidget()
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"isReactPage\": true,\n" +
                "  \"pageInfo\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\",\n" +
                "    \"latestRevision\": {\n" +
                "      \"revisionId\": 201,\n" +
                "      \"revisionDate\": 1000\n" +
                "    },\n" +
                "    \"publishedRevision\": null,\n" +
                "    \"desktopMarketUrl\": null,\n" +
                "    \"touchMarketUrl\": null,\n" +
                "    \"desktopPreviewUrl\": null,\n" +
                "    \"touchPreviewUrl\": null,\n" +
                "    \"brandId\": null,\n" +
                "    \"productId\": null\n" +
                "  },\n" +
                "  \"page\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageId\": 101,\n" +
                "      \"revisionId\": 201,\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"PageName\",\n" +
                "      \"isPublished\": false\n" +
                "    },\n" +
                "    \"links\": {\n" +
                "      \"brandId\": [\n" +
                "        401\n" +
                "      ],\n" +
                "      \"productId\": [\n" +
                "        301\n" +
                "      ]\n" +
                "    },\n" +
                "    \"rows\": []\n" +
                "  }\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    @Issue("MBO-15059")
    public void getPageWithSingleEmptyRow() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("PageName")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .endWidget()
                .endWidget()
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"isReactPage\": false,\n" +
                "  \"pageInfo\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\",\n" +
                "    \"latestRevision\": {\n" +
                "      \"revisionId\": 201,\n" +
                "      \"revisionDate\": 1000\n" +
                "    },\n" +
                "    \"publishedRevision\": null,\n" +
                "    \"desktopMarketUrl\": null,\n" +
                "    \"touchMarketUrl\": null,\n" +
                "    \"desktopPreviewUrl\": null,\n" +
                "    \"touchPreviewUrl\": null,\n" +
                "    \"brandId\": null,\n" +
                "    \"productId\": null\n" +
                "  },\n" +
                "  \"page\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageId\": 101,\n" +
                "      \"revisionId\": 201,\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"PageName\",\n" +
                "      \"isPublished\": false\n" +
                "    },\n" +
                "    \"links\": {},\n" +
                "    \"rows\": [\n" +
                "      {\n" +
                "        \"entity\": \"row\",\n" +
                "        \"columns\": []\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    @Issue("MBO-15059")
    public void getPageWithTwoColumnsRowThatContainsOneColumn()
            throws ServletException, IOException, JsonValidationException {

        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("PageName")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_2_320")
                .beginWidget("COLUMN1", "COLUMN_320")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"isReactPage\": false,\n" +
                "  \"pageInfo\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\",\n" +
                "    \"latestRevision\": {\n" +
                "      \"revisionId\": 201,\n" +
                "      \"revisionDate\": 1000\n" +
                "    },\n" +
                "    \"publishedRevision\": null,\n" +
                "    \"desktopMarketUrl\": null,\n" +
                "    \"touchMarketUrl\": null,\n" +
                "    \"desktopPreviewUrl\": null,\n" +
                "    \"touchPreviewUrl\": null,\n" +
                "    \"brandId\": null,\n" +
                "    \"productId\": null\n" +
                "  },\n" +
                "  \"page\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageId\": 101,\n" +
                "      \"revisionId\": 201,\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"PageName\",\n" +
                "      \"isPublished\": false\n" +
                "    },\n" +
                "    \"links\": {},\n" +
                "    \"rows\": [\n" +
                "      {\n" +
                "        \"entity\": \"row\",\n" +
                "        \"columns\": [\n" +
                "          {\n" +
                "            \"entity\": \"column\",\n" +
                "            \"widgets\": []\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    @Issue("MBO-15059")
    public void getPageWithTextWidget() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("PageName")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .beginWidget("COLUMN", "COLUMN_SCREEN")
                .beginWidget("WIDGETS", "WIDGET_TEXT")
                .parameter("RICH_TEXT", "<b>text with html tags</b>")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"isReactPage\": false,\n" +
                "  \"pageInfo\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\",\n" +
                "    \"latestRevision\": {\n" +
                "      \"revisionId\": 201,\n" +
                "      \"revisionDate\": 1000\n" +
                "    },\n" +
                "    \"publishedRevision\": null,\n" +
                "    \"desktopMarketUrl\": null,\n" +
                "    \"touchMarketUrl\": null,\n" +
                "    \"desktopPreviewUrl\": null,\n" +
                "    \"touchPreviewUrl\": null,\n" +
                "    \"brandId\": null,\n" +
                "    \"productId\": null\n" +
                "  },\n" +
                "  \"page\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageId\": 101,\n" +
                "      \"revisionId\": 201,\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"PageName\",\n" +
                "      \"isPublished\": false\n" +
                "    },\n" +
                "    \"links\": {},\n" +
                "    \"rows\": [\n" +
                "      {\n" +
                "        \"entity\": \"row\",\n" +
                "        \"columns\": [\n" +
                "          {\n" +
                "            \"entity\": \"column\",\n" +
                "            \"widgets\": [\n" +
                "              {\n" +
                "                \"entity\": \"widget\",\n" +
                "                \"type\": \"RichText\",\n" +
                "                \"html\": \"<b>text with html tags</b>\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    @Issue("MBO-15059")
    public void getPageWithImageWidget() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("PageName")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "ROW_SCREEN_SCREEN")
                .beginWidget("COLUMN", "COLUMN_SCREEN")
                .beginWidget("WIDGETS", "WIDGET_IMAGE_LINK")
                .beginWidget("IMAGES", "IMAGE")
                .parameter("IMAGE_URL", "https://images.example.com/image.png")
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"isReactPage\": false,\n" +
                "  \"pageInfo\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"PageName\",\n" +
                "    \"latestRevision\": {\n" +
                "      \"revisionId\": 201,\n" +
                "      \"revisionDate\": 1000\n" +
                "    },\n" +
                "    \"publishedRevision\": null,\n" +
                "    \"desktopMarketUrl\": null,\n" +
                "    \"touchMarketUrl\": null,\n" +
                "    \"desktopPreviewUrl\": null,\n" +
                "    \"touchPreviewUrl\": null,\n" +
                "    \"brandId\": null,\n" +
                "    \"productId\": null\n" +
                "  },\n" +
                "  \"page\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageId\": 101,\n" +
                "      \"revisionId\": 201,\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"PageName\",\n" +
                "      \"isPublished\": false\n" +
                "    },\n" +
                "    \"links\": {},\n" +
                "    \"rows\": [\n" +
                "      {\n" +
                "        \"entity\": \"row\",\n" +
                "        \"columns\": [\n" +
                "          {\n" +
                "            \"entity\": \"column\",\n" +
                "            \"widgets\": [\n" +
                "              {\n" +
                "                \"entity\": \"widget\",\n" +
                "                \"type\": \"Image\",\n" +
                "                \"url\": \"https://images.example.com/image.png\"\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    public void getPageWithUnknownPageType() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("UnknownType")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("PageName")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"errorCode\": \"UNSUPPORTED_PAGE_CONTENT\",\n" +
                "  \"message\": \"Page contains unsupported content\"\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    public void getPageWithUnknownWidget() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("PageName")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .beginWidget("CONTENT", "PRODUCT_CONTENT")
                .beginWidget("ROWS", "UNKNOWN_ROW")
                .endWidget()
                .endWidget()
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        request.setParameter("revision-id", REVISION1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"errorCode\": \"UNSUPPORTED_PAGE_CONTENT\",\n" +
                "  \"message\": \"Page contains unsupported content\"\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    @Test
    @Issue("MBO-15059")
    public void getPageWithoutRevisionIdParameter() throws ServletException, IOException, JsonValidationException {
        Page innerPage = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE1)
                .revisionId(REVISION1)
                .pageName("page name")
                .latestRevisionId(REVISION1)
                .latestRevisionDate(INSTANT1)
                .beginRootWidget("PRODUCT_CONTEXT")
                .endWidget()
                .build();
        addPageToCmsService(PAGE1, REVISION1, innerPage);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("user-id", USER_ID.toString());
        request.setParameter("page-id", PAGE1.toString());
        servlet.doGet(request, response);

        String expectedJson = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"isReactPage\": true,\n" +
                "  \"pageInfo\": {\n" +
                "    \"pageId\": 101,\n" +
                "    \"pageType\": \"product\",\n" +
                "    \"pageName\": \"page name\",\n" +
                "    \"latestRevision\": {\n" +
                "      \"revisionId\": 201,\n" +
                "      \"revisionDate\": 1000\n" +
                "    },\n" +
                "    \"publishedRevision\": null,\n" +
                "    \"desktopMarketUrl\": null,\n" +
                "    \"touchMarketUrl\": null,\n" +
                "    \"desktopPreviewUrl\": null,\n" +
                "    \"touchPreviewUrl\": null,\n" +
                "    \"brandId\": null,\n" +
                "    \"productId\": null\n" +
                "  },\n" +
                "  \"page\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageId\": 101,\n" +
                "      \"revisionId\": 201,\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"page name\",\n" +
                "      \"isPublished\": false\n" +
                "    },\n" +
                "    \"links\": {},\n" +
                "    \"rows\": []\n" +
                "  }\n" +
                "}";

        Assert.assertEquals(expectedJson, response.getContentAsString());
        validator.validate(expectedJson);
    }

    private void addPageToCmsService(Long pageId, Long revisionId, Page page) {
        if (page != null) {
            cmsServiceDao.addNtPromoRow(page);
        }
        // мокаем findPromoPagesById так как его сложно реализовать через CmsServiceDao
        Mockito.doReturn(page).when(cmsService).findPromoPagesById(pageId, revisionId);
    }
}
