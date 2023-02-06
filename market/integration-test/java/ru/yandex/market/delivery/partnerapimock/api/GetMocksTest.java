package ru.yandex.market.delivery.partnerapimock.api;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ru.yandex.market.delivery.partnerapimock.steps.MockSteps;
import ru.yandex.market.delivery.partnerapimock.util.FileUtils;
import ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil;

@TestExecutionListeners(value = {
    DependencyInjectionTestExecutionListener.class,
    DbUnitTestExecutionListener.class
})
class GetMocksTest extends AbstractIntegrationTest {

    private String getMocksListResponse;
    private String getMocksListSortedByCreatedAscResponse;
    private String getOneMockResponse;
    private String getMocksByContextPathForXmlResponse;
    private String getMocksByContextPathForJsonResponse;

    @Autowired
    @Qualifier("mockSteps")
    private MockSteps mockSteps;

    @Autowired
    @Qualifier("mockStepsForJsonContextPath")
    private MockSteps mockStepsForJsonContextPath;

    @BeforeEach
    void setUp() throws Exception {
        getMocksListResponse = FileUtils.readFile(IntegrationTestResourcesUtil.GET_MOCKS_LIST_RESPONSE);
        getMocksListSortedByCreatedAscResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.GET_MOCKS_LIST_SORTED_BY_CREATED_ASC_RESPONSE);
        getOneMockResponse = FileUtils.readFile(IntegrationTestResourcesUtil.GET_ONE_MOCK_RESPONSE);
        getMocksByContextPathForXmlResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.GET_MOCKS_BY_CONTEXT_PATH_FOR_XML_RESPONSE);
        getMocksByContextPathForJsonResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.GET_MOCKS_BY_CONTEXT_PATH_FOR_JSON_RESPONSE);
    }

    @Test
    @DatabaseSetup("classpath:data/setup.xml")
    void testGetMocks() throws Exception {
        mockSteps.verifyGetMocksResponse(getMocksListResponse);
    }

    @Test
    @DatabaseSetup("classpath:data/setup.xml")
    void testGetMocksSortedByCreatedAsc() throws Exception {
        mockSteps.verifyGetMocksSortedByCreatedAscResponse(getMocksListSortedByCreatedAscResponse);
    }

    @Test
    @DatabaseSetup("classpath:data/setup.xml")
    void testGetMocksByContextPathForXml() throws Exception {
        mockSteps.verifyGetMocksByContextPathResponse(getMocksByContextPathForXmlResponse);
    }

    @Test
    @DatabaseSetup("classpath:data/setup.xml")
    void testGetMocksByContextPathForJson() throws Exception {
        mockStepsForJsonContextPath.verifyGetMocksByContextPathResponse(getMocksByContextPathForJsonResponse);
    }

    @Test
    @DatabaseSetup("classpath:data/setup.xml")
    void testGetMockById() throws Exception {
        mockSteps.verifyGetMockByIdResponse(1L, getOneMockResponse);
    }
}
