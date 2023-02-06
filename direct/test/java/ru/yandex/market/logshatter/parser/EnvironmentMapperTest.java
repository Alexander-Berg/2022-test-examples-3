package ru.yandex.market.logshatter.parser;

import org.junit.Before;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.Assert;
import ru.yandex.market.logshatter.parser.trace.Environment;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentMapperTest {
    private Map<String, String> params = new HashMap<>();
    private Map<String, String> originToEnvMap = new HashMap<>();
    private static String keyPrefix = EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX;
    private EnvironmentMapper environmentMapper;

    @Before
    public void setUp() {
        params.put("logbroker://market-health-prestable", "PRESTABLE");
        params.put("logbroker://market-health-stable", "PRODUCTION");
        params.put("logbroker://market-health-testing", "TESTING");
        params.put("logbroker://market-health-dev", "DEVELOPMENT");

        originToEnvMap.put("market-health-prestable", "PRESTABLE");
        originToEnvMap.put("market-health-stable", "PRODUCTION");
        originToEnvMap.put("market-health-testing", "TESTING");
        originToEnvMap.put("market-health-dev", "DEVELOPMENT");
        environmentMapper = new EnvironmentMapper(keyPrefix);
    }

    @After
    public void tearDown() {
        params.clear();
        originToEnvMap.clear();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetEnvironment_throwIAEOnNullKeyPrefix() {
        new EnvironmentMapper(null);
    }

    @Test
    public void testGetEnvironment_returnUNKNOWNIfEnvNotFound() {
        ParserContext fakeParserContext = mock(ParserContext.class);
        when(fakeParserContext.getParams()).thenReturn(params);
        when(fakeParserContext.getOrigin()).thenReturn("some-random-env");

        Assert.assertEquals(Environment.UNKNOWN, environmentMapper.getEnvironment(fakeParserContext));
    }
    @Test
    public void testGetEnvironment_returnUNKNOWNOnEmptyEnvs() {
        ParserContext fakeParserContext = mock(ParserContext.class);
        when(fakeParserContext.getParams()).thenReturn(new HashMap<>());
        when(fakeParserContext.getOrigin()).thenReturn("some-random-env");

        Assert.assertEquals(Environment.UNKNOWN, environmentMapper.getEnvironment(fakeParserContext));
    }

    @Test
    public void testGetEnvironment_returnProperEnvOnOrigin() {
        ParserContext fakeParserContext = mock(ParserContext.class);
        when(fakeParserContext.getParams()).thenReturn(params);

        for (Map.Entry<String, String> entry : originToEnvMap.entrySet()) {
            when(fakeParserContext.getOrigin()).thenReturn(entry.getKey());
            Environment environment = environmentMapper.getEnvironment(fakeParserContext);
            Assert.assertEquals(entry.getValue(), environment.toString());
        }
    }
}
