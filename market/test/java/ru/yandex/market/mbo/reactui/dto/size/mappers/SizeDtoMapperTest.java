package ru.yandex.market.mbo.reactui.dto.size.mappers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.size.model.Size;
import ru.yandex.market.mbo.db.size.model.SizeChartMeasure;
import ru.yandex.market.mbo.reactui.dto.size.SizeChartMeasureDto;
import ru.yandex.market.mbo.reactui.dto.size.SizeDto;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SizeDtoMapperTest {

    private static final String SIZE_NAME = "XS";
    private static final Long ID = 222L;
    private static final Long SIZE_OPTION_ID = 32L;
    private static final List<Long> CATEGORY_IDS = List.of(1L, 2L);
    private static final List<SizeChartMeasure> SIZE_MEASURES = List.of(
        new SizeChartMeasure()
    );
    private static final List<SizeChartMeasureDto> SIZE_MEASURES_DTO = List.of(
        new SizeChartMeasureDto()
    );

    @Spy
    private final SizeChartMeasureDtoMapper sizeChartMeasureDtoMapper =
        Mappers.getMapper(SizeChartMeasureDtoMapper.class);

    @InjectMocks
    private final SizeDtoMapper sizeDtoMapper = Mappers.getMapper(SizeDtoMapper.class);

    @Test
    public void convertSizeToSizeDto() {
        Size size = new Size();

        size.setSizeName(SIZE_NAME);
        size.setId(ID);
        size.setSizeOptionId(SIZE_OPTION_ID);
        size.setCategoryIds(CATEGORY_IDS);
        size.setMeasures(SIZE_MEASURES);

        SizeDto result = sizeDtoMapper.toDto(size);

        assertEquals(SIZE_NAME, result.getSizeName());
        assertEquals(SIZE_OPTION_ID, result.getSizeOptionId());
        assertEquals(CATEGORY_IDS, result.getCategoryIds());
        assertEquals(ID, result.getId());


        verify(sizeChartMeasureDtoMapper, times(1)).toDto(SIZE_MEASURES.get(0));
    }

    @Test
    public void convertSizeToModel() {
        SizeDto dto = new SizeDto();

        dto.setSizeName(SIZE_NAME);
        dto.setId(ID);
        dto.setSizeOptionId(SIZE_OPTION_ID);
        dto.setCategoryIds(CATEGORY_IDS);
        dto.setMeasures(SIZE_MEASURES_DTO);

        Size result = sizeDtoMapper.toModel(dto);

        assertEquals(SIZE_NAME, result.getSizeName());
        assertEquals(SIZE_OPTION_ID, result.getSizeOptionId());
        assertEquals(CATEGORY_IDS, result.getCategoryIds());
        assertEquals(ID, result.getId());
        assertEquals(ID, result.getMeasures().get(0).getSizeId());

        verify(sizeChartMeasureDtoMapper, times(1)).toModel(SIZE_MEASURES_DTO.get(0));
    }

}
