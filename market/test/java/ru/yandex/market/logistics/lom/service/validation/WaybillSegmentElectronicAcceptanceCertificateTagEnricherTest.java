package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.PartnerSettings;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillSegmentElectronicAcceptanceCertificateTagEnricher;

@DisplayName("Обогащение для дропшипа-экспресса")
class WaybillSegmentElectronicAcceptanceCertificateTagEnricherTest extends AbstractTest {
    private static final PartnerSettings SETTINGS = PartnerSettings.builder()
        .electronicAcceptanceCertificateRequired(true)
        .build();

    private final FeatureProperties featureProperties = new FeatureProperties();
    private final WaybillSegmentElectronicAcceptanceCertificateTagEnricher enricher =
        new WaybillSegmentElectronicAcceptanceCertificateTagEnricher(featureProperties);

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Обогатить сегменты, если у партнера есть флаг ЭАПП и проперти включена для всех")
    void enrichSuccess(
        @SuppressWarnings("unused") String name,
        boolean enabledForAll,
        Set<Long> listOfEnabledPartners
    ) {
        featureProperties.setAddElectronicReceptionTransferActTag(enabledForAll);
        featureProperties.setPartnerIdsWithRequiredElectronicReceptionTransferAct(listOfEnabledPartners);
        WaybillSegment dropship = getWaybillSegment(PartnerType.DROPSHIP, 0, SETTINGS, 1L);
        WaybillSegment delivery = getWaybillSegment(PartnerType.DELIVERY, 1, null, 2L);
        Order order = Order.builder().waybill(List.of(dropship, delivery)).build();

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());

        softly.assertThat(results.getOrderModifier().apply(order).getWaybill().stream()
            .filter(w -> w.getPartnerType() == PartnerType.DELIVERY || w.getPartnerType() == PartnerType.DROPSHIP)
            .anyMatch(w -> w.hasTag(WaybillSegmentTag.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED))
        )
            .isTrue();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("enrichSuccess")
    @DisplayName(
        "Обогатить сегменты в маршруте DROPSHIP -> MOVEMENT -> PICKUP,"
            + " у партнера есть флаг ЭАПП и проперти включена для всех"
    )
    void enrichSuccessWithMovement(
        @SuppressWarnings("unused") String name,
        boolean enabledForAll,
        Set<Long> listOfEnabledPartners
    ) {
        featureProperties.setAddElectronicReceptionTransferActTag(enabledForAll);
        featureProperties.setPartnerIdsWithRequiredElectronicReceptionTransferAct(listOfEnabledPartners);
        WaybillSegment dropship = getWaybillSegment(PartnerType.DROPSHIP, 0, SETTINGS, 1L);
        WaybillSegment movement = getWaybillSegment(PartnerType.SORTING_CENTER, 1, SETTINGS, 2L)
            .setSegmentType(SegmentType.MOVEMENT);
        WaybillSegment delivery = getWaybillSegment(PartnerType.DELIVERY, 2, null, 3L);
        Order order = Order.builder().waybill(List.of(dropship, movement, delivery)).build();

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());

        softly.assertThat(results.getOrderModifier().apply(order).getWaybill().stream()
            .filter(w -> w.getSegmentType() == SegmentType.MOVEMENT)
            .anyMatch(w -> w.hasTag(WaybillSegmentTag.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED))
        )
            .isTrue();
    }

    @Nonnull
    private static Stream<Arguments> enrichSuccess() {
        return Stream.of(
            Arguments.of("У партнера есть флаг ЭАПП и проперти включена для всех", true, Set.of()),
            Arguments.of("У партнера есть флаг ЭАПП и партнер в списке включенных", false, Set.of(1L)),
            Arguments.of(
                "У партнера есть флаг ЭАПП, проперти включена для всех и партнер в списке включенных",
                true,
                Set.of(1L)
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Ничего не делать")
    void enrichNotEnabled(
        @SuppressWarnings("unused") String name,
        boolean enabledForAll,
        Set<Long> listOfEnabledPartners,
        PartnerSettings partnerSettings
    ) {
        featureProperties.setAddElectronicReceptionTransferActTag(enabledForAll);
        featureProperties.setPartnerIdsWithRequiredElectronicReceptionTransferAct(listOfEnabledPartners);
        WaybillSegment dropship = getWaybillSegment(PartnerType.DROPSHIP, 0, partnerSettings, 1L);
        WaybillSegment delivery = getWaybillSegment(PartnerType.DELIVERY, 1, null, 2L);
        Order order = Order.builder().waybill(List.of(dropship, delivery)).build();
        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());

        softly.assertThat(results.getOrderModifier().apply(order).getWaybill().stream()
            .filter(w -> w.getPartnerType() == PartnerType.DELIVERY)
            .noneMatch(w -> w.hasTag(WaybillSegmentTag.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED))
        )
            .isTrue();
    }

    @Nonnull
    private static Stream<Arguments> enrichNotEnabled() {
        return Stream.of(
            Arguments.of("У партнера нет флага ЭАПП, проперти включена для всех",
                true,
                Set.of(),
                PartnerSettings.builder().build()
            ),
            Arguments.of(
                "У партнера нет флага ЭАПП, партнер в списке в пропертях",
                false,
                Set.of(1L),
                PartnerSettings.builder().build()
            ),
            Arguments.of("У партнера есть флаг ЭАПП, отключение через проперти", false, Set.of(3L), SETTINGS)
        );
    }

    @DisplayName("Сегменты, при которых не нужны изменения")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void enrichIncorrectSegments(
        @SuppressWarnings("unused") String name,
        List<WaybillSegment> segments
    ) {
        featureProperties.setAddElectronicReceptionTransferActTag(true);
        Order order = Order.builder().waybill(segments).build();
        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());

        softly.assertThat(results.getOrderModifier().apply(order).getWaybill().stream()
            .filter(w -> w.getPartnerType() == PartnerType.DELIVERY)
            .noneMatch(w -> w.hasTag(WaybillSegmentTag.ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED))
        )
            .isTrue();
    }

    @Nonnull
    private static Stream<Arguments> enrichIncorrectSegments() {
        return Stream.of(
            Arguments.of("Только первый сегмент", List.of(getWaybillSegment(PartnerType.DROPSHIP, 0, SETTINGS, 1L))),
            Arguments.of("Только второй сегмент", List.of(getWaybillSegment(PartnerType.DELIVERY, 1, null, 2L))),
            Arguments.of(
                "Первый сегмент не дропшип",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, 0, SETTINGS, 1L),
                    getWaybillSegment(PartnerType.DELIVERY, 1, null, 2L)
                )
            )
        );
    }

    @Nonnull
    private static WaybillSegment getWaybillSegment(
        PartnerType partnerType,
        int index,
        PartnerSettings settings,
        Long partnerId
    ) {
        return new WaybillSegment()
            .setPartnerType(partnerType)
            .setWaybillSegmentIndex(index)
            .setPartnerSettings(settings)
            .setPartnerId(partnerId);
    }
}
