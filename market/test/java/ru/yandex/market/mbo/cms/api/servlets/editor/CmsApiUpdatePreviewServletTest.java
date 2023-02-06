package ru.yandex.market.mbo.cms.api.servlets.editor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import ru.yandex.market.mbo.cms.core.models.DocumentExport;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.core.models.ViewTemplatePageBuilder;
import ru.yandex.market.mbo.cms.core.service.CmsService;
import ru.yandex.market.mbo.cms.core.service.CmsServiceDaoMock;
import ru.yandex.market.mbo.cms.core.utils.CmsUtils;
import ru.yandex.market.mbo.common.validation.json.JsonSchemaValidator;
import ru.yandex.market.mbo.common.validation.json.JsonValidationException;

/**
 * @author ayratgdl
 * @date 21.03.18
 */
@Issue("MBO-14940")
public class CmsApiUpdatePreviewServletTest {
    private static final String RESPONSE_SCHEMA_PATH =
            "/mbo-cms-api/schemas/editor/update-preview-response.schema.json";

    private static final Long PAGE_ID_1 = 101L;
    private static final Long REVISION_ID_1 = 201L;
    private static final Long PRODUCT_1 = 301L;
    private static final Long USER_ID = 401L;
    private static final long WAIT_INVOKE_UPDATE_STAND_IN_THREAD = 1000;

    private CmsApiUpdatePreviewServlet servlet;
    private CmsService cmsService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private JsonSchemaValidator validator;
    private CmsSchema schema;

    @Before
    public void setUp() {
        cmsService = Mockito.mock(CmsService.class);

        ClientDescription clientDescriptionDesktop = new ClientDescription();
        clientDescriptionDesktop.setHostPreview("market.content-preview.yandex.ru");
        clientDescriptionDesktop.setIconPreview("a");

        ClientDescription clientDescriptionTouch = new ClientDescription();
        clientDescriptionTouch.setHostPreview("m.market.content-preview.yandex.ru");
        clientDescriptionTouch.setIconPreview("b");


        Map<String, ClientDescription> clientDescriptions = new HashMap<>();
        clientDescriptions.put("market_desktop", clientDescriptionDesktop);
        clientDescriptions.put("market_touch", clientDescriptionTouch);

        schema = new CmsSchema();
        String namespace = "abc";
        schema.setNamespace(namespace);

        Map<String, DocumentDescription> descriptions = new HashMap<>();
        DocumentDescription docDescr = new DocumentDescription(namespace, "product");
        DocumentExport deDesktop = new DocumentExport();
        deDesktop.setClient("market_desktop");
        deDesktop.setView("full");
        deDesktop.setUrlPrefix("/product/{product_id}");
        deDesktop.setIdentityFields(Collections.singletonList("product_id"));
        deDesktop.setView("full");

        DocumentExport deTouch = new DocumentExport();
        deTouch.setClient("market_touch");
        deTouch.setView("full");
        deTouch.setUrlPrefix("/product/{product_id}");
        deTouch.setIdentityFields(Collections.singletonList("product_id"));
        deTouch.setView("full");

        docDescr.setExports(Arrays.asList(deDesktop, deTouch));
        descriptions.put("product", docDescr);

        schema.getDocuments().putAll(descriptions);
        schema.getClients().putAll(clientDescriptions);

        Mockito.when(cmsService.getSchema(Mockito.anyString())).thenReturn(schema);

        servlet = new CmsApiUpdatePreviewServlet();
        servlet.setCmsService(cmsService);
        servlet.setPermissionCheckHelper(Mockito.mock(PermissionCheckHelper.class));

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        validator = JsonSchemaValidator.createFromClasspath(RESPONSE_SCHEMA_PATH);
    }

