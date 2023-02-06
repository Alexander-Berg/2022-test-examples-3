package ru.yandex.market.wms.common.converter;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.PackDTO;
import ru.yandex.market.wms.common.model.dto.SkuDTO;
import ru.yandex.market.wms.common.pojo.Dimensions;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class DimensionsConverterTest {

    @Test
    public void shouldSuccessConvert() {
        SkuDTO skuDTO = new SkuDTO();
        skuDTO.setStdcube(BigDecimal.valueOf(6000));
        skuDTO.setStdgrosswgt(BigDecimal.valueOf(1.2));

        PackDTO packDTO = new PackDTO();
        packDTO.setWidthuom3(10.0);
        packDTO.setLengthuom3(20.0);
        packDTO.setHeightuom3(30.0);

        Dimensions actualDimensions = DimensionsConverter.convert(skuDTO, packDTO);

        assertSoftly(assertions -> {
            assertions.assertThat(actualDimensions).isNotNull();
            assertions.assertThat(actualDimensions.getWeight()).isEqualTo(BigDecimal.valueOf(1.2));
            assertions.assertThat(actualDimensions.getCube()).isEqualTo(BigDecimal.valueOf(6000));
            assertions.assertThat(actualDimensions.getWidth()).isEqualTo(BigDecimal.valueOf(10.0));
            assertions.assertThat(actualDimensions.getLength()).isEqualTo(BigDecimal.valueOf(20.0));
            assertions.assertThat(actualDimensions.getHeight()).isEqualTo(BigDecimal.valueOf(30.0));
        });
    }

    @Test
    public void shouldNoSuccessConvert() {
        Dimensions actualDimensions = DimensionsConverter.convert(null, null);

        assertSoftly(assertions -> {
            assertions.assertThat(actualDimensions).isNotNull();
            assertions.assertThat(actualDimensions.getWeight()).isNull();
            assertions.assertThat(actualDimensions.getCube()).isNull();
            assertions.assertThat(actualDimensions.getWidth()).isNull();
            assertions.assertThat(actualDimensions.getLength()).isNull();
            assertions.assertThat(actualDimensions.getHeight()).isNull();
        });
    }
}
