package ru.yandex.market.logistics.lom.lms.converter;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.lms.model.CutoffLightModel;
import ru.yandex.market.logistics.lom.lms.model.PartnerRelationLightModel;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelation;
import ru.yandex.market.logistics.lom.service.yt.dto.YtPartnerRelationTo;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;

@DisplayName("Конвертация связок партнеров")
class PartnerRelationYtToLmsConverterTest extends AbstractTest {

    private static final Long FROM_PARTNER_ID = 1L;

    private final PartnerRelationYtToLmsConverter converter = new PartnerRelationYtToLmsConverter(objectMapper);

    @Test
    @DisplayName("Конвертация связки с катоффами")
    void convertRelationWithCutoffs() {
        YtPartnerRelation relation = new YtPartnerRelation()
            .setPartnerFrom(FROM_PARTNER_ID)
            .setPartnerTo(2L)
            .setCutoffs(Optional.of(
                    "{\"cutoffs\":["
                    + "{\"cutoff_time\":\"12:34:56.000000\",\"location_id\":201},"
                    + "{\"cutoff_time\":\"01:02:03.000000\",\"location_id\":202}"
                    + "]}"
                )
            );

        softly.assertThat(converter.convert(relation))
            .usingRecursiveComparison()
            .isEqualTo(buildLightModel(2L, null, buildCutoffs()));
    }

    @Test
    @DisplayName("Конвертация связки с null-катоффами")
    void convertRelationWithNullCutoffs() {
        YtPartnerRelation relation = new YtPartnerRelation()
            .setPartnerFrom(FROM_PARTNER_ID)
            .setPartnerTo(2L)
            .setCutoffs(Optional.empty());

        softly.assertThat(converter.convert(relation))
            .usingRecursiveComparison()
            .isEqualTo(buildLightModel(2L, null, Collections.emptySet()));
    }

    @Test
    @DisplayName("Конвертация связки с пустыми катоффами")
    void convertRelationWithEmptyCutoffs() {
        YtPartnerRelation relation = new YtPartnerRelation()
            .setPartnerFrom(FROM_PARTNER_ID)
            .setPartnerTo(2L)
            .setCutoffs(Optional.of("{\"cutoffs\":[]}"));

        softly.assertThat(converter.convert(relation))
            .usingRecursiveComparison()
            .isEqualTo(buildLightModel(2L, null, Collections.emptySet()));
    }

    @Test
    @DisplayName("Конвертация связок с возвратными партнерами")
    void convertRelationWithReturnPatrners() {
        YtPartnerRelationTo relationTo = new YtPartnerRelationTo()
            .setPartnerFrom(FROM_PARTNER_ID)
            .setPartnersTo(
                "{\"relations\":["
                + "{\"partner_to\":2,\"return_partner\":2},"
                + "{\"partner_to\":3,\"return_partner\":1}"
                + "]}"
            );

        softly.assertThat(converter.convert(relationTo))
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(List.of(
                buildLightModel(2L, 2L, null),
                buildLightModel(3L, 1L, null)
            ));
    }

    @Test
    @DisplayName("Конвертация пустого списка связок")
    void convertEmptyRelations() {
        YtPartnerRelationTo relationTo = new YtPartnerRelationTo()
            .setPartnerFrom(FROM_PARTNER_ID)
            .setPartnersTo(
                "{\"relations\":[]}"
            );

        softly.assertThat(converter.convert(relationTo))
            .isEqualTo(Collections.emptyList());
    }

    @Nonnull
    private PartnerRelationLightModel buildLightModel(
        Long toPartner,
        Long returnPartner,
        Set<CutoffResponse> cutoffs
    ) {
        return PartnerRelationLightModel.build(
            PartnerRelationEntityDto.newBuilder()
                .fromPartnerId(FROM_PARTNER_ID)
                .toPartnerId(toPartner)
                .returnPartnerId(returnPartner)
                .cutoffs(cutoffs)
                .build()
        );
    }

    @Nonnull
    private Set<CutoffResponse> buildCutoffs() {
        return Set.of(
            CutoffLightModel.build(
                CutoffResponse.newBuilder()
                    .locationId(201)
                    .cutoffTime(LocalTime.of(12, 34, 56))
                    .build()
            ),
            CutoffLightModel.build(
                CutoffResponse.newBuilder()
                    .locationId(202)
                    .cutoffTime(LocalTime.of(1, 2, 3))
                    .build()
            )
        );
    }
}
