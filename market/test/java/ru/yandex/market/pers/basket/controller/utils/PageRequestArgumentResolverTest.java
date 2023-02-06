package ru.yandex.market.pers.basket.controller.utils;

import org.junit.Test;
import org.springframework.web.context.request.NativeWebRequest;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.model.ResultLimit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author ifilippov5
 */
public class PageRequestArgumentResolverTest extends PersBasketTest {

    private final PageRequestArgumentResolver resolver = new PageRequestArgumentResolver();

    @Test
    public void test() {
        testCase(null, null, null, null, null);
        testCase("1", null, null, null,
            new ResultLimit(null, null));
        testCase("1", "4", null, null,
            new ResultLimit(0, 4));
        testCase("2", "0", null, null,
            new ResultLimit(0, 0));
        testCase("3", "2", null, null,
            new ResultLimit(4, 2));
        testCase(null, null, "2", "8",
            new ResultLimit(2, 8));
        testCase(null, null, null, "5",
            new ResultLimit(null, 5));
    }

    private void testCase(String page, String pageSize, String offset, String limit,
                          ResultLimit expectedResultLimit) {
        NativeWebRequest request = mock(NativeWebRequest.class);
        when(request.getParameter("page"))
            .thenReturn(page);
        when(request.getParameter("pageSize"))
            .thenReturn(pageSize);
        when(request.getParameter("offset"))
            .thenReturn(offset);
        when(request.getParameter("limit"))
            .thenReturn(limit);
        ResultLimit resultLimit = (ResultLimit) resolver.resolveArgument(null, null,
            request, null);

        if (expectedResultLimit == null) {
            assertNull(resultLimit);
        } else {
            assertEquals(expectedResultLimit.getOffset(), resultLimit.getOffset());
            assertEquals(expectedResultLimit.getLimit(), resultLimit.getLimit());
        }
    }

}
