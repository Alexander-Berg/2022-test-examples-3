package ru.yandex.market.wms.api.converter;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.iris.client.model.entity.Korobyte;
import ru.yandex.market.logistics.iris.client.model.entity.MeasurementDimensions;
import ru.yandex.market.logistics.iris.client.model.entity.UnitId;
import ru.yandex.market.wms.common.spring.converter.DimensionsDTOConverter;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.PushReferenceItemsResultDto;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DimensionsDTOConverterTest {

    @Test
    public void shouldSuccessConvert() {
        MeasurementDimensions measurementDimensions =
                DimensionsDTOConverter.convert(createDimensionsDTO());

        assertEquals(measurementDimensions.getUnitId(), new UnitId(null, 4334L, "Sku1"));
        assertEquals(measurementDimensions.getKorobyte(), getExpectedKorobyte());
    }

    private PushReferenceItemsResultDto createDimensionsDTO() {
        return PushReferenceItemsResultDto.builder()
                .korobyte(createActualKorobyte())
                .unitId(new ru.yandex.market.logistic.api.model.fulfillment.UnitId(null, 4334L, "Sku1"))
                .build();
    }

    private ru.yandex.market.logistic.api.model.fulfillment.Korobyte createActualKorobyte() {
        return new ru.yandex.market.logistic.api.model.fulfillment.Korobyte.KorobyteBuiler(
                10, 20, 30, BigDecimal.valueOf(1220)
        ).build();
    }

    private Korobyte getExpectedKorobyte() {
        return new Korobyte.KorobyteBuilder()
                .setWidth(BigDecimal.valueOf(10))
                .setHeight(BigDecimal.valueOf(20))
                .setLength(BigDecimal.valueOf(30))
                .setWeightGross(BigDecimal.valueOf(1220))
                .build();
    }

}
