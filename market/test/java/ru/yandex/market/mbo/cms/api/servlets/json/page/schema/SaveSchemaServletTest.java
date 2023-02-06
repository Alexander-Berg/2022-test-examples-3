package ru.yandex.market.mbo.cms.api.servlets.json.page.schema;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ru.yandex.market.mbo.cms.core.dao.CmsServiceDao;
import ru.yandex.market.mbo.cms.core.dao.SchemaDao;
import ru.yandex.market.mbo.cms.core.helpers.PermissionCheckHelper;
import ru.yandex.market.mbo.cms.core.json.service.json_schema.CmsJsonSchemaService;
import ru.yandex.market.mbo.cms.core.json.service.json_schema.CmsJsonSchemaServiceImpl;
import ru.yandex.market.mbo.cms.core.json.utilites.schema.FlowSchemaParser;
import ru.yandex.market.mbo.cms.core.log.MetricsLogger;
import ru.yandex.market.mbo.cms.core.models.ClientDescription;
import ru.yandex.market.mbo.cms.core.models.CmsSchema;
import ru.yandex.market.mbo.cms.core.models.DocumentDescription;
import ru.yandex.market.mbo.cms.core.models.JsonSchemaParsed;
import ru.yandex.market.mbo.cms.core.models.NodeType;
import ru.yandex.market.mbo.cms.core.service.CmsService;
import ru.yandex.market.mbo.cms.core.service.CmsUsersManagerMock;
import ru.yandex.market.mbo.cms.core.service.PermissionCheckHelperMock;
import ru.yandex.market.mbo.cms.core.service.SchemaService;
import ru.yandex.market.mbo.cms.core.service.SchemaServiceImpl;

/**
 * @author chervotkin
 * @date 24.07.18
 */
public class SaveSchemaServletTest {
    private static final int BAD_REQUEST_CODE = 400;
    private static final long FIRST_NEW_REVISION_ID = 201;
    private static final long CHANGESET_ID = 555;
    private static final Long USER_ID = 301L;
    private static final String SERVICE = "whitemarket";
    private static final String PLATFORM = "desktop";
    private static final String BRANCH = "feature/MARKETVERSTKA-100500-schema-test";
    private static final long SCHEMA_ID = 2L;
    private static final long MASTER_SCHEMA_ID = 3L;

    private SaveJsonSchemaServlet servlet;
    private MockHttpServletResponse response;

    private SchemaDao schemaDao;
    private PermissionCheckHelper permissionCheckHelper;
    private SchemaService schemaService;
    private CmsService cmsService;
    private CmsJsonSchemaService cmsJsonSchemaService;
    private FlowSchemaParser flowSchemaParser;

    @Before
    public void setUp() {
        schemaDao = buildMockSchemaDao();

        schemaService = new SchemaServiceImpl(
            schemaDao,
            Mockito.mock(CmsServiceDao.class),
            Mockito.mock(MetricsLogger.class)
        );

        cmsService = Mockito.mock(CmsService.class);
        flowSchemaParser = Mockito.mock(FlowSchemaParser.class);

        permissionCheckHelper = new PermissionCheckHelperMock();

        cmsJsonSchemaService =
                new CmsJsonSchemaServiceImpl(new CmsUsersManagerMock(USER_ID),
                    schemaDao,
                    schemaService,
                    cmsService,
                    flowSchemaParser,
                    permissionCheckHelper
                );

        servlet = new SaveJsonSchemaServlet(cmsJsonSchemaService, Mockito.mock(PermissionCheckHelper.class));

        response = new MockHttpServletResponse();
    }

    @Test
    public void handleEmptyRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", USER_ID.toString());
        request.setParameter("service", SERVICE);
        request.setParameter("platform", PLATFORM);
        request.setParameter("branch", BRANCH);
        request.setContent(new byte[0]);
        servlet.doPost(request, response);

        Assert.assertEquals(BAD_REQUEST_CODE, response.getStatus());
        String expectedResponse = "{\"success\":false,\"errorMessage\":\"Request body is empty\"}";
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void handleRequestWithMissingUserId() throws ServletException, IOException {
        String contentRequest = "{\n" +
                "  \"data\": {\n" +
                "    \"metadata\": {\n" +
                "      \"pageType\": \"product\",\n" +
                "      \"pageName\": \"PageName\"\n" +
                "    },\n" +
                "    \"links\": {},\n" +
                "    \"rows\": []\n" +
                "  }\n" +
                "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("service", SERVICE);
        request.setParameter("platform", PLATFORM);
        request.setParameter("branch", BRANCH);
        request.setContent(contentRequest.getBytes());

        servlet.doPost(request, response);

        String expectedResponse = "{\"errorMessage\":\"User id is not specified\"," +
                "\"errorCode\":\"MISSING_REQUIRED_PARAMETER\"}";
        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void createNewSchemaBadJson() throws IOException {
        String contentRequest = "{}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", USER_ID.toString());
        request.setParameter("service", SERVICE);
        request.setParameter("platform", PLATFORM);
        request.setParameter("branch", BRANCH);
        request.setContent(contentRequest.getBytes());

        servlet.doPost(request, response);
        Assert.assertEquals(BAD_REQUEST_CODE, response.getStatus());
    }

