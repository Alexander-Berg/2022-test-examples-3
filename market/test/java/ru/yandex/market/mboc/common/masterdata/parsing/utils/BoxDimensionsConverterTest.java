package ru.yandex.market.mboc.common.masterdata.parsing.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.model.BoxDimensionsInUm;

/**
 * @author dmserebr
 * @date 18/10/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BoxDimensionsConverterTest {
    @Test
    public void convertBoxDimensions() {
        Assertions.assertThat(BoxDimensionsConverter.boxDimensionsToString(null)).isNull();
        Assertions.assertThat(BoxDimensionsConverter.boxDimensionsToString(
            new BoxDimensionsInUm(null, null, null))).isNull();
        Assertions.assertThat(BoxDimensionsConverter.boxDimensionsToString(
            new BoxDimensionsInUm(10000L, 20000L, 30000L)))
            .isEqualTo("1/2/3");
        Assertions.assertThat(BoxDimensionsConverter.boxDimensionsToString(
            new BoxDimensionsInUm(10000000L, 20000000L, 30000000L)))
            .isEqualTo("1000/2000/3000");
        Assertions.assertThat(BoxDimensionsConverter.boxDimensionsToString(
            new BoxDimensionsInUm(100L, 200L, 300L)))
            .isEqualTo("0.01/0.02/0.03");
    }

    @Test
    public void convertBoxWeight() {
        Assertions.assertThat(BoxDimensionsConverter.weightToString(null)).isNull();
        Assertions.assertThat(BoxDimensionsConverter.weightToString(10000000L)).isEqualTo("10");
        Assertions.assertThat(BoxDimensionsConverter.weightToString(10000000000L)).isEqualTo("10000");
        Assertions.assertThat(BoxDimensionsConverter.weightToString(20L)).isEqualTo("0.00002");
    }
}
