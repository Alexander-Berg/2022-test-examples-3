package ru.yandex.market.mbo.reactui.dto.size.mappers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.size.model.Size;
import ru.yandex.market.mbo.db.size.model.SizeChart;
import ru.yandex.market.mbo.reactui.dto.size.SizeChartDto;
import ru.yandex.market.mbo.reactui.dto.size.SizeDto;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SizeChartDtoMapperTest {

    private static final String CHART_NAME = "ADIDAS";
    private static final Long CHART_ID = 445L;
    private static final Long VENDOR_ID = 222L;
    private static final Long OPTION_ID = 921L;
    private static final Long ID = 98L;
    private static final List<Size> SIZES = List.of(new Size());
    private static final List<SizeDto> SIZES_DTO = List.of(new SizeDto());

    @Mock
    private SizeDtoMapper sizeDtoMapper;

    @InjectMocks
    private final SizeChartDtoMapper sizeChartDtoMapper = Mappers.getMapper(SizeChartDtoMapper.class);

    @Test
    public void convertToSizeChartDtoTest() {

        SizeChart sizeChart = new SizeChart();

        sizeChart.setName(CHART_NAME);
        sizeChart.setId(CHART_ID);
        sizeChart.setVendorId(VENDOR_ID);
        sizeChart.setOptionId(OPTION_ID);
        sizeChart.setSizes(SIZES);
        sizeChart.setId(ID);

        SizeChartDto result = sizeChartDtoMapper.toDto(sizeChart);

        assertEquals(CHART_NAME, result.getName());
        assertEquals(VENDOR_ID, result.getVendorId());
        assertEquals(OPTION_ID, result.getOptionId());
        assertEquals(ID, result.getId());

        verify(sizeDtoMapper, times(1)).toDto(SIZES.get(0));
    }

    @Test
    public void convertToSizeChartModelTest() {

        SizeChartDto dto = new SizeChartDto();

        dto.setName(CHART_NAME);
        dto.setId(CHART_ID);
        dto.setVendorId(VENDOR_ID);
        dto.setOptionId(OPTION_ID);
        dto.setSizes(SIZES_DTO);
        dto.setId(ID);

        SizeChart result = sizeChartDtoMapper.toModel(dto);

        assertEquals(CHART_NAME, result.getName());
        assertEquals(VENDOR_ID, result.getVendorId());
        assertEquals(OPTION_ID, result.getOptionId());
        assertEquals(ID, result.getId());

        verify(sizeDtoMapper, times(1)).toModel(SIZES_DTO.get(0));
    }

}
