package ru.yandex.direct.feature;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class FeatureGenerationTest {
    @Test
    public void allFeaturesAreGeneratedAndNotManuallyAddedToEnum() throws IOException {
        var expectedKeys = getEnumSet();
        var actualKeys = readConfigKeys();
        Assertions.assertThat(actualKeys)
                .as("Все фичи сгенерированы, см direct/libs-internal/feature-generator/README.md")
                .containsExactlyInAnyOrderElementsOf(expectedKeys);
    }

    private Set<String> getEnumSet() {
        return Arrays.stream(FeatureName.values())
                .map(v -> v.getName().toLowerCase()).collect(Collectors.toSet());
    }

    private Set<String> readConfigKeys() throws IOException {
        var featuresConf = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("feature.yaml");

        var mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        Map<String, ?> lst = mapper.readValue(featuresConf, Map.class);
        return lst.keySet();
    }
}
