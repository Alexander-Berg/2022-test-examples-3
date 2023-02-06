package ru.yandex.market.pipelinetests.tests.lms_lom.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Assertions;

import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;

@UtilityClass
@ParametersAreNonnullByDefault
public class PartnerRelationCompareUtil {
    public void comparePartnerRelationWithCutoffs(
        SoftAssertions softly,
        List<PartnerRelationEntityDto> expected,
        List<PartnerRelationEntityDto> actual
    ) {
        Assertions.assertEquals(expected.size(), actual.size(), "Не совпало количество связок");

        for (int i = 0; i < actual.size(); i++) {
            softly.assertThat(actual.get(i))
                .usingRecursiveComparison()
                .comparingOnlyFields(
                    "fromPartnerId",
                    "toPartnerId"
                )
                .as("Не совпали поля связок партнеров")
                .isEqualTo(expected.get(i));

            Set<CutoffResponse> expectedCutoffs = expected.get(i).getCutoffs();
            Set<CutoffResponse> actualCutoffs = actual.get(i).getCutoffs();

            compareCutoffs(softly, sortAndCollectCutoffs(expectedCutoffs), sortAndCollectCutoffs(actualCutoffs));
        }
    }

    public void comparePartnerRelationWithReturnPartners(
        SoftAssertions softly,
        List<PartnerRelationEntityDto> expected,
        List<PartnerRelationEntityDto> actual
    ) {
        Assertions.assertEquals(expected.size(), actual.size(), "Не совпало количество найденных связок");

        expected.sort(Comparator.comparing(PartnerRelationEntityDto::getToPartnerId));
        actual.sort(Comparator.comparing(PartnerRelationEntityDto::getToPartnerId));

        for (int i = 0; i < expected.size(); i++) {
            softly.assertThat(actual.get(i))
                .usingRecursiveComparison()
                .comparingOnlyFields(
                    "fromPartnerId",
                    "toPartnerId",
                    "returnPartner"
                )
                .as("Не совпали поля связок партнеров")
                .isEqualTo(expected.get(i));
        }
    }

    @Nonnull
    private List<CutoffResponse> sortAndCollectCutoffs(Set<CutoffResponse> cutoffSet) {
        return cutoffSet.stream()
            .sorted(Comparator.comparing(CutoffResponse::getCutoffTime).thenComparing(CutoffResponse::getLocationId))
            .collect(Collectors.toList());
    }

    private void compareCutoffs(SoftAssertions softly, List<CutoffResponse> expected, List<CutoffResponse> actual) {
        Assertions.assertEquals(expected.size(), actual.size(), "Не совпало количество катоффов");
        for (int i = 0; i < actual.size(); i++) {
            softly.assertThat(actual.get(i))
                .usingRecursiveComparison()
                .comparingOnlyFields(
                    "locationId",
                    "cutoffTime"
                )
                .as("Не совпали катоффы у связки партнеров")
                .isEqualTo(expected.get(i));
        }
    }
}
