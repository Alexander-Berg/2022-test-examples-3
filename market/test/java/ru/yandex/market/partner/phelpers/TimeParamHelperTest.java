package ru.yandex.market.partner.phelpers;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.SimpleErrorInfo;
import ru.yandex.common.util.parameters.ParametersSource;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author wadim
 */
public class TimeParamHelperTest {
    private TimeParamHelper timeParamHelper;

    @Before
    public void setUp() throws Exception {
        timeParamHelper = new TimeParamHelper();
    }

    @Test
    public void testGetValueNumberFormatException() throws Exception {
        testGetValueException("incorrect:format");
    }

    @Test
    public void testGetValueIndexOutOfBoundsException() throws Exception {
        testGetValueException("incorrect format");
    }

    @Test
    public void testGetValue() throws Exception {
        String time = "10:20";
        String paramName = "name";
        MockServResponse response = new MockServResponse();

        ParametersSource parametersSource = mock(ParametersSource.class);
        when(parametersSource.getParam(paramName, true)).thenReturn(time);

        int[] value = timeParamHelper.getValue(parametersSource, response, paramName);

        assertArrayEquals(value, new int[]{10, 20});
        assertTrue(response.getErrors().isEmpty());
    }

    private void testGetValueException(String time) throws Exception {
        String paramName = "name";
        MockServResponse response = new MockServResponse();

        ParametersSource parametersSource = mock(ParametersSource.class);
        when(parametersSource.getParam(paramName, true)).thenReturn(time);

        int[] value = timeParamHelper.getValue(parametersSource, response, paramName);

        assertArrayEquals(value, new int[]{0, 0});
        assertEquals(response.getErrors().size(), 1);
        assertTrue(((SimpleErrorInfo) response.getErrors().get(0)).getMessageCode().startsWith("wrong-"));
    }
}
