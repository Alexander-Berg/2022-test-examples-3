package ru.yandex.market.logistics.lom.controller.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.CostDto;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;

@DisplayName("Валидация стоимостей заказа CostDto")
class OrderCostDtoValidationTest extends AbstractContextualTest {

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validOrderCostDtoProvider")
    @DisplayName("Провалидировать стоимости заказа")
    void validateOrderDto(
        @SuppressWarnings("unused") String displayName,
        Consumer<WaybillOrderRequestDto> orderBuilderModifier
    ) {
        WaybillOrderRequestDto orderDto = new WaybillOrderRequestDto();
        orderDto.setSenderId(1L)
            .setPlatformClientId(3L)
            .setReturnSortingCenterId(1L);
        orderBuilderModifier.accept(orderDto);
        Set<ConstraintViolation<WaybillOrderRequestDto>> violations = validator.validate(orderDto);
        softly.assertThat(violations).hasSize(0);
    }

    private static Stream<Arguments> validOrderCostDtoProvider() {
        return Stream.<Pair<String, Consumer<WaybillOrderRequestDto>>>of(
            Pair.of(
                "Список items пустой и itemsSum не указан",
                b -> {
                }
            ),
            Pair.of(
                "Цена товара не указана и itemsSum не указан",
                b -> b.setItems(firstItemWithPriceNull())
            ),
            Pair.of(
                "Значение цены товара не указано и itemsSum не указан",
                b -> b.setItems(firstItemWithPriceValueNull())
            ),
            Pair.of(
                "Количество товара не указано и itemsSum не указан",
                b -> b.setItems(firstItemWithCountNull())
            ),
            Pair.of(
                "Список items пустой и itemsSum = 0",
                b -> b.setCost(CostDto.builder().build())
            ),
            Pair.of(
                "Цена товара не указана и itemsSum любой",
                b -> b.setCost(CostDto.builder().build())
                    .setItems(firstItemWithPriceNull())
            ),
            Pair.of(
                "exchangeRate отличный от 1",
                b -> b.setCost(CostDto.builder().build())
                    .setItems(itemsWithExchangeRate())
            )
        ).map(pair -> Arguments.of(pair.getLeft(), pair.getRight()));
    }

    private static List<ItemDto> firstItemWithPriceNull() {
        return List.of(ItemDto.builder().price(null).build());
    }

    private static List<ItemDto> firstItemWithPriceValueNull() {
        return List.of(ItemDto.builder().price(MonetaryDto.builder().value(new BigDecimal("250.00")).build()).build());
    }

    private static List<ItemDto> firstItemWithCountNull() {
        return List.of(ItemDto.builder().count(null).build());
    }

    private static List<ItemDto> itemsWithExchangeRate() {
        return List.of(
            ItemDto.builder()
                .count(2)
                .price(MonetaryDto.builder().value(new BigDecimal("100")).exchangeRate(new BigDecimal("1.5")).build())
                .build(),
            ItemDto.builder()
                .count(3)
                .price(MonetaryDto.builder().value(new BigDecimal("300")).exchangeRate(new BigDecimal("2.5")).build())
                .build()
        );
    }
}
