package ru.yandex.market.mbo.reactui.dto.size.mappers;

import org.junit.Test;
import org.mapstruct.factory.Mappers;
import ru.yandex.market.mbo.db.size.model.SizeChartMeasure;
import ru.yandex.market.mbo.reactui.dto.size.SizeChartMeasureDto;

import static org.junit.Assert.assertEquals;

public class SizeChartMeasureDtoMapperTest {

    private static final Long MEASURE_ID = 87L;
    private static final Long ID = 21L;
    private static final Long OPTION_ID = 49L;
    private static final Long SIZE_ID = 12L;
    private static final Integer MAX_VALUE = 50;
    private static final Integer MIN_VALUE = 40;
    private static final Boolean USED_IN_FILTER = true;
    private static final Boolean CONVERTED_TO_SIZE = false;
    private static final Boolean SET_NAME_BY_USER = true;

    private final SizeChartMeasureDtoMapper sizeChartMeasureDtoMapper =
        Mappers.getMapper(SizeChartMeasureDtoMapper.class);

    @Test
    public void convertSizeChartMeasureDto() {
        SizeChartMeasure sizeChartMeasure = new SizeChartMeasure();

        sizeChartMeasure.setMeasureId(MEASURE_ID);
        sizeChartMeasure.setId(ID);
        sizeChartMeasure.setOptionId(OPTION_ID);
        sizeChartMeasure.setUsedInFilter(USED_IN_FILTER);
        sizeChartMeasure.setConvertedToSize(CONVERTED_TO_SIZE);
        sizeChartMeasure.setMaxValue(MAX_VALUE);
        sizeChartMeasure.setMinValue(MIN_VALUE);
        sizeChartMeasure.setSetNameByUser(SET_NAME_BY_USER);
        sizeChartMeasure.setSizeId(SIZE_ID);

        SizeChartMeasureDto result = sizeChartMeasureDtoMapper.toDto(sizeChartMeasure);

        assertEquals(MEASURE_ID, result.getMeasureId());
        assertEquals(ID, result.getId());
        assertEquals(OPTION_ID, result.getOptionId());
        assertEquals(MIN_VALUE, result.getMinValue());
        assertEquals(USED_IN_FILTER, result.getUsedInFilter());
        assertEquals(CONVERTED_TO_SIZE, result.getConvertedToSize());
        assertEquals(SET_NAME_BY_USER, result.getSetNameByUser());
        assertEquals(SIZE_ID, result.getSizeId());
    }

    @Test
    public void convertSizeChartMeasureModel() {
        SizeChartMeasureDto dto = new SizeChartMeasureDto();

        dto.setMeasureId(MEASURE_ID);
        dto.setId(ID);
        dto.setOptionId(OPTION_ID);
        dto.setUsedInFilter(USED_IN_FILTER);
        dto.setConvertedToSize(CONVERTED_TO_SIZE);
        dto.setMaxValue(MAX_VALUE);
        dto.setMinValue(MIN_VALUE);
        dto.setSetNameByUser(SET_NAME_BY_USER);
        dto.setSizeId(SIZE_ID);

        SizeChartMeasure result = sizeChartMeasureDtoMapper.toModel(dto);

        assertEquals(MEASURE_ID, result.getMeasureId());
        assertEquals(ID, result.getId());
        assertEquals(OPTION_ID, result.getOptionId());
        assertEquals(MIN_VALUE, result.getMinValue());
        assertEquals(USED_IN_FILTER, result.getUsedInFilter());
        assertEquals(CONVERTED_TO_SIZE, result.getConvertedToSize());
        assertEquals(SET_NAME_BY_USER, result.getSetNameByUser());
        assertEquals(SIZE_ID, result.getSizeId());
    }

}
