package ru.yandex.market.gutgin.tms.service.goodcontent;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawParamValue;
import ru.yandex.market.partner.content.common.entity.goodcontent.SkuTicket;
import ru.yandex.market.partner.content.common.service.DeduplicateService;
import ru.yandex.market.robot.db.ParameterValueComposer;

public class DeduplicateServiceTest {

    @Test
    public void deduplicateBarcodes() {
        RawParamValue barcode = new RawParamValue(ParameterValueComposer.BARCODE_ID,
            ParameterValueComposer.BARCODE,
            "0000000000");

        RawParamValue vendorCode = new RawParamValue(ParameterValueComposer.VENDOR_CODE_ID,
            ParameterValueComposer.VENDOR_CODE,
            "VENDOR_CODE");

        final SkuTicket skuTicket = SkuTicket.newBuilder()
            .addRawParamValue(barcode)
            .addRawParamValue(barcode)
            .addRawParamValue(vendorCode)
            .addRawParamValue(vendorCode)
            .build();

        DeduplicateService.deduplicateBarcodes(skuTicket);
        Assertions.assertThat(skuTicket.getRawParamValues()).hasSize(3);
        Assertions.assertThat(skuTicket.getRawParamValues(ParameterValueComposer.BARCODE_ID)).hasSize(1);
        Assertions.assertThat(skuTicket.getRawParamValues(ParameterValueComposer.VENDOR_CODE_ID)).hasSize(2);
    }
}