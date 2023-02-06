package ru.yandex.market.mbo.mdm.common.utils;

import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;

import ru.yandex.market.mbo.http.ModelStorage;

public class TestMboModelUtils {
    private TestMboModelUtils() {
    }

    public static ModelStorage.ParameterValue clearModificationDate(ModelStorage.ParameterValue parameterValue) {
        return parameterValue.toBuilder()
            .clearModificationDate()
            .build();
    }

    public static ModelStorage.Model clearValuesModificationDate(ModelStorage.Model model) {
        return model.toBuilder()
            .clearParameterValues()
            .addAllParameterValues(model.getParameterValuesList().stream()
                .map(TestMboModelUtils::clearModificationDate)
                .collect(Collectors.toList()))
            .build();
    }

    public static void assertEqualsIgnoringModificationDate(ModelStorage.ParameterValue actual,
                                                            ModelStorage.ParameterValue expected) {
        Assertions.assertThat(clearModificationDate(actual))
            .isEqualTo(clearModificationDate(expected));
    }

    public static void assertEqualsIgnoringValuesModificationDate(ModelStorage.Model actual,
                                                                  ModelStorage.Model expected) {
        Assertions.assertThat(clearValuesModificationDate(actual))
            .isEqualTo(clearValuesModificationDate(expected));
    }

    public static void assertNotEqualsIgnoringModificationDate(ModelStorage.ParameterValue actual,
                                                               ModelStorage.ParameterValue expected) {
        Assertions.assertThat(clearModificationDate(actual))
            .isNotEqualTo(clearModificationDate(expected));
    }

    public static void assertNotEqualsIgnoringValuesModificationDate(ModelStorage.Model actual,
                                                                     ModelStorage.Model expected) {
        Assertions.assertThat(clearValuesModificationDate(actual))
            .isNotEqualTo(clearValuesModificationDate(expected));
    }
}
