package ru.yandex.market.cocon.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.cocon.model.Cabinet;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.cocon.model.Feature;
import ru.yandex.market.cocon.model.Page;
import ru.yandex.market.cocon.reader.AuthorityParser;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ValidCabinetConfigTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("configFiles")
    @DisplayName("Валидация парсинга authority конфигов кабинетов")
    void validateConfigs(CabinetType type) {
        Cabinet cabinet = readCabinet(type.getId() + ".json");

        if (cabinet.getFeatures() != null) {
            assertFeaturesNotThrow(cabinet.getFeatures());
        }
        cabinet.getPages().forEach(page -> {
            if (page.getFeatures() != null) {
                assertFeaturesNotThrow(page.getFeatures());
            }
        });
    }

    @ParameterizedTest
    @MethodSource("configFiles")
    @DisplayName("Дубликаты страниц")
    void validateDuplicatePages(CabinetType type) {
        readCabinet(type.getId() + ".json").getPages().stream()
                .map(Page::getName)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                .forEach((name, count) -> assertEquals(1, count, "Duplicate page entry found in config: " + name));
    }

    @ParameterizedTest
    @MethodSource("configFiles")
    @DisplayName("Дубликаты фич")
    void validateDuplicateFeatures(CabinetType type) {
        readCabinet(type.getId() + ".json").getPages()
                .forEach(page -> Stream.ofNullable(page.getFeatures())
                        .flatMap(Collection::stream)
                        .map(Feature::getName)
                        .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                        .forEach((name, count) -> assertEquals(1, count, "Duplicate feature entry found in " +
                                "config: " + name)));
    }

    @ParameterizedTest
    @MethodSource("configFiles")
    @DisplayName("Дубликаты фич кабинета")
    void validateDuplicateCabinetFeatures(CabinetType type) {
        Optional.ofNullable(readCabinet(type.getId() + ".json").getFeatures()).stream()
                .flatMap(Collection::stream)
                .map(Feature::getName)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                .forEach((name, count) -> assertEquals(1, count, "Duplicate feature entry found in config: " + name));
    }

    static Stream<CabinetType> configFiles() {
        return Arrays.stream(CabinetType.values());
    }

    private Cabinet readCabinet(String fileName) {
        InputStream is = getClass().getClassLoader().getResourceAsStream("cabinets/" + fileName);
        try {
            return objectMapper.readerFor(Cabinet.class)
                    .readValue(StringTestUtil.getString(Objects.requireNonNull(is)));
        } catch (IOException e) {
            fail("can't parse cabinet file" + fileName);
        }
        // ignored
        throw new RuntimeException("File not found " + fileName);
    }

    private void assertFeaturesNotThrow(List<Feature> features) {
        features.forEach(feature -> feature.getStates().ifPresent(st -> st.getItems().forEach(
                item -> assertDoesNotThrow(() -> {
                    AuthorityParser.parse(item);
                }))));
    }
}
