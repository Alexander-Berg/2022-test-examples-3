package ru.yandex.market.delivery.partnerapimock.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.delivery.partnerapimock.steps.MockSteps;
import ru.yandex.market.delivery.partnerapimock.util.FileUtils;

import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_JSON_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_JSON_MOCK_RESPONSE_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_XML_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_XML_MOCK_RESPONSE_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CREATE_JSON_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CREATE_XML_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.DELETE_XML_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.ERROR_CREATE_MOCK_REQUEST_FILE_PATH;

class MockApiTest extends AbstractIntegrationTest {

    @Autowired
    @Qualifier("mockSteps")
    private MockSteps mockSteps;

    @Autowired
    @Qualifier("mockStepsForJsonContextPath")
    private MockSteps mockStepsForJsonContextPath;

    @Test
    void testCreatingAndCheckingMockForXml() throws Exception {
        String createMockRequest = FileUtils.readFile(CREATE_XML_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest = FileUtils.readFile(CHECK_XML_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse = FileUtils.readFile(CHECK_XML_MOCK_RESPONSE_FILE_PATH)
            .replaceAll("\\s+", " ").trim();
        String deleteMockRequest = FileUtils.readFile(DELETE_XML_MOCK_REQUEST_FILE_PATH);

        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
        mockSteps.deleteMockForXml(deleteMockRequest);
    }

    @Test
    void testCreatingAndCheckingMockForJson() throws Exception {
        String createMockRequest = FileUtils.readFile(CREATE_JSON_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest = FileUtils.readFile(CHECK_JSON_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse = FileUtils.readFile(CHECK_JSON_MOCK_RESPONSE_FILE_PATH).trim();
        String deleteMockRequest = FileUtils.readFile(CHECK_JSON_MOCK_REQUEST_FILE_PATH);

        mockStepsForJsonContextPath.createMock(createMockRequest);
        mockStepsForJsonContextPath.verifyCheckMockResponseForJson(checkMockRequest, checkMockResponse);
        mockStepsForJsonContextPath.deleteMockForJson(deleteMockRequest);
    }

    @Test
    void testExceptionHandling() throws Exception {
        String createMockRequest = FileUtils.readFile(ERROR_CREATE_MOCK_REQUEST_FILE_PATH);
        mockSteps.verifyCreateMockFailed(createMockRequest);
    }
}