    @Test
    public void handleRequestWhenPageIsNotExist() throws ServletException, IOException, JsonValidationException {
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong())).thenThrow(IllegalStateException.class);
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong(), Mockito.anyLong()))
                .thenThrow(IllegalStateException.class);

        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", "0");
        servlet.doGet(request, response);

        String expectedResponse = "{\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"errorCode\": \"NOT_FOUND\",\n" +
                "  \"message\": \"Page 0 isn't found.\"\n" +
                "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void handleRequestWhenPageIsNotFullness() throws IOException, ServletException, JsonValidationException {
        Page page = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE_ID_1)
                .revisionId(REVISION_ID_1)
                .pageName("PageName")
                .latestRevisionId(REVISION_ID_1)
                .build();
        page = Mockito.spy(page);

        CmsSchema schemaMock = Mockito.mock(CmsSchema.class);
        ClientDescription clientDescriptionMock = new ClientDescription();

        Mockito.when(cmsService.getSchema(Mockito.anyString())).thenReturn(schemaMock);
        Mockito.when(schemaMock.getClient(Mockito.any())).thenReturn(clientDescriptionMock);

        Mockito.when(cmsService.findPromoPagesById(PAGE_ID_1)).thenReturn(page);

        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID_1.toString());
        servlet.doGet(request, response);

        String expectedResponse = "{\n" +
                                  "  \"status\": \"SUCCESS\"\n" +
                                  "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void successHandleRequest()
            throws IOException, ServletException, InterruptedException, JsonValidationException {

        Page page = buildPage(REVISION_ID_1);

        Mockito.when(cmsService.findPromoPagesById(PAGE_ID_1)).thenReturn(page);

        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID_1.toString());
        servlet.doGet(request, response);
        Thread.sleep(WAIT_INVOKE_UPDATE_STAND_IN_THREAD);

        String expectedResponse = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"desktopPreviewUrl\": \"https://market.content-preview.yandex.ru/product/" + PRODUCT_1 + "\",\n" +
                "  \"touchPreviewUrl\": \"https://m.market.content-preview.yandex.ru/product/" + PRODUCT_1 + "\"\n" +
                "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());

        String query = "page_id=" + PAGE_ID_1 + "&rev=" + REVISION_ID_1 +
                "&relations=1&context=1";
        Mockito.verify(cmsService).updateStand(query);
    }

    @Test
    public void specificVersion()
            throws IOException, ServletException, InterruptedException, JsonValidationException {

        Page page = buildPage(REVISION_ID_1);

        Mockito.when(cmsService.findPromoPagesById(PAGE_ID_1)).thenThrow(IllegalStateException.class);
        Mockito.when(cmsService.findPromoPagesById(PAGE_ID_1, REVISION_ID_1)).thenReturn(page);

        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID_1.toString());
        request.addParameter("rev-id", REVISION_ID_1.toString());
        servlet.doGet(request, response);
        Thread.sleep(WAIT_INVOKE_UPDATE_STAND_IN_THREAD);

        String expectedResponse = "{\n" +
                "  \"status\": \"SUCCESS\",\n" +
                "  \"desktopPreviewUrl\": \"https://market.content-preview.yandex.ru/product/" + PRODUCT_1 + "\",\n" +
                "  \"touchPreviewUrl\": \"https://m.market.content-preview.yandex.ru/product/" + PRODUCT_1 + "\"\n" +
                "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());

        String query = "page_id=" + PAGE_ID_1 + "&rev=" + REVISION_ID_1 +
                "&relations=1&context=1";
        Mockito.verify(cmsService).updateStand(query);
    }

    public Page buildPage(Long revisionId) {
        Page page = new ViewTemplatePageBuilder(schema)
                .pageType("product")
                .pageId(PAGE_ID_1)
                .revisionId(revisionId)
                .latestRevisionId(revisionId)
                .beginRootWidget("PRODUCT_CONTEXT")
                .beginWidget("LINKS", "UNKNOWN")
                .beginWidget("PAGE_LINK", "PAGE_LINK_PRODUCT")
                .parameter("MODEL_ID", PRODUCT_1)
                .endWidget()
                .endWidget()
                .endWidget()
                .build();


        String propsString = new CmsServiceDaoMock.PagePropertiesBuilder()
                .addProperty("product_id", String.valueOf(PRODUCT_1))
                .addProperty("type", "product")
                .build();
        page.setPropertiesObject(CmsUtils.propertiesToObject(propsString));

        page = Mockito.spy(page);

        Map<String, Set<String>> exportParams = new HashMap<>();
        exportParams.put("product_id", new HashSet<>(Arrays.asList(String.valueOf(PRODUCT_1))));
        exportParams.put("type", new HashSet<>(Arrays.asList("product")));
        Mockito.when(page.collectExportedParameters(Mockito.anyBoolean(), Mockito.any(), Mockito.any()))
                .thenReturn(exportParams);

        return page;
    }
}
