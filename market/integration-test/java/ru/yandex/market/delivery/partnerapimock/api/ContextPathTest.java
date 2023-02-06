package ru.yandex.market.delivery.partnerapimock.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.delivery.partnerapimock.steps.MockSteps;
import ru.yandex.market.delivery.partnerapimock.util.FileUtils;

import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_XML_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CREATE_XML_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.DELETE_XML_MOCK_REQUEST_FILE_PATH;

class ContextPathTest extends AbstractIntegrationTest {

    private String createMockRequest;
    private String checkMockRequest;
    private String deleteMockRequest;

    @Autowired
    private MockSteps mockSteps;

    @Autowired
    @Qualifier("customContextMockSteps")
    private MockSteps customContextMockSteps;

    @Autowired
    @Qualifier("contextWithSlashesInPathMockSteps")
    private MockSteps contextWithSlashesInPathMockSteps;

    @BeforeEach
    void setup() throws Exception {
        createMockRequest = FileUtils.readFile(CREATE_XML_MOCK_REQUEST_FILE_PATH);
        checkMockRequest = FileUtils.readFile(CHECK_XML_MOCK_REQUEST_FILE_PATH);
        deleteMockRequest = FileUtils.readFile(DELETE_XML_MOCK_REQUEST_FILE_PATH);
    }

    @Test
    void testCheckIncorrectContextPath() throws Exception {
        customContextMockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockFailed(checkMockRequest);
    }

    @Test
    void testDeleteIncorrectContextPath() throws Exception {
        customContextMockSteps.createMock(createMockRequest);
        customContextMockSteps.verifyCheckMockSuccess(checkMockRequest);
        mockSteps.verifyDeleteMockFailed(deleteMockRequest);
    }

    @Test
    void testWorkflowForCustomContextWithSlashes() throws Exception {
        contextWithSlashesInPathMockSteps.createMock(createMockRequest);
        contextWithSlashesInPathMockSteps.verifyCheckMockWhenUrlIsSuffixSuccess(checkMockRequest);
        contextWithSlashesInPathMockSteps.verifyCheckMockSuccess(checkMockRequest);
        contextWithSlashesInPathMockSteps.verifyCheckMockByPutSuccess(checkMockRequest);
        contextWithSlashesInPathMockSteps.deleteMockForXml(deleteMockRequest);
        contextWithSlashesInPathMockSteps.verifyCheckMockFailed(checkMockRequest);
        contextWithSlashesInPathMockSteps.verifyCheckMockByPutFailed(checkMockRequest);
    }

    @Test
    void testDoNothingWorksCorrect() throws Exception {
        mockSteps.verifyDoNothingWorksCorrect();
        customContextMockSteps.verifyDoNothingWorksCorrect();
        contextWithSlashesInPathMockSteps.verifyDoNothingWorksCorrect();
    }
}
