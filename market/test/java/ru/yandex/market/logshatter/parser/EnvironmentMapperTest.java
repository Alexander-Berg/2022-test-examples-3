package ru.yandex.market.logshatter.parser;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.trace.Environment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("MethodName")
public class EnvironmentMapperTest {
    private static String keyPrefix = EnvironmentMapper.LOGBROKER_PROTOCOL_PREFIX;
    private Map<String, String> params = new HashMap<>();
    private Map<String, String> originToEnvMap = new HashMap<>();
    private EnvironmentMapper environmentMapper;

    @BeforeEach
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

    @AfterEach
    public void tearDown() {
        params.clear();
        originToEnvMap.clear();
    }

    @Test
    public void testGetEnvironment_throwIAEOnNullKeyPrefix() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnvironmentMapper(null));
    }

    @Test
    public void testGetEnvironment_returnUNKNOWNIfEnvNotFound() {
        ParserContext fakeParserContext = mock(ParserContext.class);
        when(fakeParserContext.getParams()).thenReturn(params);
        when(fakeParserContext.getOrigin()).thenReturn("some-random-env");

        Assertions.assertEquals(Environment.UNKNOWN, environmentMapper.getEnvironment(fakeParserContext));
    }

    @Test
    public void testGetEnvironment_returnUNKNOWNOnEmptyEnvs() {
        ParserContext fakeParserContext = mock(ParserContext.class);
        when(fakeParserContext.getParams()).thenReturn(new HashMap<>());
        when(fakeParserContext.getOrigin()).thenReturn("some-random-env");

        Assertions.assertEquals(Environment.UNKNOWN, environmentMapper.getEnvironment(fakeParserContext));
    }

    @Test
    public void testGetEnvironment_returnProperEnvOnOrigin() {
        ParserContext fakeParserContext = mock(ParserContext.class);
        when(fakeParserContext.getParams()).thenReturn(params);

        for (Map.Entry<String, String> entry : originToEnvMap.entrySet()) {
            when(fakeParserContext.getOrigin()).thenReturn(entry.getKey());
            Environment environment = environmentMapper.getEnvironment(fakeParserContext);
            Assertions.assertEquals(entry.getValue(), environment.toString());
        }
    }
}
