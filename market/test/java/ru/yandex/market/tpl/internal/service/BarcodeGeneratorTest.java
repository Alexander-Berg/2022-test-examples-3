package ru.yandex.market.tpl.internal.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.internal.TplIntAbstractTest;
import ru.yandex.market.tpl.internal.service.report.barcodes.BarcodeGenerator;
import ru.yandex.market.tpl.internal.service.report.barcodes.BarcodeType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RequiredArgsConstructor
public class BarcodeGeneratorTest extends TplIntAbstractTest {
    private final BarcodeGenerator barcodeGenerator;

    @ParameterizedTest
    @EnumSource(BarcodeType.class)
    public void testBarcodeMask(BarcodeType barcodeType) {
        try {
            var barcode = barcodeGenerator.generateBarcode(barcodeType);
            String barcodePrefix = barcodeType.getMaskName();
            assertThat(barcode.startsWith(barcodePrefix)).isTrue();
        } catch (Exception e) {
            if (e.getMessage().contains("does not exist")) {
                throw new TplIllegalArgumentException("Could not find sequence " + barcodeType.getSequenceName() +
                        " in the db for enum " + barcodeType.name());
            }
            throw e;
        }
    }
}
