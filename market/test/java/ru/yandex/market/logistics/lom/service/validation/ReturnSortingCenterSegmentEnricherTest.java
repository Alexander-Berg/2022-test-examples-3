package ru.yandex.market.logistics.lom.service.validation;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.Location;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.PartnerType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.entity.enums.tags.WaybillSegmentTag;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.ReturnSortingCenterSegmentEnricher;

@DisplayName("Валидация и обогащение возвратного сегмента вейбилла")
class ReturnSortingCenterSegmentEnricherTest extends AbstractTest {

    private final ReturnSortingCenterSegmentEnricher enricher = new ReturnSortingCenterSegmentEnricher();

    @Test
    @DisplayName("Возвратный СЦ уже существует")
    void returnSortingCenterAlreadyExists() {
        var partnerId = 1L;
        Order order = new Order()
            .setReturnSortingCenterId(partnerId)
            .setWaybill(
                List.of(
                    new WaybillSegment()
                        .setPartnerId(partnerId)
                        .setPartnerType(PartnerType.SORTING_CENTER)
                )
            );

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        softly.assertThat(results.isValidationPassed()).isTrue();

        results.getOrderModifier().apply(order);
        softly.assertThat(order.getWaybill()).hasSize(1);
    }

    @DisplayName("Возвратный партнер - не СЦ")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(value = PartnerType.class, names = {"SORTING_CENTER", "FULFILLMENT"}, mode = EnumSource.Mode.EXCLUDE)
    void returnPartnerNotSortingCenter(PartnerType returnPartnerType) {
        Order order = new Order()
            .setReturnSortingCenterId(1L)
            .setReturnSortingCenterWarehouse(createWarehouse())
            .setWaybill(List.of(new WaybillSegment().setPartnerId(2L)));

        var context = new ValidateAndEnrichContext();
        context.setPartnerTypeById(Map.of(1L, returnPartnerType));

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();

        results.getOrderModifier().apply(order);
        softly.assertThat(order.getWaybill()).hasSize(1);
    }

    @Test
    @DisplayName("Возвратный склад не был обогащен, новый сегмент вейбилла не будет добавлен")
    void returnSortingCenterWarehouseIsNull() {
        Order order = new Order()
            .setReturnSortingCenterId(1L)
            .setWaybill(List.of(new WaybillSegment().setPartnerId(2L)));

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, new ValidateAndEnrichContext());
        softly.assertThat(results.isValidationPassed()).isTrue();

        softly.assertThatThrownBy(
            () -> results.getOrderModifier().apply(order),
            "returnSortingCenterWarehouse is null. order: %s",
            order.getId()
        );
    }

    @Test
    @DisplayName("Успех обогащения возвратного сегмента")
    void enrichingSucceeded() {
        var context = new ValidateAndEnrichContext();
        context.setPartnerTypeById(Map.of(1L, PartnerType.SORTING_CENTER));

        Location returnSortingCenterWarehouse = createWarehouse();
        Order order = new Order()
            .setReturnSortingCenterId(1L)
            .setWaybill(List.of(new WaybillSegment().setPartnerId(2L)))
            .setReturnSortingCenterWarehouse(returnSortingCenterWarehouse);

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(results.getOrderModifier().apply(order).getWaybill())
            .usingRecursiveFieldByFieldElementComparator()
            .contains(
                new WaybillSegment()
                    .setPartnerId(1L)
                    .setOrder(order)
                    .setWaybillShipment(
                        new WaybillSegment.WaybillShipment()
                            .setLocationTo(returnSortingCenterWarehouse)
                    )
                    .setSegmentType(SegmentType.SORTING_CENTER)
                    .setPartnerType(PartnerType.SORTING_CENTER)
                    .setWaybillSegmentIndex(1)
            );
    }

    @Test
    @DisplayName("Успех обогащения возвратного сегмента FF")
    void enrichingSucceededFF() {
        var context = new ValidateAndEnrichContext();
        context.setPartnerTypeById(
            Map.of(
                1L, PartnerType.FULFILLMENT,
                2L, PartnerType.FULFILLMENT
            )
        );

        Location returnFFWarehouse = createWarehouse();
        Order order = new Order()
            .setReturnSortingCenterId(1L)
            .setWaybill(
                List.of(
                    new WaybillSegment().setPartnerId(2L)
                        .setSegmentType(SegmentType.FULFILLMENT)
                        .setPartnerType(PartnerType.FULFILLMENT)
                        .setExternalId("exId")
                )
            )
            .setReturnSortingCenterWarehouse(returnFFWarehouse);

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();
        softly.assertThat(results.getOrderModifier().apply(order).getWaybill().get(1))
            .usingRecursiveComparison()
            .isEqualTo(
                new WaybillSegment()
                    .setPartnerId(1L)
                    .setOrder(order)
                    .setWaybillShipment(
                        new WaybillSegment.WaybillShipment()
                            .setLocationTo(returnFFWarehouse)
                    )
                    .setSegmentType(SegmentType.FULFILLMENT)
                    .setPartnerType(PartnerType.FULFILLMENT)
                    .addTag(WaybillSegmentTag.RETURN)
                    .setWaybillSegmentIndex(1)
            );
    }

    @Test
    @DisplayName("Ошибка обогащения заказа без прямого сегмента FF")
    void enrichingFailedFF() {
        var context = new ValidateAndEnrichContext();
        context.setPartnerTypeById(Map.of(1L, PartnerType.FULFILLMENT));

        Location returnFFWarehouse = createWarehouse();
        Order order = new Order()
            .setReturnSortingCenterId(1L)
            .setWaybill(
                List.of(
                    new WaybillSegment().setPartnerId(2L)
                        .setSegmentType(SegmentType.FULFILLMENT)
                        .setPartnerType(PartnerType.DROPSHIP)
                        .setExternalId("exId")
                )
            )
            .setReturnSortingCenterWarehouse(returnFFWarehouse);

        ValidateAndEnrichResults results = enricher.validateAndEnrich(order, context);
        softly.assertThat(results.isValidationPassed()).isTrue();

        softly.assertThatThrownBy(
            () -> results.getOrderModifier().apply(order),
            "Direct fulfillment segment is not fulfillment, order: %s",
            order.getId()
        ).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("It's not possible to create return fulfillment segment " +
                "if direct segment partner type is not FULFILLMENT.");
    }

    private Location createWarehouse() {
        return new Location().setWarehouseId(3L);
    }
}
