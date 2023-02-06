package ru.yandex.market.promoboss.validator.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.promoboss.validator.exception.FieldsValidationException;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.PromoSrcParams;
import ru.yandex.mj.generated.server.model.SrcCifaceDtoV2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SrcCifaceFieldsValidatorV2Test {

    private static final SrcCifaceFieldsValidator validator = new SrcCifaceFieldsValidator();

    @Test
    public void shouldSuccessValidate() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        assertDoesNotThrow(() -> validator.validate(request));
    }

    @Test
    public void shouldThrowExceptionIfDepartmentIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(null)
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("Department field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfDepartmentIsEmpty() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(Collections.emptyList())
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("Department field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfDepartmentContainsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(Stream.of("department1", null).toList())
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("Department field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfDepartmentContainsEmptyString() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", ""))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("Department field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfPurposeIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("Purpose field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfPurposeIsEmptyString() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("Purpose field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfCompensationSourceIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("CompensationSource field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfCompensationSourceIsEmptyString() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("CompensationSource field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfTradeManagerIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("TradeManager field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfTradeManagerIsEmptyString() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("TradeManager field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfCatManagerIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("Markom field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfCatManagerIsEmptyString() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("Markom field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfpromoKindIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("PromoKind field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfpromoKindIsEmptyString() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("")
                                        .supplierType("supplierType")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("PromoKind field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfSupplierTypeIsNull() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .promoKind("promoKind")
                                        .markom("catManager")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("SupplierType field is not passed or empty", e.getMessage());
    }

    @Test
    public void shouldThrowExceptionIfSupplierTypeIsEmptyString() {

        // setup
        PromoRequestV2 request = new PromoRequestV2()
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .departments(List.of("department1", "department2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("")
                                )
                );

        // act and verify
        FieldsValidationException e = assertThrows(FieldsValidationException.class,
                () -> validator.validate(request));

        assertEquals("SupplierType field is not passed or empty", e.getMessage());
    }
}
