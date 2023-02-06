package ru.yandex.common.util.parameters;

import junit.framework.TestCase;

import static ru.yandex.common.util.parameters.ParametersUtils.filterParams;

/**
 * Date: 15.10.2010
 * Time: 14:25:11
 *
 * @author Dima Schitinin, dimas@yandex-team.ru
 */
public class ParameterUtilsTest extends TestCase {

    private static final ParametersSourceImpl EMPTY_PARAMETERS_SOURCE = new ParametersSourceImpl();
    private static final ParametersSourceImpl A_PARAMETERS_SOURCE = new ParametersSourceImpl();

    static {
        A_PARAMETERS_SOURCE.setParam("a", "a");
    }

    public void testFilterParams() throws Exception {
        final ParametersSourceImpl params = new ParametersSourceImpl();
        params.setParam("a", "a");
        params.setParam("b", "b");
        assertEquals(EMPTY_PARAMETERS_SOURCE, filterParams(params, false, "a", "b", "c"));
        assertEquals(filterParams(params, true, "a", "c"), A_PARAMETERS_SOURCE);
    }

}
