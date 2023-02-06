package ru.yandex.market.pipelinetests.tests.lms_lom.utils;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;

import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;

@UtilityClass
@ParametersAreNonnullByDefault
public class PartnerApiSettingsMethodsCompareUtils {

    public void comparePartnerSettingsMethods(
        SoftAssertions softly,
        List<SettingsMethodDto> expected,
        List<SettingsMethodDto> actual
    ) {
        softly.assertThat(expected)
            .as("Не совпали количества найденных методов партнёров")
            .hasSameSizeAs(actual);

        for (int i = 0; i < expected.size(); i++) {
            softly.assertThat(actual.get(i))
                .usingRecursiveComparison()
                .comparingOnlyFields(
                    "partnerId",
                    "method",
                    "active"
                )
                .isEqualTo(expected.get(i));
        }
    }
}
