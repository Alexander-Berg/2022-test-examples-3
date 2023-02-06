package ru.yandex.market.logistics.iris.converter;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.market.logistics.iris.core.index.complex.Barcode;
import ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource;

public class BarcodesConverterTest {

    @Test
    public void shouldConvertIrisBarcodeToLgw() {
        final Barcode irisBarcode = new Barcode("code", "type", BarcodeSource.SUPPLIER);

        BarcodesConverter converter = new BarcodesConverter();
        ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode lgwBarcode = converter.toLgwBarcode(irisBarcode);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(lgwBarcode).isNotNull();
            softly.assertThat(lgwBarcode.getCode()).isEqualTo("code");
            softly.assertThat(lgwBarcode.getType()).isEqualTo("type");
            softly.assertThat(lgwBarcode.getSource()).isEqualTo(ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource.SUPPLIER);
        });
    }

    @Test
    public void shouldConvertLgwBarcodeToIris() {
        final ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode lgwBarcode = new ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode("code", "type", ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource.SUPPLIER);

        BarcodesConverter converter = new BarcodesConverter();
        Barcode irisBarcode = converter.toIrisBarcode(lgwBarcode);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(irisBarcode).isNotNull();
            softly.assertThat(irisBarcode.getCode()).isEqualTo("code");
            softly.assertThat(irisBarcode.getType()).isEqualTo("type");
            softly.assertThat(irisBarcode.getSource()).isEqualTo(BarcodeSource.SUPPLIER);
        });
    }

    @Test
    public void  shouldConvertIrisBarcodesToLgw() {
        List<Barcode> irisBarcodes = ImmutableList.of(
                new Barcode("code1", "type1", BarcodeSource.SUPPLIER),
                new Barcode("code2", "type2", BarcodeSource.PARTNER)
        );

        BarcodesConverter converter = new BarcodesConverter();
        List<ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode> lgwBarcodes = converter.toLgwBarcodes(irisBarcodes);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(lgwBarcodes.size()).isEqualTo(2);

            softly.assertThat(lgwBarcodes.get(0).getCode()).isEqualTo("code1");
            softly.assertThat(lgwBarcodes.get(0).getType()).isEqualTo("type1");
            softly.assertThat(lgwBarcodes.get(0).getSource()).isEqualTo(ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource.SUPPLIER);

            softly.assertThat(lgwBarcodes.get(1).getCode()).isEqualTo("code2");
            softly.assertThat(lgwBarcodes.get(1).getType()).isEqualTo("type2");
            softly.assertThat(lgwBarcodes.get(1).getSource()).isEqualTo(ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource.PARTNER);
        });
    }
}
