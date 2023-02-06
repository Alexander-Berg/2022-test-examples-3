package ru.yandex.market.logistics.lom.service.validation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Streams;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.lms.PartnerExternalParamLmsConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.WaybillSegmentExpressTagEnricher;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.EXPRESS_REQUIREMENTS_COURIER_CAR;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.EXPRESS_REQUIREMENTS_COURIER_PEDESTRIAN;
import static ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType.EXPRESS_REQUIREMENTS_COURIER_PRO;

@DisplayName("Обогащение тэгом EXPRESS")
@ParametersAreNonnullByDefault
class WaybillSegmentExpressTagEnricherTest extends AbstractTest {
    private static final Map<PartnerExternalParamType, WaybillSegmentTag> EXPRESS_REQUIREMENTS_PARAM_WAYBILL_TAG =
        Map.of(
            EXPRESS_REQUIREMENTS_COURIER_CAR, WaybillSegmentTag.COURIER_CAR,
            EXPRESS_REQUIREMENTS_COURIER_PRO, WaybillSegmentTag.COURIER_PRO,
            EXPRESS_REQUIREMENTS_COURIER_PEDESTRIAN, WaybillSegmentTag.COURIER_PEDESTRIAN
        );
    private static final long DELIVERY_PARTNER_ID = 100L;
    private static final long DROPSHIP_ID = 1L;

    private final PartnerExternalParamLmsConverter converter = new PartnerExternalParamLmsConverter();
    private final WaybillSegmentExpressTagEnricher enricher = new WaybillSegmentExpressTagEnricher(converter);

    @DisplayName("Успешное обогащение сегментов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void enrichSuccess(@SuppressWarnings("unused") String name, List<WaybillSegment> segments) {
        segments.get(2).addTag(WaybillSegmentTag.CALL_COURIER);
        Order order = Order.builder().waybill(segments).build();

        ValidateAndEnrichResults results = enricher.validateAndEnrich(
            order,
            new ValidateAndEnrichContext().setPartners(List.of(PartnerResponse.newBuilder().id(1L).build()))
        );
        List<WaybillSegment> resultWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(resultWaybill.get(0).hasTag(WaybillSegmentTag.EXPRESS)).isFalse();
        softly.assertThat(resultWaybill.get(1).hasTag(WaybillSegmentTag.EXPRESS)).isTrue();
        softly.assertThat(resultWaybill.get(2).hasTag(WaybillSegmentTag.EXPRESS)).isTrue();
        if (segments.size() == 4) {
            softly.assertThat(resultWaybill.get(3).hasTag(WaybillSegmentTag.EXPRESS)).isFalse();
        }
    }

