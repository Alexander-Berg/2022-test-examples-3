package ru.yandex.market.delivery.partnerapimock.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.partnerapimock.steps.MockSteps;
import ru.yandex.market.delivery.partnerapimock.util.FileUtils;

import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_DISPOSABLE_MOCK_RESPONSE_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_XML_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_XML_MOCK_RESPONSE_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CREATE_DISPOSABLE_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CREATE_XML_MOCK_REQUEST_FILE_PATH;

class DisposableMockTest extends AbstractIntegrationTest {

    private static final String CREATE_MOCK_RESPONSE =
        "Добавлена новая запись. В базе имеется 1 одноразовых записей, они будут применены в первую очередь";


    private String createMockRequest;
    private String createDisposableMockRequest;
    private String checkMockRequest;
    private String checkMockResponse;
    private String checkDisposableMockResponse;

    @Autowired
    private MockSteps mockSteps;

    @BeforeEach
    void setup() throws Exception {
        createMockRequest = FileUtils.readFile(CREATE_XML_MOCK_REQUEST_FILE_PATH);
        createDisposableMockRequest = FileUtils.readFile(CREATE_DISPOSABLE_MOCK_REQUEST_FILE_PATH);
        checkMockRequest = FileUtils.readFile(CHECK_XML_MOCK_REQUEST_FILE_PATH);
        checkMockResponse = FileUtils.readFile(CHECK_XML_MOCK_RESPONSE_FILE_PATH);
        checkDisposableMockResponse = FileUtils.readFile(CHECK_DISPOSABLE_MOCK_RESPONSE_FILE_PATH);
    }

    @Test
    void testDisposableMockSecondUse() throws Exception {
        mockSteps.createMock(createDisposableMockRequest);
        mockSteps.verifyCheckMockSuccess(checkMockRequest);
        mockSteps.verifyCheckMockFailed(checkMockRequest);
    }

    @Test
    void testCreateDisposableMockAfterPersistentMock() throws Exception {
        mockSteps.createMock(createMockRequest);
        mockSteps.createMock(createDisposableMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkDisposableMockResponse);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testCreatePersistentMockAfterDisposableMock() throws Exception {
        mockSteps.createMock(createDisposableMockRequest);
        mockSteps.createMock(createMockRequest, CREATE_MOCK_RESPONSE);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkDisposableMockResponse);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testCreateTwoDisposableMocks() throws Exception {
        mockSteps.createMock(createDisposableMockRequest);
        mockSteps.createMock(createDisposableMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkDisposableMockResponse);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkDisposableMockResponse);
    }

    @Test
    void testPersistentMock() throws Exception {
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockSuccess(checkMockRequest);
        mockSteps.verifyCheckMockSuccess(checkMockRequest);
    }
}
