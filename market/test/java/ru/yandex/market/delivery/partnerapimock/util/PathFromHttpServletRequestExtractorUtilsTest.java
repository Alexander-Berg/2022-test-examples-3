package ru.yandex.market.delivery.partnerapimock.util;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.web.servlet.HandlerMapping;

import ru.yandex.market.delivery.partnerapimock.exception.IncorrectPathPatternException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.partnerapimock.util.PathFromHttpServletRequestExtractorUtils.extractContextPath;

public class PathFromHttpServletRequestExtractorUtilsTest {

    @Test
    public void pathExtractedCorrectlyWhenWildcardInTheBeginning() {
        checkExtractionIsCorrect("/orders/status/push/mock/check", "/**/mock/check", "orders/status/push");
    }

    @Test
    public void pathExtractedCorrectlyWhenWildcardInTheMiddle() {
        checkExtractionIsCorrect("/orders/status/push/mock/check", "/orders/**/mock/check", "status/push");
    }

    @Test
    public void pathExtractedCorrectlyWhenWildcardInTheEnd() {
        checkExtractionIsCorrect("/mock/orders/status/push", "/mock/**", "orders/status/push");
    }

    @Test
    public void pathExtractedCorrectlyWhenPatternIsWholeRequest() {
        checkExtractionIsCorrect("/orders/status/push", "/**", "orders/status/push");
    }

    @Test(expected = IncorrectPathPatternException.class)
    public void exceptionIfThereAreNoWildcardInPattern() {
        checkExtractionIsCorrect("/orders/status/push/mock/check", "/orders/status/push/mock/check", null);
    }

    @Test(expected = IncorrectPathPatternException.class)
    public void exceptionIfPathNotMatchesPattern() {
        checkExtractionIsCorrect("/orders/status/push/mock/check", "/order/**/mock/check", null);
    }

    @Test(expected = IncorrectPathPatternException.class)
    public void exceptionIfPathIsTooShort() {
        checkExtractionIsCorrect("/orders/mock/check", "/orders/status/**/mock/check", null);
    }

    private void checkExtractionIsCorrect(String fullPath, String pattern, String expectedResult) {
        String contextPath = extractContextPath(createHttpServletRequest(fullPath, pattern));
        assertEquals(expectedResult, contextPath);
    }

    private HttpServletRequest createHttpServletRequest(String fullPath, String pattern) {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE)).thenReturn(fullPath);
        when(mockRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).thenReturn(pattern);
        return mockRequest;
    }
}
