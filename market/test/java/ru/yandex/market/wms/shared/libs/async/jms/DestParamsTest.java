package ru.yandex.market.wms.shared.libs.async.jms;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DestParamsTest {

    @Test
    public void getParamReturnsExistingParam() {
        DestParams actualDestParams = new DestParams(Map.of("param1", "value1"));
        Assertions.assertEquals("value1", actualDestParams.getParam("param1"));
    }

    @Test
    public void getParamReturnsReturnsNullWhenParamIsNotExisting() {
        DestParams actualDestParams = new DestParams(Collections.emptyMap());
        Assertions.assertNull(actualDestParams.getParam("param1"));
    }

    @Test
    public void getParamWithParserReturnsExistingParam() {
        DestParams actualDestParams = new DestParams(Map.of(
                "param1", "9999",
                "param2", "true",
                "param3", "99.99"
        ));
        Integer actualIntegerParam = actualDestParams.getParam("param1", Integer::parseInt);
        Boolean actualBooleanParam = actualDestParams.getParam("param2", Boolean::parseBoolean);
        Double actualDoubleParam = actualDestParams.getParam("param3", Double::parseDouble);
        Assertions.assertEquals(9999, actualIntegerParam);
        Assertions.assertTrue(actualBooleanParam);
        Assertions.assertEquals(99.99, actualDoubleParam);
    }

    @Test
    public void getParamWithParserReturnsNullWhenParamIsNotExisting() {
        DestParams actualDestParams = new DestParams(Collections.emptyMap());
        Integer actualParam = actualDestParams.getParam("param1", Integer::parseInt);
        Assertions.assertNull(actualParam);
    }

    @Test
    public void getParamWithParserReturnsNullWhenParserIsInvalid() {
        DestParams actualDestParams = new DestParams(Map.of("param1", "value1"));
        Integer actualParam = actualDestParams.getParam("param1", Integer::parseInt);
        Assertions.assertNull(actualParam);
    }

    @Test
    public void getParamOrDefaultReturnsExistingParam() {
        DestParams actualDestParams = new DestParams(Map.of(
                "param1", "9999"
        ));
        Integer actualIntegerParam = actualDestParams.getParamOrDefault("param1", 1111,
                Integer::parseInt);
        Assertions.assertEquals(9999, actualIntegerParam);
    }

    @Test
    public void getParamOrDefaultReturnsDefaultValueWhenParamIsNotExisting() {
        DestParams actualDestParams = new DestParams(Collections.emptyMap());
        Integer actualIntegerParam = actualDestParams.getParamOrDefault("param1", 1111,
                Integer::parseInt);
        Assertions.assertEquals(1111, actualIntegerParam);
    }
}
