package ru.yandex.market.mbo.cms.api.servlets.editor;

import java.io.IOException;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.mbo.cms.core.helpers.PermissionCheckHelper;
import ru.yandex.market.mbo.cms.core.models.Page;
import ru.yandex.market.mbo.cms.core.models.PushPriority;
import ru.yandex.market.mbo.cms.core.models.permission.ActionType;
import ru.yandex.market.mbo.cms.core.service.CmsService;
import ru.yandex.market.mbo.common.validation.json.JsonSchemaValidator;
import ru.yandex.market.mbo.common.validation.json.JsonValidationException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;

/**
 * @author ayratgdl
 * @date 22.03.18
 */
@Issue("MBO-14940")
public class CmsApiPublicationPageServletTest {
    private static final String RESPONSE_SCHEMA_PATH =
        "/mbo-cms-api/schemas/editor/publication-page-response.schema.json";
    private static final long WAIT_INVOKE_UPDATE_STAND_IN_THREAD = 1000;
    private static final Long USER_ID = 101L;
    private static final Long PAGE_ID = 201L;
    private static final Long REVISION_ID = 301L;

    private CmsApiPublicationPageServlet servlet;
    private PermissionCheckHelper permissionCheckHelper;
    private CmsService cmsService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private JsonSchemaValidator validator;

    @Before
    public void setUp() {
        cmsService = Mockito.mock(CmsService.class);
        servlet = new CmsApiPublicationPageServlet(cmsService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        validator = JsonSchemaValidator.createFromClasspath(RESPONSE_SCHEMA_PATH);
        permissionCheckHelper = Mockito.mock(PermissionCheckHelper.class);
        servlet.setPermissionCheckHelper(permissionCheckHelper);
    }

    @Test
    public void absentParameters() throws IOException, JsonValidationException {
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
            "  \"status\": \"ERROR\",\n" +
            "  \"errorCode\": \"BAD_PARAMETERS_REQUEST\",\n" +
            "  \"message\": \"Absent required request parameter 'user-id'.\"\n" +
            "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void wrongValueOfPublishParameter() throws IOException, JsonValidationException {
        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID.toString());
        request.addParameter("publish", "unknown");
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
            "  \"status\": \"ERROR\",\n" +
            "  \"errorCode\": \"BAD_PARAMETERS_REQUEST\",\n" +
            "  \"message\": \"Request parameter 'publish' should be boolean. Actual value = 'unknown'.\"\n" +
            "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void pageHasNotFound() throws IOException, JsonValidationException {
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong())).thenThrow(IllegalStateException.class);
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong(), Mockito.anyLong()))
            .thenThrow(IllegalStateException.class);

        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID.toString());
        request.addParameter("publish", "true");
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
            "  \"status\": \"ERROR\",\n" +
            "  \"errorCode\": \"NOT_FOUND\",\n" +
            "  \"message\": \"Page " + PAGE_ID + " isn't found.\"\n" +
            "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void revisionHasNotFound() throws IOException, JsonValidationException {
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong())).thenThrow(IllegalStateException.class);
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong(), Mockito.anyLong()))
            .thenThrow(IllegalStateException.class);

        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID.toString());
        request.addParameter("rev-id", REVISION_ID.toString());
        request.addParameter("publish", "true");
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
            "  \"status\": \"ERROR\",\n" +
            "  \"errorCode\": \"NOT_FOUND\",\n" +
            "  \"message\": \"Page " + PAGE_ID + " revision " + REVISION_ID + " isn't found.\"\n" +
            "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void publishPage() throws IOException, JsonValidationException {
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong())).thenReturn(new Page());
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(new Page());
        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID.toString());
        request.addParameter("publish", "true");
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
            "  \"status\": \"SUCCESS\"\n" +
            "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
        Mockito.verify(cmsService).publishCmsPage(Mockito.any(Page.class), eq(USER_ID), Mockito.any());
    }

    @Test
    public void publishRevision() throws IOException, JsonValidationException {
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong())).thenReturn(new Page());
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong(), Mockito.anyLong()))
            .thenReturn(new Page());
        doNothing()
            .when(permissionCheckHelper)
            .checkPermissionsForDocument(
                Mockito.anyString(), Mockito.anyLong(), Mockito.any(Page.class), eq(ActionType.PUBLISH));
        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID.toString());
        request.addParameter("rev-id", REVISION_ID.toString());
        request.addParameter("publish", "true");
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
            "  \"status\": \"SUCCESS\"\n" +
            "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
        Mockito.verify(cmsService).publishCmsPage(Mockito.any(Page.class), eq(USER_ID), Mockito.any());
    }

    @Test
    @Issue("MBO-15059")
    public void unpublishPage() throws IOException, JsonValidationException, InterruptedException {
        Mockito.when(cmsService.findPromoPagesById(Mockito.anyLong())).thenReturn(new Page());
        request.addParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", PAGE_ID.toString());
        request.addParameter("publish", "false");
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
            "  \"status\": \"SUCCESS\"\n" +
            "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());

        Thread.sleep(WAIT_INVOKE_UPDATE_STAND_IN_THREAD);
        Mockito.verify(cmsService).unpublishCmsPage(PAGE_ID, USER_ID, PushPriority.USER);
    }
}
