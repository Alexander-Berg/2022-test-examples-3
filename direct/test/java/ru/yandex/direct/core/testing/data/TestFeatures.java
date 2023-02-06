package ru.yandex.direct.core.testing.data;

import javax.annotation.ParametersAreNonnullByDefault;

import org.thymeleaf.util.StringUtils;

@ParametersAreNonnullByDefault
public class TestFeatures {
    private TestFeatures() {
    }

    public static String newFeatureName(String prefix) {
        return prefix + StringUtils.randomAlphanumeric(10);
    }

}
