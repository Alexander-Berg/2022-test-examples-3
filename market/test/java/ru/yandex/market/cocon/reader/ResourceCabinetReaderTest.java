package ru.yandex.market.cocon.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import ru.yandex.common.util.IOUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class ResourceCabinetReaderTest {
    private static final String RESOURCE_PATTERN = "classpath:ru/yandex/market/cocon/reader/%s";
    ResourceLoader loader = new DefaultResourceLoader();
    ObjectMapper mapper = new ObjectMapper();
    ResourceCabinetReader reader = new ResourceCabinetReader(mapper, loader, RESOURCE_PATTERN);

    @ParameterizedTest
    @ValueSource(strings = {"pages/include", "pages/transitive", "features/include", "features/transitive"})
    void success(String name) throws Exception {
        doTest(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"recursive", "pages/duplicate", "features/duplicate"})
    void errors(String name) {
        assertThrows(IllegalArgumentException.class, () -> reader.read(name + ".json"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "merge/features",
            "merge/features.params",
            "merge/pages",
            "merge/page.feature",
            "merge/full"
    })
    void merge(String name) throws Exception {
        doTest(name);
    }

    private void doTest(String name) throws Exception {
        var expected = IOUtils.readInputStream(
                loader.getResource(String.format(RESOURCE_PATTERN, name + ".expected.json"))
                        .getInputStream());

        var cabinet = reader.read(name + ".json");
        var actual = mapper.writeValueAsString(cabinet);

        JSONAssert.assertEquals(expected, actual, true);
    }

}
