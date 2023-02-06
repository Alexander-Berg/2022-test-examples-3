package ru.yandex.market.delivery.partnerapimock.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.partnerapimock.steps.MockSteps;
import ru.yandex.market.delivery.partnerapimock.util.FileUtils;

import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_SECOND_MOCK_RESPONSE_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CHECK_XML_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CREATE_SECOND_MOCK_REQUEST_FILE_PATH;
import static ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil.CREATE_XML_MOCK_REQUEST_FILE_PATH;


/**
 * Тест проверяет выбор мока, когда подходит несколько вариантов
 * Тест не проверяет логику работы с флагом disposable
 * Выбирается самый новый мок.
 */
class ChooseMockStrategyTest extends AbstractIntegrationTest {

    private String createMockRequest;
    private String createSecondMockRequest;
    private String checkMockRequest;
    private String checkSecondMockResponse;

    @Autowired
    private MockSteps mockSteps;

    @BeforeEach
    void setup() throws Exception {
        createMockRequest = FileUtils.readFile(CREATE_XML_MOCK_REQUEST_FILE_PATH);
        createSecondMockRequest = FileUtils.readFile(CREATE_SECOND_MOCK_REQUEST_FILE_PATH);
        checkMockRequest = FileUtils.readFile(CHECK_XML_MOCK_REQUEST_FILE_PATH);
        checkSecondMockResponse = FileUtils.readFile(CHECK_SECOND_MOCK_RESPONSE_FILE_PATH);
    }

    @Test
    void testNewestMockSelected() throws Exception {
        mockSteps.createMock(createMockRequest);
        mockSteps.createMock(createSecondMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkSecondMockResponse);
    }
}
