package ru.yandex.market.wms.api.converter;

import java.math.BigDecimal;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.wms.common.spring.service.converter.FulfillmentKorobyteToKorobyteDtoConverter;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.KorobyteDto;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class FulfillmentKorobyteToKorobyteDtoConverterTest {

    private SoftAssertions assertions = new SoftAssertions();

    @Test
    public void shouldSuccessConvert() {
        Korobyte korobyte = new Korobyte.KorobyteBuiler(
                10,
                20,
                30,
                BigDecimal.valueOf(2.55))
                .setWeightNet(BigDecimal.valueOf(2.6))
                .setWeightTare(BigDecimal.valueOf(2.62))
                .build();

        KorobyteDto actualKorobyte = FulfillmentKorobyteToKorobyteDtoConverter.convert(korobyte);

        assertSoftly(assertions -> {
            assertions.assertThat(actualKorobyte.getWidth()).isEqualTo(BigDecimal.valueOf(10));
            assertions.assertThat(actualKorobyte.getHeight()).isEqualTo(BigDecimal.valueOf(20));
            assertions.assertThat(actualKorobyte.getLength()).isEqualTo(BigDecimal.valueOf(30));
            assertions.assertThat(actualKorobyte.getWeightGross()).isEqualTo(BigDecimal.valueOf(2.55));
            assertions.assertThat(actualKorobyte.getWeightNet()).isEqualTo(BigDecimal.valueOf(2.6));
            assertions.assertThat(actualKorobyte.getWeightTare()).isEqualTo(BigDecimal.valueOf(2.62));
        });
    }

    @Test
    public void shouldSuccessConvertIfLengthNull() {
        Korobyte korobyte = new Korobyte.KorobyteBuiler(
                10,
                20,
                null,
                BigDecimal.valueOf(2.55))
                .setWeightNet(BigDecimal.valueOf(2.6))
                .setWeightTare(BigDecimal.valueOf(2.62))
                .build();

        KorobyteDto actualKorobyte = FulfillmentKorobyteToKorobyteDtoConverter.convert(korobyte);

        assertSoftly(assertions -> {
            assertions.assertThat(actualKorobyte.getWidth()).isEqualTo(BigDecimal.valueOf(10));
            assertions.assertThat(actualKorobyte.getHeight()).isEqualTo(BigDecimal.valueOf(20));
            assertions.assertThat(actualKorobyte.getLength()).isNull();
            assertions.assertThat(actualKorobyte.getWeightGross()).isEqualTo(BigDecimal.valueOf(2.55));
            assertions.assertThat(actualKorobyte.getWeightNet()).isEqualTo(BigDecimal.valueOf(2.6));
            assertions.assertThat(actualKorobyte.getWeightTare()).isEqualTo(BigDecimal.valueOf(2.62));
        });
    }

    @Test
    public void shouldNotConvertIfKorobyteNull() {
        KorobyteDto actualKorobyte = FulfillmentKorobyteToKorobyteDtoConverter.convert(null);

        assertions.assertThat(actualKorobyte).isNull();
    }
}
