package ru.yandex.market.jmf.entities.initialization.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.jmf.entities.initialization.JsonEntityInitializationProvider;
import ru.yandex.market.jmf.metadata.Fqn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DifferentEnvironmentsTest {
    public static final String RESOURCE_NAME = "testEnv.json";
    private static final String JSON_CONTENT = """
            {
              "items": [
                {
                  "key": 1,
                  "commonAttr": "test common",
                  "envAttr": "test default"
                }
              ],
              "env": {
                "production": [
                  {
                    "key": 1,
                    "envAttr": "test prod"
                  }
                ],
                "testing": [
                  {
                    "key": 1,
                    "envAttr": "test test"
                  }
                ],
                "development": [
                  {
                    "key": 1,
                    "envAttr": "test dev"
                  }
                ]
              }
            }
            """;

    @ParameterizedTest
    @CsvSource({
            "DEVELOPMENT",
            "LOCAL",
            "TESTING",
            "TEST2",
            "PRODUCTION",
            "INTEGRATION_TEST"
    })
    public void test(Environment environment) throws IOException {
        EnvironmentResolver environmentResolver = mock(EnvironmentResolver.class);
        when(environmentResolver.get()).thenReturn(environment);

        Resource resource = mock(Resource.class);
        when(resource.getInputStream())
                .thenReturn(new ByteArrayInputStream(JSON_CONTENT.getBytes(StandardCharsets.UTF_8)));

        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource(RESOURCE_NAME)).thenReturn(resource);

        JsonEntityInitializationProvider provider = new JsonEntityInitializationProvider(
                Fqn.of("testEnv"), "key", RESOURCE_NAME, resourceLoader,
                new ObjectMapper(), environmentResolver
        );

        assertThat(provider.items())
                .toIterable()
                .containsExactly(Map.of(
                        "key", 1,
                        "commonAttr", "test common",
                        "envAttr", switch (environment) {
                            case PRODUCTION -> "test prod";
                            case TESTING, TEST2 -> "test test";
                            case DEVELOPMENT, LOCAL -> "test dev";
                            default -> "test default";
                        }
                ));
    }
}