    @Test
    public void createNewSchema() throws IOException {
        String contentRequest = "{\n" +
                "    \"$id\": \"a\"" +
                "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", USER_ID.toString());
        request.setParameter("service", SERVICE);
        request.setParameter("platform", PLATFORM);
        request.setParameter("branch", BRANCH);
        request.setContent(contentRequest.getBytes());

        servlet.doPost(request, response);
        Assert.assertEquals(BAD_REQUEST_CODE, response.getStatus());
//        String expectedResponse = "{\"revisionId\":201}";
//
//        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    @Test
    public void checkSuccessResultFormat() throws IOException {
        Mockito
            .when(schemaService.getSchema(Mockito.anyString(), Mockito.any()))
            .thenAnswer(invocation -> {
                CmsSchema result = new CmsSchema("1");
                result.getNodeTypes().put("nodeType", new NodeType("nodeType"));

                return result;
            });
        Mockito
            .when(flowSchemaParser.parse(Mockito.anyString(), Mockito.any()))
            .thenAnswer(invocation -> {
                JsonSchemaParsed result = new JsonSchemaParsed();
                result.getNodeTypes().put("nodeType", new NodeType("nodeType"));

                return result;
            });
        Mockito
            .when(schemaDao.getNamespace(Mockito.anyString(), Mockito.any()))
            .thenAnswer(new Answer<String>() {
                @Override
                public String answer(InvocationOnMock invocation) throws Throwable {
                    return "1";
                }
            });

        String contentRequest = "{\n" +
                "    \"$id\": \"a\"" +
                "}";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("userId", USER_ID.toString());
        request.setParameter("service", SERVICE);
        request.setParameter("platform", PLATFORM);
        request.setParameter("branch", BRANCH);
        request.setContent(contentRequest.getBytes());

        servlet.doPost(request, response);
        String expectedResponse = "{\"pagesWithChangedNodes\":[],\"rawRevisionId\":201,\"pagesWithRemovedNodes\":[]," +
            "\"success\":true,\"warnings\":[],\"schemaRevisionId\":0,\"nodeTypes\":[{\"name\":\"nodeType\"," +
            "\"namePointer\":\"[nodeType]\"}],\"errors\":[]}";

        Assert.assertEquals(expectedResponse, response.getContentAsString());
    }

    private SchemaDao buildMockSchemaDao() {
        SchemaDao mockSchemaDao = Mockito.mock(SchemaDao.class);
        CmsSchema schema = new CmsSchema();
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

        Mockito
                .when(mockSchemaDao.saveChangeset(Mockito.anyString(), Mockito.any(), Mockito.anyLong()))
                .thenAnswer(new Answer<Long>() {
                    @Override
                    public Long answer(InvocationOnMock invocation) throws Throwable {
                        return CHANGESET_ID;
                    }
                });
        Mockito
            .when(mockSchemaDao.loadSchemaWithRevision(Mockito.anyLong(), Mockito.eq(0L)))
            .thenAnswer(invocation -> new CmsSchema("1"));
        Mockito
            .when(mockSchemaDao.loadSchemaWithRevisionCached(Mockito.anyLong(), Mockito.eq(0L)))
            .thenAnswer(invocation -> new CmsSchema("1"));
        Mockito
            .when(mockSchemaDao.loadSchemaWithRevision(Mockito.anyLong(), Mockito.eq(201L)))
            .thenAnswer(invocation -> {
                CmsSchema result = new CmsSchema("1");
                result.getNodeTypes().put("nodeType", new NodeType("nodeType"));

                return result;
            });
        Mockito
            .when(mockSchemaDao.loadSchemaWithRevisionCached(Mockito.anyLong(), Mockito.eq(201L)))
            .thenAnswer(invocation -> {
                CmsSchema result = new CmsSchema("1");
                result.getNodeTypes().put("nodeType", new NodeType("nodeType"));

                return result;
            });
        Mockito
                .when(mockSchemaDao.saveSchemaRevision(Mockito.anyLong(), Mockito.anyList(),
                        Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any()))
                .thenAnswer(new Answer<Long>() {
                    @Override
                    public Long answer(InvocationOnMock invocation) throws Throwable {
                        return FIRST_NEW_REVISION_ID;
                    }
                });
        Mockito
                .when(mockSchemaDao.getMasterSchema(Mockito.anyString()))
                .thenAnswer(new Answer<CmsSchema>() {
                    @Override
                    public CmsSchema answer(InvocationOnMock invocation) throws Throwable {
                        return schema;
                    }
                });
        Mockito
                .when(mockSchemaDao.saveSchemaObject(Mockito.anyString(), Mockito.any(), Mockito.anyLong()))
                .thenAnswer(new Answer<Long>() {
                    @Override
                    public Long answer(InvocationOnMock invocation) throws Throwable {
                        return FIRST_NEW_REVISION_ID;
                    }
                });

        Mockito
                .when(mockSchemaDao.saveRawJsonSchema(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyLong())).thenAnswer(new Answer<Long>() {
            @Override
            public Long answer(InvocationOnMock invocation) throws Throwable {
                return FIRST_NEW_REVISION_ID;
            }
        });

        return mockSchemaDao;
    }
}
