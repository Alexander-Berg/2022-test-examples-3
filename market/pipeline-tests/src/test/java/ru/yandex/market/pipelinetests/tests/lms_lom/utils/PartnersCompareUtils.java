package ru.yandex.market.pipelinetests.tests.lms_lom.utils;

import java.util.Comparator;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;
import org.assertj.core.api.SoftAssertions;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;

@UtilityClass
@ParametersAreNonnullByDefault
public class PartnersCompareUtils {

    public void comparePartnerLists(
        SoftAssertions softly,
        List<PartnerResponse> expected,
        List<PartnerResponse> actual
    ) {
        softly.assertThat(expected)
            .as("Не совпали количества найденных партнеров")
            .hasSameSizeAs(actual);

        expected.sort(Comparator.comparingLong(PartnerResponse::getId));
        actual.sort(Comparator.comparingLong(PartnerResponse::getId));

        for (int i = 0; i < expected.size(); ++i) {
            comparePartners(softly, expected.get(i), actual.get(i));
        }
    }

    public void comparePartners(
        SoftAssertions softly,
        PartnerResponse expected,
        PartnerResponse actual
    ) {
        softly.assertThat(actual)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .comparingOnlyFields(
                "id",
                "marketId",
                "name",
                "readableName",
                "partnerType",
                "subtype.id",
                "billingClientId",
                "domain"
            )
            .as("Не совпали поля партнеров")
            .isEqualTo(expected);
        GetPartnerExternalParamsResponseCompareUtils.comparePartnerExternalParams(
            softly,
            expected.getParams(),
            actual.getParams()
        );
    }
}
