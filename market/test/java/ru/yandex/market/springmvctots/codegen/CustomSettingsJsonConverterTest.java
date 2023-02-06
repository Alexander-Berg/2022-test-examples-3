package ru.yandex.market.springmvctots.codegen;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CustomSettingsJsonConverterTest {

    @Test
    public void parseJson() throws IOException {
        String json = "{\n" +
            "  \"basePackageNames\": [\"ru.yandex.market.ts\"],\n" +
            "  \"convertArgsToObject\": true,\n" +
            "  \"maxArgs\": 2,\n" +
            "  \"maxOptionalArgs\": 1,\n" +
            "  \"extraControllers\" : [" +
            "       \"ru.yandex.market.ts.ExtraController\"," +
            "       \"ru.yandex.market.ts.ExtraController2\"" +
            "]" +
            "}";
        CustomSettings actual = CustomSettingsJsonConverter.convertFromJson(json);

        Assertions.assertThat(actual.basePackageNames).containsExactly("ru.yandex.market.ts");
        Assertions.assertThat(actual.convertArgsToObject).isEqualTo(true);
        Assertions.assertThat(actual.maxArgs).isEqualTo(2);
        Assertions.assertThat(actual.maxOptionalArgs).isEqualTo(1);
        Assertions.assertThat(actual.extraControllers)
            .containsExactly("ru.yandex.market.ts.ExtraController", "ru.yandex.market.ts.ExtraController2");
    }

    @Test
    public void doubleConvertion() throws IOException {
        CustomSettings expected = new CustomSettings();

        String json = CustomSettingsJsonConverter.convertToJson(expected);
        CustomSettings actual = CustomSettingsJsonConverter.convertFromJson(json);

        // invoke get* method to lazy initialize field
        actual.getLoadedDataLibraries();

        Assertions.assertThat(actual)
            .usingRecursiveComparison()
            .isEqualTo(expected);
    }
}
