package ru.yandex.market.tpl.core.service.barcode;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.domain.HasBarcode;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefix;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixRepository;
import ru.yandex.market.tpl.core.domain.barcode_prefix.ReturnBarcodePrefixType;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor
class BarcodeValidatorTest extends TplAbstractTest {

    private final BarcodeValidator barcodeValidator;
    private final ReturnBarcodePrefixRepository barcodePrefixRepository;

    @DisplayName("Проверка, что для любого префикса неверные последюущие символы не проходят проверку")
    @ParameterizedTest
    @EnumSource(value = ReturnBarcodePrefixType.class)
    void throwIfNotDigitPartialReturn(ReturnBarcodePrefixType returnBarcodePrefixType) {
        List<String> barcodePrefixes = barcodePrefixRepository.findAllByType(returnBarcodePrefixType).stream()
                .map(ReturnBarcodePrefix::getBarcodePrefix)
                .collect(Collectors.toList());

        barcodePrefixes.forEach(prefix -> {
                    assertThrows(
                            TplIllegalArgumentException.class,
                            () -> barcodeValidator.validate(prefix + "&,#&rH", returnBarcodePrefixType)
                    );

                    assertThrows(
                            TplIllegalArgumentException.class,
                            () -> barcodeValidator.validate(prefix + "123a",
                                    returnBarcodePrefixType)
                    );
                }
        );


    }

    @Test
    void throwIfRepeatableBarcodes() {
        assertThrows(
                TplInvalidParameterException.class,
                () -> barcodeValidator.validateRepeatableBarcodes(List.of("BARCODE_1", "BARCODE_1"))
        );
    }

    @Test
    void throwIfAlreadyUserBarcodes() {
        assertThrows(
                TplInvalidParameterException.class,
                () -> barcodeValidator.validateAlreadyUsedBarcodes(
                        List.of("BARCODE_1, BARCODE_2"),
                        p -> List.of(ClassReturnBarcode.builder().barcode("BARCODE_1").build())
                )
        );

    }

    @DisplayName("Проверка, что для любого префикса верные последующие символы проходят проверку")
    @ParameterizedTest
    @EnumSource(value = ReturnBarcodePrefixType.class)
    void successValidate(ReturnBarcodePrefixType returnBarcodePrefixType) {
        List<String> barcodePrefixes = barcodePrefixRepository.findAllByType(returnBarcodePrefixType).stream()
                .map(ReturnBarcodePrefix::getBarcodePrefix)
                .collect(Collectors.toList());
        barcodePrefixes.forEach(prefix -> barcodeValidator.validate(prefix + "123", returnBarcodePrefixType));
    }

    @Data
    @Builder
    private static class ClassReturnBarcode implements HasBarcode {
        String barcode;
    }
}
