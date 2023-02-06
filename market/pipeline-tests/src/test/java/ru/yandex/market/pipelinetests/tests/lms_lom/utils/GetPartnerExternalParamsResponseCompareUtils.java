package ru.yandex.market.pipelinetests.tests.lms_lom.utils;

import java.util.Comparator;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;

@UtilityClass
@ParametersAreNonnullByDefault
public class GetPartnerExternalParamsResponseCompareUtils {

    public void comparePartnerExternalParamGroups(
        SoftAssertions softly,
        List<PartnerExternalParamGroup> expected,
        List<PartnerExternalParamGroup> actual
    ) {
        softly.assertThat(expected)
            .as("Не совпали количества найденных параметров партнёров")
            .hasSameSizeAs(actual);
        expected.sort(Comparator.comparingLong(PartnerExternalParamGroup::getPartnerId));
        actual.sort(Comparator.comparingLong(PartnerExternalParamGroup::getPartnerId));

        for (int i = 0; i < expected.size(); i++) {
            PartnerExternalParamGroup expectedGroup = expected.get(i);
            PartnerExternalParamGroup actualGroup = actual.get(i);

            softly.assertThat(actualGroup.getPartnerId())
                .isEqualTo(expectedGroup.getPartnerId());
            comparePartnerExternalParams(
                softly,
                expectedGroup.getPartnerExternalParams(),
                actualGroup.getPartnerExternalParams()
            );
        }
    }

    public void comparePartnerExternalParams(
        SoftAssertions softly,
        List<PartnerExternalParam> expected,
        List<PartnerExternalParam> actual
    ) {
        softly.assertThat(expected)
            .as("Не совпали количества найденных параметров партнеров")
            .hasSameSizeAs(actual);
        expected.sort(Comparator.comparing(PartnerExternalParam::getKey));
        actual.sort(Comparator.comparing(PartnerExternalParam::getKey));

        for (int i = 0; i < expected.size(); i++) {
            softly.assertThat(actual.get(i))
                .usingRecursiveComparison()
                .comparingOnlyFields(
                    "key",
                    "value"
                )
                .isEqualTo(expected.get(i));
        }
    }
}
