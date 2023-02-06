package ru.yandex.market.logistics.nesu.converter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import one.util.streamex.IntStreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.model.dto.ItemDto;
import ru.yandex.market.logistics.lom.model.dto.KorobyteDto;
import ru.yandex.market.logistics.lom.model.dto.MonetaryDto;
import ru.yandex.market.logistics.lom.model.dto.OrderItemBoxDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.enums.StorageUnitType;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.api.converter.EnumConverter;
import ru.yandex.market.logistics.nesu.api.model.Dimensions;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.converter.model.OrderUnitItems;
import ru.yandex.market.logistics.nesu.dto.Item;
import ru.yandex.market.logistics.nesu.dto.MultiplaceItem;
import ru.yandex.market.logistics.nesu.dto.Place;
import ru.yandex.market.logistics.nesu.model.entity.Sender;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class OrderItemsConverterTest extends AbstractTest {

    public static final long SHOP_ID = 1L;
    private final FeatureProperties featureProperties = mock(FeatureProperties.class);

    private final OrderItemsConverter converter = new OrderItemsConverter(
        new DimensionsConverter(),
        new EnumConverter(),
        featureProperties
    );

    private List<Place> places;
    private List<MultiplaceItem> items;

    @Test
    @DisplayName("Нет мест, только габариты всего заказа")
    void nullPlacesNullItems() {
        assertConverted(
            OrderUnitItems.builder()
                .rootUnit(
                    rootUnit()
                        .dimensions(new KorobyteDto(15, 150, 45, BigDecimal.ONE))
                        .externalId("generated-0")
                        .build()
                )
                .items(null)
                .build(),
            createDimensions(15, 150, 45)
        );
    }

    @Test
    @DisplayName("Нет товаров")
    void noItems() {
        places = List.of(
            new Place().setExternalId("external-id")
        );

        assertConverted(
            OrderUnitItems.builder()
                .items(List.of())
                .rootUnit(rootUnit().build())
                .placeUnits(List.of(place().externalId("external-id").build()))
                .build()
        );
    }

    @Test
    @DisplayName("Не генерируем externalId мест")
    void doNotGeneratePlaceId() {
        places = List.of(
            new Place(),
            new Place().setExternalId("generated-0"),
            new Place(),
            new Place().setExternalId("generated-2"),
            new Place().setExternalId("external-id")
        );

        assertConverted(
            OrderUnitItems.builder()
                .rootUnit(rootUnit().externalId("generated-1").build())
                .placeUnits(List.of(
                    place().externalId(null).parentExternalId("generated-1").build(),
                    place().externalId("generated-0").parentExternalId("generated-1").build(),
                    place().externalId(null).parentExternalId("generated-1").build(),
                    place().externalId("generated-2").parentExternalId("generated-1").build(),
                    place().externalId("external-id").parentExternalId("generated-1").build()
                ))
                .items(List.of())
                .build()
        );
    }

    @Test
    @DisplayName("Товары в грузоместах")
    void itemsInPlaces() {
        Dimensions dimensions = new Dimensions().setLength(10).setWidth(20).setHeight(30).setWeight(BigDecimal.ONE);
        places = List.of(
            new Place()
                .setExternalId("generated-1")
                .setDimensions(dimensions)
                .setItems(List.of(new Item().setDimensions(dimensions).setSupplierInn("some-inn")))
        );

        KorobyteDto korobyte = KorobyteDto.builder()
            .length(10)
            .width(20)
            .height(30)
            .weightGross(BigDecimal.ONE)
            .build();
        assertConverted(
            OrderUnitItems.builder()
                .rootUnit(rootUnit().dimensions(korobyte).build())
                .placeUnits(List.of(
                    place()
                        .externalId("generated-1")
                        .dimensions(korobyte)
                        .build()
                ))
                .items(List.of(
                    ItemDto.builder()
                        .vendorId(SHOP_ID)
                        .price(new MonetaryDto("RUB", null, BigDecimal.ONE))
                        .assessedValue(new MonetaryDto("RUB", null, BigDecimal.ONE))
                        .dimensions(korobyte)
                        .boxes(List.of(
                            OrderItemBoxDto.builder()
                                .dimensions(korobyte)
                                .storageUnitExternalIds(Set.of("generated-1"))
                                .storageUnitIndexes(List.of(0))
                                .build()
                        ))
                        .supplierInn("some-inn")
                        .build()
                ))
                .build()
        );
    }

    @Test
    @DisplayName("Товары")
    void items() {
        doReturn(true).when(featureProperties).isUseMultiplaceLogic();
        Dimensions dimensions = new Dimensions().setLength(10).setWidth(20).setHeight(30).setWeight(BigDecimal.ONE);
        places = List.of(
            new Place()
                .setDimensions(dimensions)
                .setExternalId("ext-id")
        );

        MultiplaceItem item = new MultiplaceItem();
        item.setDimensions(dimensions).setSupplierInn("some-inn");
        items = List.of(item);

        KorobyteDto korobyte = KorobyteDto.builder()
            .length(10)
            .width(20)
            .height(30)
            .weightGross(BigDecimal.ONE)
            .build();
        assertConverted(
            OrderUnitItems.builder()
                .rootUnit(rootUnit().dimensions(korobyte).build())
                .placeUnits(List.of(
                    place()
                        .externalId("ext-id")
                        .dimensions(korobyte)
                        .build()
                ))
                .items(List.of(
                    ItemDto.builder()
                        .vendorId(SHOP_ID)
                        .price(new MonetaryDto("RUB", null, BigDecimal.ONE))
                        .assessedValue(new MonetaryDto("RUB", null, BigDecimal.ONE))
                        .dimensions(korobyte)
                        .boxes(List.of(
                            OrderItemBoxDto.builder()
                                .dimensions(korobyte)
                                .storageUnitExternalIds(null)
                                .build()
                        ))
                        .supplierInn("some-inn")
                        .build()
                ))
                .build()
        );
    }

    @Test
    @DisplayName("Переопределяем общие габариты заказа")
    void customOrderDimensions() {
        places = List.of(
            new Place()
                .setDimensions(createDimensions(10, 100, 40))
                .setExternalId("ext-id"),
            new Place()
                .setDimensions(createDimensions(20, 200, 50))
                .setExternalId("ext-id-2")
        );

        assertConverted(
            OrderUnitItems.builder()
                .rootUnit(
                    rootUnit().dimensions(new KorobyteDto(15, 150, 45, BigDecimal.ONE)).build()
                )
                .placeUnits(List.of(
                    place()
                        .externalId("ext-id")
                        .dimensions(new KorobyteDto(10, 100, 40, BigDecimal.ONE))
                        .build(),
                    place()
                        .externalId("ext-id-2")
                        .dimensions(new KorobyteDto(20, 200, 50, BigDecimal.ONE))
                        .build()
                ))
                .items(List.of())
                .build(),
            createDimensions(15, 150, 45)
        );
    }

    @Nonnull
    private static Stream<Arguments> severalBoxes() {
        return Stream.of(
            Arguments.of(
                List.of(
                    createDimensions(30, 20, 10),
                    createDimensions(30, 20, 10)
                ),
                KorobyteDto.builder().length(30).width(20).height(20).weightGross(BigDecimal.valueOf(2)).build()
            ),
            Arguments.of(
                List.of(
                    createDimensions(30, 50, 10),
                    createDimensions(50, 10, 50),
                    createDimensions(10, 40, 10)
                ),
                KorobyteDto.builder().length(50).width(50).height(30).weightGross(BigDecimal.valueOf(3)).build()
            ),
            Arguments.of(
                List.of(
                    createDimensions(10, 100, 40),
                    createDimensions(80, 10, 50),
                    createDimensions(20, 60, 30)
                ),
                KorobyteDto.builder().length(100).width(50).height(40).weightGross(BigDecimal.valueOf(3)).build()
            ),
            Arguments.of(
                List.of(
                    createDimensions(10, 100, 40),
                    createDimensions(80, 10, 50),
                    createDimensions(20, 60, 60)
                ),
                KorobyteDto.builder().length(100).width(60).height(40).weightGross(BigDecimal.valueOf(3)).build()
            ),
            Arguments.of(
                List.of(
                    createDimensions(20, 30, 50)
                ),
                KorobyteDto.builder().length(20).width(30).height(50).weightGross(BigDecimal.valueOf(1)).build()
            ),
            Arguments.of(
                List.of(),
                null
            )
        );
    }

    @ParameterizedTest
    @MethodSource("severalBoxes")
    @DisplayName("Несколько коробок с товарами")
    void severalItems(
        List<Dimensions> inputBoxes,
        KorobyteDto rootUnitDimensions
    ) {
        DimensionsConverter dimensionsConverter = new DimensionsConverter();

        places = IntStreamEx.range(inputBoxes.size())
            .mapToObj(i -> createPlace(inputBoxes.get(i), "externalId-" + i))
            .collect(Collectors.toList());

        AtomicInteger index = new AtomicInteger();
        assertConverted(
            OrderUnitItems.builder()
                .rootUnit(rootUnit().dimensions(rootUnitDimensions).build())
                .placeUnits(
                    places.stream()
                        .map(place ->
                            place()
                                .externalId(place.getExternalId())
                                .dimensions(dimensionsConverter.toLomKorobyte(place.getDimensions()))
                                .build()
                        )
                        .collect(Collectors.toList())
                )
                .items(
                    places.stream()
                        .map(place -> {
                            var korobyte = dimensionsConverter.toLomKorobyte(place.getDimensions());
                            return ItemDto.builder()
                                .vendorId(SHOP_ID)
                                .price(new MonetaryDto("RUB", null, BigDecimal.ONE))
                                .assessedValue(new MonetaryDto("RUB", null, BigDecimal.ONE))
                                .dimensions(korobyte)
                                .boxes(List.of(
                                    OrderItemBoxDto.builder()
                                        .dimensions(korobyte)
                                        .storageUnitExternalIds(Set.of(place.getExternalId()))
                                        .storageUnitIndexes(List.of(index.getAndIncrement()))
                                        .build()
                                ))
                                .build();
                        })
                        .collect(Collectors.toList())
                )
                .build()
        );
    }

    @Nonnull
    private StorageUnitDto.StorageUnitDtoBuilder rootUnit() {
        return StorageUnitDto.builder()
            .type(StorageUnitType.ROOT)
            .externalId("generated-0");
    }

    @Nonnull
    private StorageUnitDto.StorageUnitDtoBuilder place() {
        return StorageUnitDto.builder()
            .type(StorageUnitType.PLACE)
            .parentExternalId("generated-0");
    }

    @Nonnull
    private Place createPlace(Dimensions dimensions, String externalId) {
        return new Place()
            .setExternalId(externalId)
            .setDimensions(dimensions)
            .setItems(List.of(new Item().setDimensions(dimensions)));
    }

    @Nonnull
    private static Dimensions createDimensions(Integer length, Integer width, Integer height) {
        return new Dimensions().setLength(length).setWidth(width).setHeight(height).setWeight(BigDecimal.ONE);
    }

    private void assertConverted(OrderUnitItems expected) {
        assertConverted(expected, null);
    }

    private void assertConverted(OrderUnitItems expected, @Nullable Dimensions orderDimensions) {
        OrderUnitItems result = converter.getUnitItems(
            SHOP_ID,
            new Sender(),
            orderDimensions,
            places,
            items
        );
        softly.assertThat(result).usingRecursiveComparison().isEqualTo(expected);

        if (orderDimensions != null) {
            softly.assertThat(Objects.requireNonNull(expected.getAllUnits()).contains(expected.getRootUnit()));
        }
    }

}
