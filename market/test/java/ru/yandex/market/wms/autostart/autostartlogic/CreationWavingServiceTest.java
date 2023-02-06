package ru.yandex.market.wms.autostart.autostartlogic;

import java.math.BigDecimal;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.service.CreationWaveService;
import ru.yandex.market.wms.common.pojo.Dimensions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreationWavingServiceTest {

    @Test
    public void orderIsOversizeFits() {
        Dimensions dimensions =
                new Dimensions.DimensionsBuilder().weight(BigDecimal.ZERO).length(BigDecimal.valueOf(10.0))
                        .height(BigDecimal.valueOf(10.0)).width(BigDecimal.valueOf(10.0)).build();
        boolean isOversize = CreationWaveService
                .orderIsOversize(Lists.newArrayList(dimensions), BigDecimal.valueOf(11.0), BigDecimal.valueOf(18));
        assertFalse(isOversize);
    }

    @Test
    public void orderIsOversizeDoesntFitBiggerSize() {
        Dimensions dimensions =
                new Dimensions.DimensionsBuilder().weight(BigDecimal.ZERO).length(BigDecimal.valueOf(10.0))
                        .height(BigDecimal.valueOf(10.0)).width(BigDecimal.valueOf(10.0)).build();
        boolean isOversize = CreationWaveService
                .orderIsOversize(Lists.newArrayList(dimensions), BigDecimal.valueOf(100), BigDecimal.valueOf(16));
        assertTrue(isOversize);
    }

    @Test
    public void orderIsOversizeDoesntFitBiggerWeight() {
        Dimensions dimensions =
                new Dimensions.DimensionsBuilder().weight(BigDecimal.valueOf(11.0)).length(BigDecimal.valueOf(10.0))
                        .height(BigDecimal.valueOf(10.0)).width(BigDecimal.valueOf(10.0)).build();
        boolean isOversize = CreationWaveService
                .orderIsOversize(Lists.newArrayList(dimensions), BigDecimal.valueOf(9.0), BigDecimal.valueOf(1000));
        assertTrue(isOversize);
    }
}
