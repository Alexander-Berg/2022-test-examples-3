package ru.yandex.market.api.server.sec;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.integration.UnitTestBase;

import javax.servlet.http.HttpServletRequest;

/**
 * @see <a href="https://st.yandex-team.ru/MARKETAPI-3122">MARKETAPI-3122: подумать про закрытие fields=all в проде</a>
 */
public class FieldsAllHandlerInterceptorFilterTest extends BaseTest {

    FieldsAllHandlerInterceptorFilter filter;

    @Test
    public void hasFieldsAll() {
        HttpServletRequest request = buildRequest("all");
        boolean result = filter.isFiltered(request);

        Assert.assertTrue("Должны проверять кол-во запросов т.к. содержится fields с значением ALL", result);
    }

    @Test
    public void hasFieldsALL() {
        HttpServletRequest request = buildRequest("ALL");
        boolean result = filter.isFiltered(request);

        Assert.assertTrue("Должны проверять кол-во запросов т.к. содержится fields с значением ALL", result);
    }

    @Test
    public void hasFieldsAllAndOther() {
        HttpServletRequest request = buildRequest("a,all,b");
        boolean result = filter.isFiltered(request);

        Assert.assertTrue("Должны проверять кол-во запросов т.к. содержится fields с значением ALL", result);
    }

    @Test
    public void hasFieldsWithoutAll() {
        HttpServletRequest request = buildRequest("other");
        boolean result = filter.isFiltered(request);

        Assert.assertFalse("Не должны проверять кол-во запросов т.к. содержится fields без значения ALL", result);
    }

    @Test
    public void hasntFields() {
        HttpServletRequest request = MockRequestBuilder.start().build();
        boolean result = filter.isFiltered(request);

        Assert.assertFalse("Не должны проверять кол-во запросов т.к. нет параметра fields", result);
    }

    @Before
    public void setUp() {
        filter = new FieldsAllHandlerInterceptorFilter(true);
    }

    private HttpServletRequest buildRequest(String value) {
        return MockRequestBuilder.start().param(Parameters.FIELDS_PARAM_NAME, value).build();
    }
}
