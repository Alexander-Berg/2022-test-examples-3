package ru.yandex.market.wms.shippingsorter.sorting.service;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.exception.BoxWeightExceededException;
import ru.yandex.market.wms.shippingsorter.sorting.model.BoxWeights;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class BoxWeightsServiceTest extends IntegrationTest {

    @Autowired
    private BoxWeightsService boxWeightsService;

    @Test
    @DatabaseSetup("/sorting/service/box-weights/immutable.xml")
    public void shouldSuccessCalcBoxWeight() {
        BoxWeights boxWeights = boxWeightsService.getWeights(1500);

        assertSoftly(assertions -> {
            assertions.assertThat(boxWeights).isNotNull();
            assertions.assertThat(boxWeights.getMinWeight().intValue()).isEqualTo(750);
            assertions.assertThat(boxWeights.getMaxWeight().intValue()).isEqualTo(2250);
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/box-weights/immutable.xml")
    public void shouldRoundUpMinWeight() {
        BoxWeights boxWeights = boxWeightsService.getWeights(50);

        assertSoftly(assertions -> {
            assertions.assertThat(boxWeights).isNotNull();
            assertions.assertThat(boxWeights.getMinWeight().intValue()).isEqualTo(50);
            assertions.assertThat(boxWeights.getMaxWeight().intValue()).isEqualTo(55);
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/box-weights/immutable.xml")
    public void shouldRoundUpMinWeightToMax() {
        BoxWeights boxWeights = boxWeightsService.getWeights(30081);

        assertSoftly(assertions -> {
            assertions.assertThat(boxWeights).isNotNull();
            assertions.assertThat(boxWeights.getMinWeight().intValue()).isEqualTo(29999);
            assertions.assertThat(boxWeights.getMaxWeight().intValue()).isEqualTo(30000);
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/box-weights/immutable.xml")
    public void shouldRoundUpMinWeightToMaxException() {
        Assertions.assertThrows(BoxWeightExceededException.class, () -> boxWeightsService.getWeights(31000));
    }

    @Test
    @DatabaseSetup("/sorting/service/box-weights/immutable.xml")
    public void shouldRoundUpMaxWeight() {
        BoxWeights boxWeights = boxWeightsService.getWeights(30000);

        assertSoftly(assertions -> {
            assertions.assertThat(boxWeights).isNotNull();
            assertions.assertThat(boxWeights.getMinWeight().intValue()).isEqualTo(29999);
            assertions.assertThat(boxWeights.getMaxWeight().intValue()).isEqualTo(30000);
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/box-weights/immutable.xml")
    public void shouldRoundUpMaxWeightToMin() {
        BoxWeights boxWeights = boxWeightsService.getWeights(30);

        assertSoftly(assertions -> {
            assertions.assertThat(boxWeights).isNotNull();
            assertions.assertThat(boxWeights.getMinWeight().intValue()).isEqualTo(50);
            assertions.assertThat(boxWeights.getMaxWeight().intValue()).isEqualTo(51);
        });
    }
}
