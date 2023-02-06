package ru.yandex.market.mbo.cms.api.servlets.editor;

import java.io.IOException;

import javax.servlet.ServletException;

import io.qameta.allure.Issue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.mbo.cms.core.dao.CmsServiceDao;
import ru.yandex.market.mbo.cms.core.helpers.PermissionCheckHelper;
import ru.yandex.market.mbo.cms.core.service.CmsService;
import ru.yandex.market.mbo.common.validation.json.JsonSchemaValidator;
import ru.yandex.market.mbo.common.validation.json.JsonValidationException;

/**
 * @author ayratgdl
 * @date 23.03.18
 */
@Issue("MBO-14940")
public class CmsApiDeleteEditablePageServletTest {
    private static final String RESPONSE_SCHEMA_PATH =
            "/mbo-cms-api/schemas/editor/delete-page-response.schema.json";
    private static final Long USER_ID = 101L;

    private CmsApiDeleteEditablePageServlet servlet;
    private CmsService cmsService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private JsonSchemaValidator validator;

    @Before
    public void setUp() {
        cmsService = Mockito.mock(CmsService.class);
        servlet = new CmsApiDeleteEditablePageServlet();
        servlet.setCmsService(cmsService);
        servlet.setPermissionCheckHelper(Mockito.mock(PermissionCheckHelper.class));
        servlet.setCmsServiceDao(Mockito.mock(CmsServiceDao.class));
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        validator = JsonSchemaValidator.createFromClasspath(RESPONSE_SCHEMA_PATH);
    }

    @Test
    public void absentPageIdParameter() throws ServletException, IOException, JsonValidationException {
        request.setParameter("user-id", USER_ID.toString());
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
                "  \"status\": \"ERROR\",\n" +
                "  \"errorCode\": \"BAD_PARAMETERS_REQUEST\",\n" +
                "  \"message\": \"Absent required request parameter 'page-id'.\"\n" +
                "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void deletePage() throws ServletException, IOException, JsonValidationException {
        request.setParameter("user-id", USER_ID.toString());
        request.addParameter("page-id", "1");
        servlet.doGet(request, response);
        String expectedResponse = "{\n" +
                "  \"status\": \"SUCCESS\"\n" +
                "}";
        validator.validate(expectedResponse);
        Assert.assertEquals(expectedResponse, response.getContentAsString());
        Mockito.verify(cmsService).deleteCmsPage(1L, USER_ID, null);
    }
}