    @Nonnull
    private static Stream<Arguments> enrichSuccess() {
        return Stream.of(
            Arguments.of(
                "Есть pickup сегмент",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 1),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 2)
                        .addTag(WaybillSegmentTag.CALL_COURIER),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.PICKUP, 3)
                )
            ),
            Arguments.of(
                "Нет pickup сегмента",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 1),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 2)
                        .addTag(WaybillSegmentTag.CALL_COURIER)
                )
            )
        );
    }

    @DisplayName("Успешное обогащение сегментов с партнерскими требованиями")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({"enrichWithRequirements", "oneRequiredParam"})
    void enrichWithRequirements(
        @SuppressWarnings("unused") String name,
        ValidateAndEnrichContext context,
        Set<WaybillSegmentTag> expected
    ) {
        Order order = Order.builder()
            .waybill(List.of(
                getWaybillSegment(PartnerType.DROPSHIP, SegmentType.FULFILLMENT, 0),
                getWaybillSegment(
                    DELIVERY_PARTNER_ID,
                    PartnerType.DELIVERY,
                    SegmentType.COURIER,
                    2
                ).addTag(WaybillSegmentTag.CALL_COURIER)
            ))
            .build();

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, context);
        List<WaybillSegment> resultWaybill = results.getOrderModifier().apply(order).getWaybill();

        softly.assertThat(resultWaybill.get(0).hasTag(WaybillSegmentTag.EXPRESS)).isTrue();
        softly.assertThat(resultWaybill.get(1).hasTag(WaybillSegmentTag.EXPRESS)).isTrue();
        if (!expected.isEmpty()) {
            softly.assertThat(resultWaybill.get(1).hasTags(expected)).isTrue();
        }
    }

    @Nonnull
    private static Stream<Arguments> enrichWithRequirements() {
        return Stream.of(
            Arguments.of(
                "Есть два параметра в true",
                validateAndEnrichContext(Map.of(
                    EXPRESS_REQUIREMENTS_COURIER_PRO, "true",
                    EXPRESS_REQUIREMENTS_COURIER_CAR, "true"
                )),
                Set.of(WaybillSegmentTag.COURIER_CAR, WaybillSegmentTag.COURIER_PRO)
            ),
            Arguments.of(
                "Есть два параметра в false",
                validateAndEnrichContext(Map.of(
                    EXPRESS_REQUIREMENTS_COURIER_PRO, "false",
                    EXPRESS_REQUIREMENTS_COURIER_CAR, "false"
                )),
                Set.of()
            ),
            Arguments.of(
                "Есть два параметра, один в false",
                validateAndEnrichContext(Map.of(
                    EXPRESS_REQUIREMENTS_COURIER_PRO, "true",
                    EXPRESS_REQUIREMENTS_COURIER_CAR, "false"
                )),
                Set.of(WaybillSegmentTag.COURIER_PRO)
            ),
            Arguments.of(
                "Есть все параметры в true",
                validateAndEnrichContext(Map.of(
                    EXPRESS_REQUIREMENTS_COURIER_PRO, "true",
                    EXPRESS_REQUIREMENTS_COURIER_CAR, "true",
                    EXPRESS_REQUIREMENTS_COURIER_PEDESTRIAN, "true"
                )),
                new HashSet<>(EXPRESS_REQUIREMENTS_PARAM_WAYBILL_TAG.values())
            ),
            Arguments.of(
                "Лишние теги не добавляются",
                validateAndEnrichContext(
                    Arrays.stream(PartnerExternalParamType.values())
                        .collect(Collectors.toMap(Function.identity(), v -> "true"))
                ),
                new HashSet<>(EXPRESS_REQUIREMENTS_PARAM_WAYBILL_TAG.values())
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> oneRequiredParam() {
        return Streams.concat(oneRequiredParam(true), oneRequiredParam(false));
    }

    @Nonnull
    private static Stream<Arguments> oneRequiredParam(boolean value) {
        return EXPRESS_REQUIREMENTS_PARAM_WAYBILL_TAG.keySet().stream()
            .sorted()
            .map(param -> Arguments.of(
                "Есть параметр " + param + " в " + value,
                validateAndEnrichContext(Map.of(param, value ? "true" : "false")),
                value ? Set.of(EXPRESS_REQUIREMENTS_PARAM_WAYBILL_TAG.get(param)) : Set.of()
            ));
    }

    @Test
    @DisplayName("Ошибка обогащения тегами требований - в контексте нет партнеров")
    void enrichWithRequirementsFail() {
        Order order = Order.builder()
            .waybill(List.of(
                getWaybillSegment(PartnerType.DROPSHIP, SegmentType.FULFILLMENT, 0),
                getWaybillSegment(
                    DELIVERY_PARTNER_ID,
                    PartnerType.DELIVERY,
                    SegmentType.COURIER,
                    2
                ).addTag(WaybillSegmentTag.CALL_COURIER)
            ))
            .build();

        softly.assertThatCode(
                () -> enricher.validateAndEnrich(order, new ValidateAndEnrichContext())
                    .getOrderModifier().apply(order).getWaybill()
            )
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Cannot find partner with id 1");
    }

    @DisplayName("Некорректные сегменты")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void enrichNotPerformed(
        @SuppressWarnings("unused") String name,
        List<WaybillSegment> segments
    ) {
        Order order = Order.builder().waybill(segments).build();
        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());

        softly.assertThat(results.getOrderModifier().apply(order).getWaybill().stream()
                .filter(w -> w.getPartnerType() == PartnerType.DELIVERY)
                .noneMatch(w -> w.hasTag(WaybillSegmentTag.EXPRESS))
            )
            .isTrue();
    }

    @Nonnull
    private static Stream<Arguments> enrichNotPerformed() {
        return Stream.of(
            Arguments.of(
                "Только fulfillment сегмент",
                List.of(getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0))
            ),
            Arguments.of(
                "Только delivery сегмент",
                List.of(
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 0)
                        .addTag(WaybillSegmentTag.CALL_COURIER)
                )
            ),
            Arguments.of(
                "У delivery сегмента не выставлен тэг CALL_COURIER",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 1)
                )
            ),
            Arguments.of(
                "Только fulfillment и pickup сегменты",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.PICKUP, 1)
                )
            ),
            Arguments.of(
                "Только delivery и pickup сегменты",
                List.of(
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 0)
                        .addTag(WaybillSegmentTag.CALL_COURIER),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.PICKUP, 1)
                )
            ),
            Arguments.of(
                "У delivery сегмента не выставлен тэг CALL_COURIER, есть pickup",
                List.of(
                    getWaybillSegment(PartnerType.FULFILLMENT, SegmentType.FULFILLMENT, 0),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.COURIER, 1),
                    getWaybillSegment(PartnerType.DELIVERY, SegmentType.PICKUP, 2)
                )
            )
        );
    }

    @Nonnull
    private static WaybillSegment getWaybillSegment(
        long partnerId,
        PartnerType partnerType,
        SegmentType segmentType,
        int index
    ) {
        return new WaybillSegment()
            .setPartnerType(partnerType)
            .setSegmentType(segmentType)
            .setWaybillSegmentIndex(index)
            .setPartnerId(partnerId);
    }

    @Nonnull
    private static WaybillSegment getWaybillSegment(PartnerType partnerType, SegmentType segmentType, int index) {
        return getWaybillSegment(DROPSHIP_ID, partnerType, segmentType, index);
    }

    @Nonnull
    private static ValidateAndEnrichContext validateAndEnrichContext(Map<PartnerExternalParamType, String> params) {
        return new ValidateAndEnrichContext().setPartners(List.of(
            PartnerResponse.newBuilder().id(DELIVERY_PARTNER_ID).build(),
            PartnerResponse.newBuilder()
                .id(DROPSHIP_ID)
                .params(
                    EntryStream.of(params)
                        .map(param -> new PartnerExternalParam(param.getKey().name(), "", param.getValue()))
                        .collect(Collectors.toList())
                )
                .build()
        ));
    }
}
