package ru.yandex.market.wms.common.service.validation.dimensions;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.dto.PackDTO;
import ru.yandex.market.wms.common.model.dto.SkuAndPackDTO;
import ru.yandex.market.wms.common.model.dto.SkuDTO;
import ru.yandex.market.wms.common.model.enums.DimensionsValidationCode;
import ru.yandex.market.wms.common.model.enums.DimensionsValidationLevel;
import ru.yandex.market.wms.common.pojo.Dimensions;
import ru.yandex.market.wms.common.service.DbConfigService;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DimensionsValidationProcessorTest {

    @Mock
    private DbConfigService dbConfigService;

    private DimensionsValidationProcessor validationProcessor;

    @BeforeEach
    void initialize() {
        validationProcessor = new DimensionsValidationProcessor(dbConfigService);
    }

    @Test
    public void shouldSuccessValidateIfDimensionsValidAndAllowedLevelWarn() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;
        final Dimensions dimensions =
                createDimensions(of(10.0), of(20.0), of(30.0), of(6000.0), of(1.2));

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void shouldSuccessValidateIfDimensionsNotValidAndAllowedLevelWarn() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;
        final Dimensions dimensions =
                createDimensions(of(10.0), of(10.0), of(10.0), of(6000.0), of(1.2));

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void shouldNoSuccessValidateIfDimensionsNotValidAndAllowedLevelOk() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.OK;
        final Dimensions dimensions =
                createDimensions(of(10.0), of(10.0), of(10.0), of(6000.0), of(1.2));

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertEquals(1, actualResult.size());
        assertSoftly(assertions -> {
            DimensionsValidationResult result = actualResult.get(0);
            assertions.assertThat(result.getCode()).isEqualTo(DimensionsValidationCode.ALL_DIMENSIONS_SAME);
            assertions.assertThat(result.getLevel()).isEqualTo(DimensionsValidationLevel.WARN);
        });
    }

    @Test
    public void shouldNoSuccessValidateIfDimensionsLowDensityAndAllowedLevelWarn() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;
        final Dimensions dimensions =
                createDimensions(of(1.0), of(2.0), of(3.0), of(6.0), of(0.000048));

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertEquals(1, actualResult.size());
        assertSoftly(assertions -> {
            DimensionsValidationResult result = actualResult.get(0);
            assertions.assertThat(result.getCode()).isEqualTo(DimensionsValidationCode.LOW_DENSITY);
            assertions.assertThat(result.getLevel()).isEqualTo(DimensionsValidationLevel.CRIT);
        });
    }

    @Test
    public void shouldNoSuccessValidateIfDimensionsHighDensityAndAllowedLevelWarn() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;
        final Dimensions dimensions =
                createDimensions(of(1.0), of(2.0), of(3.0), of(6.0), of(0.0486));

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertEquals(1, actualResult.size());
        assertSoftly(assertions -> {
            DimensionsValidationResult result = actualResult.get(0);
            assertions.assertThat(result.getCode()).isEqualTo(DimensionsValidationCode.HIGH_DENSITY);
            assertions.assertThat(result.getLevel()).isEqualTo(DimensionsValidationLevel.CRIT);
        });
    }

    @Test
    public void shouldNoSuccessValidateIfDimensionsIsNullAndAllowedLevelWarn() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;
        final Dimensions dimensions =
                createDimensions(null, of(20.0), of(30.0), of(6000.0), null);

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertEquals(1, actualResult.size());
        assertSoftly(assertions -> {
            DimensionsValidationResult result = actualResult.get(0);
            assertions.assertThat(result.getCode()).isEqualTo(DimensionsValidationCode.DIMENSION_NOT_PRESENTED);
            assertions.assertThat(result.getLevel()).isEqualTo(DimensionsValidationLevel.CRIT);
        });
    }

    @Test
    public void shouldNoSuccessValidateIfDimensionsIsZeroAndAllowedLevelWarn() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;
        final Dimensions dimensions =
                createDimensions(of(0.0), of(20.0), of(30.0), of(0.0), of(1.2));

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertEquals(1, actualResult.size());
        assertSoftly(assertions -> {
            DimensionsValidationResult result = actualResult.get(0);
            assertions.assertThat(result.getCode()).isEqualTo(DimensionsValidationCode.DIMENSION_NOT_PRESENTED);
            assertions.assertThat(result.getLevel()).isEqualTo(DimensionsValidationLevel.CRIT);
        });
    }

    @Test
    public void shouldSuccessValidateIfDimensionsValidAndAllowedLevelOk() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.OK;
        final Dimensions dimensions =
                createDimensions(of(10.0), of(20.0), of(30.0), of(6000.0), of(1.2));

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void shouldSuccessValidateIfDimensionsIsKGTAndAllowedLevelWarn() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;
        final Dimensions dimensions =
                createDimensions(of(100.0), of(20.0), of(30.0), of(6500.0), of(30.0));

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void shouldSuccessValidateIfValidationDisabled() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(true);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.OK;
        final Dimensions dimensions =
                createDimensions(null, null, of(30.0), of(6500.0), null);

        List<DimensionsValidationResult> actualResult =
                validationProcessor.validateDimensions(dimensions, allowedLevel);

        assertTrue(actualResult.isEmpty());
    }

    @Test
    public void shouldMeasureIfDimensionsNull() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;

        assertTrue(validationProcessor.shouldMeasure(null, null, allowedLevel));
    }

    @Test
    public void shouldNotMeasureIfDimensionsNotNull() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;
        final SkuDTO skuDTO = new SkuDTO();
        skuDTO.setStdcube(BigDecimal.valueOf(6000));
        skuDTO.setStdgrosswgt(BigDecimal.valueOf(1.2));

        final PackDTO packDTO = new PackDTO();
        packDTO.setWidthuom3(10.0);
        packDTO.setLengthuom3(20.0);
        packDTO.setHeightuom3(30.0);

        assertFalse(validationProcessor.shouldMeasure(skuDTO, packDTO, allowedLevel));
    }

    @Test
    public void shouldSuccessValidateSkuAndPackDTO() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        when(dbConfigService.getConfigAsBoolean(eq("YM_ALLOW_SET_MEASUREMENTS"), anyBoolean())).thenReturn(false);
        when(dbConfigService.getConfigAsBoolean(eq("YM_ENABLE_MEASURE_DIMENSIONS"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;

        List<SkuAndPackDTO> skuAndPackDTOS = createSkuAndPackDTO();

        validationProcessor.validateSkuAndPackDTODimensions(skuAndPackDTOS,
                allowedLevel,
                "101",
                "test");

        assertSoftly(assertions -> {
            SkuAndPackDTO firstSku = skuAndPackDTOS.get(0);
            assertions.assertThat(firstSku.getPack()).isNotNull();
            assertions.assertThat(firstSku.getPackkey()).isEqualTo("P_R001");
            assertions.assertThat(firstSku.getNeedMeasurement()).isNull();

            SkuAndPackDTO secondSku = skuAndPackDTOS.get(1);
            assertions.assertThat(secondSku.getPack()).isNotNull();
            assertions.assertThat(secondSku.getPackkey()).isEqualTo("P_R002");
            assertions.assertThat(secondSku.getNeedMeasurement()).isNull();

            SkuAndPackDTO thirdSku = skuAndPackDTOS.get(2);
            assertions.assertThat(thirdSku.getPack()).isNull();
            assertions.assertThat(thirdSku.getPackkey()).isEqualTo("STD");
            assertions.assertThat(thirdSku.getNeedMeasurement()).isNull();
        });
    }

    @Test
    public void shouldNoSuccessValidateSkuAndPackDTOIfEnabledChangingFromApi() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        when(dbConfigService.getConfigAsBoolean(eq("YM_ALLOW_SET_MEASUREMENTS"), anyBoolean())).thenReturn(true);
        when(dbConfigService.getConfigAsBoolean(eq("YM_ENABLE_MEASURE_DIMENSIONS"), anyBoolean())).thenReturn(true);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;

        List<SkuAndPackDTO> skuAndPackDTOS = createSkuAndPackDTO();

        validationProcessor.validateSkuAndPackDTODimensions(skuAndPackDTOS,
                allowedLevel,
                "101",
                "test");

        assertSoftly(assertions -> {
            SkuAndPackDTO firstSku = skuAndPackDTOS.get(0);
            assertions.assertThat(firstSku.getPack()).isNotNull();
            assertions.assertThat(firstSku.getPackkey()).isEqualTo("P_R001");
            assertions.assertThat(firstSku.getNeedMeasurement()).isNull();

            SkuAndPackDTO secondSku = skuAndPackDTOS.get(1);
            assertions.assertThat(secondSku.getPack()).isNull();
            assertions.assertThat(secondSku.getPackkey()).isNull();
            assertions.assertThat(secondSku.getNeedMeasurement()).isEqualTo(true);

            SkuAndPackDTO thirdSku = skuAndPackDTOS.get(2);
            assertions.assertThat(thirdSku.getPack()).isNull();
            assertions.assertThat(thirdSku.getPackkey()).isNull();
            assertions.assertThat(thirdSku.getNeedMeasurement()).isEqualTo(true);
        });
    }

    @Test
    public void shouldNoSuccessValidateSkuAndPackDTOIfDisabledChangingFromApi() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        when(dbConfigService.getConfigAsBoolean(eq("YM_ALLOW_SET_MEASUREMENTS"), anyBoolean())).thenReturn(true);
        when(dbConfigService.getConfigAsBoolean(eq("YM_ENABLE_MEASURE_DIMENSIONS"), anyBoolean())).thenReturn(false);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;

        List<SkuAndPackDTO> skuAndPackDTOS = createSkuAndPackDTO();

        validationProcessor.validateSkuAndPackDTODimensions(skuAndPackDTOS,
                allowedLevel,
                "101",
                "test");

        assertSoftly(assertions -> {
            SkuAndPackDTO firstSku = skuAndPackDTOS.get(0);
            assertions.assertThat(firstSku.getPack()).isNotNull();
            assertions.assertThat(firstSku.getPackkey()).isEqualTo("P_R001");
            assertions.assertThat(firstSku.getNeedMeasurement()).isNull();

            SkuAndPackDTO secondSku = skuAndPackDTOS.get(1);
            assertions.assertThat(secondSku.getPack()).isNull();
            assertions.assertThat(secondSku.getPackkey()).isNull();
            assertions.assertThat(secondSku.getNeedMeasurement()).isNull();

            SkuAndPackDTO thirdSku = skuAndPackDTOS.get(2);
            assertions.assertThat(thirdSku.getPack()).isNull();
            assertions.assertThat(thirdSku.getPackkey()).isNull();
            assertions.assertThat(thirdSku.getNeedMeasurement()).isNull();
        });
    }

    @Test
    public void shouldNoSuccessValidateSkuAndPackDTOIfDisabledChangingPackFromApi() {
        when(dbConfigService.getConfigAsBoolean(eq("YM_DISABLE_DIM_VALIDATION"), anyBoolean())).thenReturn(false);
        when(dbConfigService.getConfigAsBoolean(eq("YM_ALLOW_SET_MEASUREMENTS"), anyBoolean())).thenReturn(false);
        when(dbConfigService.getConfigAsBoolean(eq("YM_ENABLE_MEASURE_DIMENSIONS"), anyBoolean())).thenReturn(true);
        final DimensionsValidationLevel allowedLevel = DimensionsValidationLevel.WARN;

        List<SkuAndPackDTO> skuAndPackDTOS = createSkuAndPackDTO();

        validationProcessor.validateSkuAndPackDTODimensions(skuAndPackDTOS,
                allowedLevel,
                "101",
                "test");

        assertSoftly(assertions -> {
            SkuAndPackDTO firstSku = skuAndPackDTOS.get(0);
            assertions.assertThat(firstSku.getPack()).isNotNull();
            assertions.assertThat(firstSku.getPackkey()).isEqualTo("P_R001");
            assertions.assertThat(firstSku.getNeedMeasurement()).isNull();

            SkuAndPackDTO secondSku = skuAndPackDTOS.get(1);
            assertions.assertThat(secondSku.getPack()).isNotNull();
            assertions.assertThat(secondSku.getPackkey()).isEqualTo("P_R002");
            assertions.assertThat(secondSku.getNeedMeasurement()).isEqualTo(true);

            SkuAndPackDTO thirdSku = skuAndPackDTOS.get(2);
            assertions.assertThat(thirdSku.getPack()).isNull();
            assertions.assertThat(thirdSku.getPackkey()).isEqualTo("STD");
            assertions.assertThat(thirdSku.getNeedMeasurement()).isEqualTo(true);
        });
    }

    private Dimensions createDimensions(BigDecimal width,
                                        BigDecimal height,
                                        BigDecimal length,
                                        BigDecimal cube,
                                        BigDecimal weight) {
        return new Dimensions.DimensionsBuilder()
                .width(width)
                .height(height)
                .length(length)
                .cube(cube)
                .weight(weight)
                .build();
    }

    private List<SkuAndPackDTO> createSkuAndPackDTO() {
        // first
        PackDTO firstPackDTO = new PackDTO();
        firstPackDTO.setWidthuom3(10.0);
        firstPackDTO.setHeightuom3(20.0);
        firstPackDTO.setLengthuom3(30.0);

        SkuAndPackDTO firstSkuAndPackDTO = new SkuAndPackDTO();
        firstSkuAndPackDTO.setStdgrosswgt(BigDecimal.valueOf(1.2));
        firstSkuAndPackDTO.setStdcube(BigDecimal.valueOf(6000.0));
        firstSkuAndPackDTO.setPackkey("P_R001");
        firstSkuAndPackDTO.setPack(firstPackDTO);

        // second
        PackDTO secondPackDTO = new PackDTO();
        secondPackDTO.setWidthuom3(1.0);
        secondPackDTO.setHeightuom3(2.0);
        secondPackDTO.setLengthuom3(3.0);

        SkuAndPackDTO secondSkuAndPackDTO = new SkuAndPackDTO();
        secondSkuAndPackDTO.setStdgrosswgt(BigDecimal.valueOf(0.0486));
        secondSkuAndPackDTO.setStdcube(BigDecimal.valueOf(6.0));
        secondSkuAndPackDTO.setPackkey("P_R002");
        secondSkuAndPackDTO.setPack(secondPackDTO);

        // third
        SkuAndPackDTO thirdSkuAndPackDTO = new SkuAndPackDTO();
        thirdSkuAndPackDTO.setStdgrosswgt(BigDecimal.valueOf(0));
        thirdSkuAndPackDTO.setStdcube(BigDecimal.valueOf(0.0));
        thirdSkuAndPackDTO.setPackkey("STD");
        thirdSkuAndPackDTO.setPack(null);

        return ImmutableList.of(
                firstSkuAndPackDTO,
                secondSkuAndPackDTO,
                thirdSkuAndPackDTO
        );
    }

    private BigDecimal of(Double value) {
        return BigDecimal.valueOf(value);
    }
}
