package ru.yandex.market.promoboss.validator.service;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.promoboss.validator.exception.ConstraintListsValidationException;
import ru.yandex.mj.generated.server.model.MskuPromoConstraintDto;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.RegionPromoConstraintDto;
import ru.yandex.mj.generated.server.model.SupplierPromoConstraintsDto;
import ru.yandex.mj.generated.server.model.VendorPromoConstraintDto;
import ru.yandex.mj.generated.server.model.WarehousePromoConstraintDto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConstraintListsValidatorV2Test {

    private static final ConstraintListsValidator validator = new ConstraintListsValidator();

    @Test
    public void shouldNotThrowExceptionIfItemsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .mskusConstraints(null)
                .suppliersConstraints(null)
                .vendorsConstraints(null)
                .warehousesConstraints(null)
                .regionsConstraints(null)
                .ssku(null);

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    public void shouldNotThrowExceptionIfItemsNotExists() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .mskusConstraints(new MskuPromoConstraintDto()
                        .exclude(null)
                        .mskus(Collections.emptyList())
                )
                .suppliersConstraints(new SupplierPromoConstraintsDto()
                        .exclude(null)
                        .suppliers(Collections.emptyList())
                )
                .vendorsConstraints(new VendorPromoConstraintDto()
                        .exclude(null)
                        .vendors(Collections.emptyList())
                )
                .warehousesConstraints(new WarehousePromoConstraintDto()
                        .exclude(null)
                        .warehouses(Collections.emptyList())
                )
                .regionsConstraints(new RegionPromoConstraintDto()
                        .regions(Collections.emptyList())
                        .excludedRegions(Collections.emptyList())
                )
                .ssku(Collections.emptyList());

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    public void shouldNotThrowExceptionIfExcludeAndItemsExists() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .mskusConstraints(new MskuPromoConstraintDto()
                        .exclude(true)
                        .mskus(List.of(123L))
                )
                .suppliersConstraints(new SupplierPromoConstraintsDto()
                        .exclude(true)
                        .suppliers(List.of(123L))
                )
                .vendorsConstraints(new VendorPromoConstraintDto()
                        .exclude(true)
                        .vendors(List.of("123"))
                )
                .warehousesConstraints(new WarehousePromoConstraintDto()
                        .exclude(true)
                        .warehouses(List.of(123L))
                )
                .regionsConstraints(new RegionPromoConstraintDto()
                        .regions(List.of("region1"))
                        .excludedRegions(List.of("region2"))
                )
                .ssku(Collections.emptyList());

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    public static Stream<Arguments> shouldThrowExceptionIfExcludeNullAndItemsExistsSource() {
        return Stream.of(
                Arguments.of("Mskus.exclude is empty", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .mskusConstraints(new MskuPromoConstraintDto()
                                .exclude(null)
                                .mskus(List.of(123L))
                        )),
                Arguments.of("Suppliers.exclude is empty", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .suppliersConstraints(new SupplierPromoConstraintsDto()
                                .exclude(null)
                                .suppliers(List.of(123L))
                        )),
                Arguments.of("Vendors.exclude is empty", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .vendorsConstraints(new VendorPromoConstraintDto()
                                .exclude(null)
                                .vendors(List.of("vendor1"))
                        )),
                Arguments.of("Warehouses.exclude is empty", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .warehousesConstraints(new WarehousePromoConstraintDto()
                                .exclude(null)
                                .warehouses(List.of(123L))
                        ))
        );
    }

    @ParameterizedTest
    @MethodSource("shouldThrowExceptionIfExcludeNullAndItemsExistsSource")
    public void shouldThrowExceptionIfExcludeNullAndItemsExists(String message, Consumer<PromoRequestV2> consumer) {

        // setup
        PromoRequestV2 request = new PromoRequestV2();

        consumer.accept(request);

        // act and verify
        ConstraintListsValidationException exception = assertThrows(
                ConstraintListsValidationException.class,
                () -> validator.validate(request)
        );

        assertEquals(message, exception.getMessage());
    }

    public static Stream<Arguments> shouldThrowExceptionIfDuplicatesExistsSource() {
        return Stream.of(
                Arguments.of("Mskus contains duplicates: 125, 126", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .mskusConstraints(new MskuPromoConstraintDto()
                                .exclude(true)
                                .mskus(List.of(123L, 124L, 125L, 125L, 126L, 126L))
                        )),
                Arguments.of("Suppliers contains duplicates: 125, 126", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .suppliersConstraints(new SupplierPromoConstraintsDto()
                                .exclude(true)
                                .suppliers(List.of(123L, 124L, 125L, 125L, 126L, 126L))
                        )),
                Arguments.of("Vendors contains duplicates: vendor6, vendor5", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .vendorsConstraints(new VendorPromoConstraintDto()
                                .exclude(true)
                                .vendors(List.of("vendor3", "vendor4", "vendor5", "vendor5", "vendor6", "vendor6"))
                        )),
                Arguments.of("Warehouses contains duplicates: 125, 126", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .warehousesConstraints(new WarehousePromoConstraintDto()
                                .exclude(true)
                                .warehouses(List.of(123L, 124L, 125L, 125L, 126L, 126L))
                        )),
                Arguments.of("Regions contains duplicates: region15, region16, region4, region5, region6", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .regionsConstraints(new RegionPromoConstraintDto()
                                .regions(List.of("region3", "region4", "region5", "region5", "region6", "region6"))
                                .excludedRegions(List.of("region13", "region4", "region15", "region15", "region16", "region16"))
                        )),
                Arguments.of("SSKU contains duplicates: ssku5, ssku6", (Consumer<PromoRequestV2>) promoRequestV2 -> promoRequestV2
                        .ssku(List.of("ssku3", "ssku4", "ssku5", "ssku5", "ssku6", "ssku6"))
                )
        );
    }

    @ParameterizedTest
    @MethodSource("shouldThrowExceptionIfDuplicatesExistsSource")
    public void shouldThrowExceptionIfDuplicatesExists(String message, Consumer<PromoRequestV2> consumer) {

        // setup
        PromoRequestV2 request = new PromoRequestV2();

        consumer.accept(request);

        // act and verify
        ConstraintListsValidationException exception = assertThrows(
                ConstraintListsValidationException.class,
                () -> validator.validate(request)
        );

        assertEquals(message, exception.getMessage());
    }
}
