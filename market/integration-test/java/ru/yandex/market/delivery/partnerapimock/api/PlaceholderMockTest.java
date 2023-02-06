package ru.yandex.market.delivery.partnerapimock.api;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerMapping;

import ru.yandex.market.delivery.partnerapimock.controller.MockController;
import ru.yandex.market.delivery.partnerapimock.exception.ArrayHandlingException;
import ru.yandex.market.delivery.partnerapimock.steps.MockSteps;
import ru.yandex.market.delivery.partnerapimock.util.FileUtils;
import ru.yandex.market.delivery.partnerapimock.util.IntegrationTestResourcesUtil;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceholderMockTest extends AbstractIntegrationTest {

    @Autowired
    private MockSteps mockSteps;

    @Autowired
    private MockController mockController;

    @Test
    void testMockWithPlaceholders() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_PLACEHOLDER_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testMockWithPlaceholdersWithArrays() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_ARRAYS_PLACEHOLDER_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testMockWithPlaceholdersWithSeveralArrays() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_SEVERAL_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_SEVERAL_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_SEVERAL_ARRAYS_PLACEHOLDER_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testMockWithPlaceholdersWithSeveralArraysAndCData() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_SEVERAL_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(
                IntegrationTestResourcesUtil.CHECK_SEVERAL_ARRAYS_AND_CDATA_PLACEHOLDER_MOCK_REQUEST_FILE_PATH
            );
        String checkMockResponse =
            FileUtils.readFile(
                IntegrationTestResourcesUtil.CHECK_SEVERAL_ARRAYS_AND_CDATA_PLACEHOLDER_MOCK_RESPONSE_FILE_PATH
            );
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testSearchMockWithPlaceholders() throws Exception {
        String firstCreateMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String secondCreateMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String thirdCreateMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_SEVERAL_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_ARRAYS_PLACEHOLDER_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(firstCreateMockRequest);
        mockSteps.createMock(secondCreateMockRequest);
        mockSteps.createMock(thirdCreateMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testMockWithPlaceholdersWithInvalidPlaceholder() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_INVALID_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_INVALID_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_INVALID_PLACEHOLDER_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testMockWithPlaceholdersWithInvalidArrays() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_INVALID_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_INVALID_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        mockSteps.createMock(createMockRequest);

        assertThrows(ArrayHandlingException.class, () -> {
            mockController.checkMock(createHttpServletRequest(
                mockSteps.getContextPath() + "/mock/check", "/**/mock/check"), checkMockRequest);
        });
    }

    @Test
    void testMockWithPlaceholdersWithEmptyArrays() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_EMPTY_ARRAYS_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_EMPTY_ARRAYS_PLACEHOLDER_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testMockWithPlaceholdersPart() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_PLACEHOLDER_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_PLACEHOLDER_MOCK_PART_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_PLACEHOLDER_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testMockWithDuplicatePlaceholders() throws Exception {
        String createMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CREATE_DUPLICATE_PLACEHOLDERS_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_DUPLICATE_PLACEHOLDERS_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(IntegrationTestResourcesUtil.CHECK_DUPLICATE_PLACEHOLDERS_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    @Test
    void testMockWithDuplicatePlaceholdersWithArrays() throws Exception {
        String createMockRequest =
            FileUtils.readFile(
                IntegrationTestResourcesUtil.CREATE_DUPLICATE_PLACEHOLDERS_WITH_ARRAYS_MOCK_REQUEST_FILE_PATH);
        String checkMockRequest =
            FileUtils.readFile(
                IntegrationTestResourcesUtil.CHECK_DUPLICATE_PLACEHOLDERS_WITH_ARRAYS_MOCK_REQUEST_FILE_PATH);
        String checkMockResponse =
            FileUtils.readFile(
                IntegrationTestResourcesUtil.CHECK_DUPLICATE_PLACEHOLDERS_WITH_ARRAYS_MOCK_RESPONSE_FILE_PATH);
        mockSteps.createMock(createMockRequest);
        mockSteps.verifyCheckMockResponseForXml(checkMockRequest, checkMockResponse);
    }

    private HttpServletRequest createHttpServletRequest(String fullPath, String pattern) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(eq(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE))).thenReturn(fullPath);
        when(mockRequest.getAttribute(eq(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE))).thenReturn(pattern);
        return mockRequest;
    }
}
