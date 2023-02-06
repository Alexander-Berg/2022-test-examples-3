package ru.yandex.market.mbo.reactui.dto.size.mappers;

import org.junit.Test;
import org.mapstruct.factory.Mappers;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.reactui.dto.size.SizeMeasureDto;

import static org.junit.Assert.assertEquals;

public class SizeMeasureMapperTest {

    SizeMeasureDtoMapper sizeMeasureDtoMapper = Mappers.getMapper(SizeMeasureDtoMapper.class);

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void toDtoTest() {
        ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureDto model =
            new ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureDto();

        GLMeasure measure = new GLMeasure();
        measure.setId(1L);
        measure.setValueParamId(2L);
        measure.setName("размеры: обхват груди");
        model.setMeasure(measure);

        SizeMeasureDto result = sizeMeasureDtoMapper.apply(model);

        assertEquals(Long.valueOf(measure.getId()), result.getId());
        assertEquals(Long.valueOf(measure.getValueParamId()), result.getParamId());
        assertEquals("обхват груди", result.getName());
    }

}
