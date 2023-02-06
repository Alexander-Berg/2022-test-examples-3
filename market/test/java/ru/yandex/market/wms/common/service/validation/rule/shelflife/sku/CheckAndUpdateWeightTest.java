package ru.yandex.market.wms.common.service.validation.rule.shelflife.sku;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.dto.SkuAndPackDTO;
import ru.yandex.market.wms.common.service.validation.rule.sku.CheckAndUpdateWeight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CheckAndUpdateWeightTest {

    private CheckAndUpdateWeight rule = new CheckAndUpdateWeight();

    @Test
    public void shouldSuccessValidate() {
        SkuAndPackDTO skuAndPackDTO = new SkuAndPackDTO();
        skuAndPackDTO.setStdgrosswgt(BigDecimal.valueOf(1.5));
        skuAndPackDTO.setStdnetwgt(BigDecimal.valueOf(1.4));
        skuAndPackDTO.setTare(BigDecimal.valueOf(0.1));

        rule.validate(skuAndPackDTO);

        assertEquals(BigDecimal.valueOf(1.5), skuAndPackDTO.getStdgrosswgt());
        assertEquals(BigDecimal.valueOf(1.4), skuAndPackDTO.getStdnetwgt());
        assertEquals(BigDecimal.valueOf(0.1), skuAndPackDTO.getTare());
    }

    @Test
    public void shouldSuccessValidateIfGrossIsNull() {
        SkuAndPackDTO skuAndPackDTO = new SkuAndPackDTO();
        skuAndPackDTO.setStdgrosswgt(null);
        skuAndPackDTO.setStdnetwgt(BigDecimal.valueOf(1.576));

        rule.validate(skuAndPackDTO);

        assertEquals(BigDecimal.valueOf(1.576), skuAndPackDTO.getStdgrosswgt());
        assertEquals(BigDecimal.valueOf(1.576), skuAndPackDTO.getStdnetwgt());
        assertNull(skuAndPackDTO.getTare());
    }

    @Test
    public void shouldSuccessValidateIfNettGreaterThanGross() {
        SkuAndPackDTO skuAndPackDTO = new SkuAndPackDTO();
        skuAndPackDTO.setStdgrosswgt(BigDecimal.valueOf(1.576));
        skuAndPackDTO.setStdnetwgt(BigDecimal.valueOf(1.676));

        rule.validate(skuAndPackDTO);

        assertEquals(BigDecimal.valueOf(1.576), skuAndPackDTO.getStdgrosswgt());
        assertEquals(BigDecimal.valueOf(1.576), skuAndPackDTO.getStdnetwgt());
        assertEquals(BigDecimal.ZERO.setScale(3, RoundingMode.DOWN), skuAndPackDTO.getTare());
    }

    @Test
    public void shouldNoSuccessValidateIfGrossIsNegative() {
        SkuAndPackDTO skuAndPackDTO = new SkuAndPackDTO();
        skuAndPackDTO.setStdgrosswgt(BigDecimal.valueOf(-0.5));
        skuAndPackDTO.setStdnetwgt(BigDecimal.valueOf(1.576));

        assertThrows(Exception.class, () -> rule.validate(skuAndPackDTO));
    }
}
