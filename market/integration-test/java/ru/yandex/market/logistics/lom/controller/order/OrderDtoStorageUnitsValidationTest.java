package ru.yandex.market.logistics.lom.controller.order;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public class OrderDtoStorageUnitsValidationTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешно создается заказ без нерутовых юнитов")
    void testOk() throws Exception {
        WaybillOrderRequestDto orderDto = new WaybillOrderRequestDto();
        orderDto.setSenderId(1L)
            .setPlatformClientId(3L)
            .setReturnSortingCenterId(1L)
            .setUnits(List.of(
                StorageUnitDto.builder()
                    .type(StorageUnitType.ROOT)
                    .externalId("root_unit")
                    .build()
            ));
        mockMvc.perform(request(HttpMethod.POST, "/orders", orderDto))
            .andExpect(status().isOk());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("invalidStorageUnits")
    @DisplayName("Провалидировать структуру юнитов")
    void validateOrderDto(
        String errorMessage,
        Consumer<WaybillOrderRequestDto> orderModifier,
        String invalidField
    ) throws Exception {
        WaybillOrderRequestDto orderDto = new WaybillOrderRequestDto();
        orderDto.setSenderId(1L)
            .setPlatformClientId(3L)
            .setReturnSortingCenterId(1L);
        orderModifier.accept(orderDto);
        mockMvc.perform(request(HttpMethod.POST, "/orders", orderDto))
            .andExpect(status().isBadRequest())
            .andExpect(content().json(buildBadRequestMessage(invalidField, errorMessage)));
    }

    @Nonnull
    private static Stream<Arguments> invalidStorageUnits() {
        return Stream.of(
            Arguments.of(
                "root units must not be empty",
                noRootUnits(),
                "units"
            ),
            Arguments.of(
                "root units must not have parent",
                rootWithParent(),
                "units"
            ),
            Arguments.of(
                "root units must have externalId",
                rootWithoutExternalId(),
                "units"
            ),
            Arguments.of(
                "not root units must have parent",
                placeWithoutParent(),
                "units"
            ),
            Arguments.of(
                "units external ids must not be equal",
                equalsExternalIds(),
                "units"
            ),
            Arguments.of(
                "all parent units must exist",
                parentUnitDoesNotExist(),
                "units"
            ),
            Arguments.of(
                "the structure of units must not have cycles",
                cycle(),
                "units"
            ),
            Arguments.of(
                "all root units must be used in waybill",
                declaredUnitsAreNotUsed(),
                "units"
            ),
            Arguments.of(
                "all used in waybill units must be declared",
                usedInWaybillUnitsAreNotDeclared(),
                "units"
            ),
            Arguments.of(
                "all used in boxes units must be declared",
                usedInBoxesUnitsAreNotDeclared(),
                "units"
            ),
            Arguments.of(
                "all used in waybill units must be equal to declared root units",
                waybillUnitIsNotRoot(),
                "units"
            ),
            Arguments.of(
                "each box must have exactly one unit from each unit tree",
                boxWithTwoUnitsFromOneWaybill(),
                "units"
            ),
            Arguments.of(
                "each box must have exactly one unit from each unit tree",
                boxWithoutUnitFromOneWaybill(),
                "units"
            ),
            Arguments.of(
                "cannot find storage unit with index -1",
                boxWithUnitIndex(-1),
                "items[0].boxes[0].storageUnitIndexes[0]"
            ),
            Arguments.of(
                "cannot find storage unit with index 3",
                boxWithUnitIndex(3),
                "items[0].boxes[0].storageUnitIndexes[0]"
            ),
            Arguments.of(
                "cannot attach item box to storage unit with index 0, because it has 'ROOT' type",
                boxWithUnitIndex(0),
                "items[0].boxes[0].storageUnitIndexes[0]"
            )
        );
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> noRootUnits() {
        return b -> b.setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("unit_1")
                .parentExternalId("unit_2")
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> rootWithoutExternalId() {
        return b -> b.setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> rootWithParent() {
        return b -> b.setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit")
                .parentExternalId("parent_unit")
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> placeWithoutParent() {
        return b -> b.setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("unit")
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> equalsExternalIds() {
        return b -> b.setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit")
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> parentUnitDoesNotExist() {
        return b -> b.setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit_1")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("unit_1")
                .parentExternalId("root_unit_2")
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> cycle() {
        return b -> b.setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("unit_1")
                .parentExternalId("root_unit")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("unit_2")
                .parentExternalId("unit_3")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("unit_3")
                .parentExternalId("unit_2")
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> declaredUnitsAreNotUsed() {
        return b -> b.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.PICKUP)
                .rootStorageUnitExternalId("root_unit")
                .build()
        ))
            .setUnits(List.of(
                StorageUnitDto.builder()
                    .type(StorageUnitType.ROOT)
                    .externalId("root_unit")
                    .build(),
                StorageUnitDto.builder()
                    .type(StorageUnitType.PLACE)
                    .externalId("unit_1")
                    .parentExternalId("root_unit")
                    .build(),
                StorageUnitDto.builder()
                    .type(StorageUnitType.ROOT)
                    .externalId("root_unit_2")
                    .build()
            ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> usedInWaybillUnitsAreNotDeclared() {
        return b -> b.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.FULFILLMENT)
                .rootStorageUnitExternalId("root_unit_1")
                .build(),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.PICKUP)
                .rootStorageUnitExternalId("root_unit_2")
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> usedInBoxesUnitsAreNotDeclared() {
        return b -> b.setItems(List.of(
            ItemDto.builder()
                .boxes(List.of(
                    OrderItemBoxDto.builder().storageUnitExternalIds(Set.of("storage_unit_external_id_1")).build(),
                    OrderItemBoxDto.builder().storageUnitExternalIds(Set.of("storage_unit_external_id_2")).build()
                ))
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> waybillUnitIsNotRoot() {
        return b -> b.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.FULFILLMENT)
                .rootStorageUnitExternalId("unit_1")
                .build(),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.PICKUP)
                .rootStorageUnitExternalId("root_unit")
                .build()
        )).setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("unit_1")
                .parentExternalId("root_unit")
                .build()
        )).setItems(List.of(
            ItemDto.builder()
                .boxes(List.of(OrderItemBoxDto.builder().storageUnitExternalIds(Set.of("root_unit")).build()))
                .build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> boxWithTwoUnitsFromOneWaybill() {
        return b -> b.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.FULFILLMENT)
                .rootStorageUnitExternalId("root_unit_1")
                .build(),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.PICKUP)
                .rootStorageUnitExternalId("root_unit_2")
                .build()
        )).setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit_1")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("place_unit_1")
                .parentExternalId("root_unit_1")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit_2")
                .build()
        )).setItems(List.of(
            ItemDto.builder()
                .boxes(List.of(
                    OrderItemBoxDto.builder()
                        .storageUnitExternalIds(Set.of("root_unit_1", "place_unit_1", "root_unit_2"))
                        .build()
                )).build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> boxWithoutUnitFromOneWaybill() {
        return b -> b.setWaybill(List.of(
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.FULFILLMENT)
                .rootStorageUnitExternalId("root_unit_1")
                .build(),
            WaybillSegmentDto.builder()
                .segmentType(SegmentType.PICKUP)
                .rootStorageUnitExternalId("root_unit_2")
                .build()
        )).setUnits(List.of(
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit_1")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.PLACE)
                .externalId("place_unit_1")
                .parentExternalId("root_unit_1")
                .build(),
            StorageUnitDto.builder()
                .type(StorageUnitType.ROOT)
                .externalId("root_unit_2")
                .build()
        )).setItems(List.of(
            ItemDto.builder()
                .boxes(List.of(
                    OrderItemBoxDto.builder()
                        .storageUnitExternalIds(Set.of("place_unit_1"))
                        .build()
                )).build()
        ));
    }

    @Nonnull
    private static Consumer<WaybillOrderRequestDto> boxWithUnitIndex(int index) {
        return b -> b
            .setUnits(List.of(
                StorageUnitDto.builder()
                    .type(StorageUnitType.ROOT)
                    .externalId("root_unit_1")
                    .build(),
                StorageUnitDto.builder()
                    .type(StorageUnitType.PLACE)
                    .externalId("place_unit_1")
                    .parentExternalId("root_unit_1")
                    .build()
            ))
            .setItems(List.of(
                ItemDto.builder()
                    .boxes(List.of(
                        OrderItemBoxDto.builder().storageUnitIndexes(List.of(index)).build()
                    ))
                    .build()
            ));
    }

    @Nonnull
    private String buildBadRequestMessage(@Nonnull String fieldName, @Nonnull String message) {
        return String.format(
            "{\"message\": \"[FieldError(propertyPath=%s, message=%s)]\"}",
            fieldName, message
        );
    }
}
