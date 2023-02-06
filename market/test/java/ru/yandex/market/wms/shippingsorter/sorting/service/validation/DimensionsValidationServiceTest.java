package ru.yandex.market.wms.shippingsorter.sorting.service.validation;

import java.math.BigDecimal;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.model.ValidateCode;
import ru.yandex.market.wms.shippingsorter.sorting.model.ValidateResult;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.wms.shippingsorter.sorting.service.validation.DimensionsValidationService.BOX_WEIGHT_PARAMETER_NAME;
import static ru.yandex.market.wms.shippingsorter.sorting.service.validation.DimensionsValidationService.MAX_HEIGHT_PARAMETER_NAME;
import static ru.yandex.market.wms.shippingsorter.sorting.service.validation.DimensionsValidationService.MAX_LENGTH_PARAMETER_NAME;
import static ru.yandex.market.wms.shippingsorter.sorting.service.validation.DimensionsValidationService.MAX_WEIGHT_PARAMETER_NAME;
import static ru.yandex.market.wms.shippingsorter.sorting.service.validation.DimensionsValidationService.MAX_WIDTH_PARAMETER_NAME;
import static ru.yandex.market.wms.shippingsorter.sorting.service.validation.DimensionsValidationService.MIN_WEIGHT_PARAMETER_NAME;

@Import(ShippingSorterSecurityTestConfiguration.class)
public class DimensionsValidationServiceTest extends IntegrationTest {

    @Autowired
    private DbConfigService configService;

    private DimensionsValidationService validationService;

    @BeforeEach
    public void setup() {
        super.setup();
        validationService = new DimensionsValidationService(configService);
    }

    @Test
    @DatabaseSetup("/sorting/service/validation-dimensions/immutable.xml")
    public void testValidateOk() {
        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(450)
                .boxWidth(new BigDecimal("12.0"))
                .boxHeight(new BigDecimal("6.0"))
                .boxLength(new BigDecimal("18.0"))
                .carrierName("DPD")
                .carrierCode("123456")
                .operationDayId(18262L)
                .build();

        ValidateResult result = validationService.validateDimensions("1", boxInfo);

        assertSoftly(assertions -> {
            assertions.assertThat(result.isFailed()).isFalse();
            assertions.assertThat(result.code()).isEqualTo(ValidateCode.OK);
            assertions.assertThat(result.args()).isEqualTo(null);
            assertions.assertThat(result.getMessage()).isEqualTo("Validation succeeded.");
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/validation-dimensions/immutable.xml")
    public void testMinWeightExceeded() {
        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(45)
                .boxWidth(new BigDecimal("12.0"))
                .boxHeight(new BigDecimal("6.0"))
                .boxLength(new BigDecimal("18.0"))
                .carrierName("DPD")
                .carrierCode("123456")
                .operationDayId(18262L)
                .build();

        ValidateResult result = validationService.validateDimensions("1", boxInfo);

        assertSoftly(assertions -> {
            assertions.assertThat(result.isFailed()).isTrue();
            assertions.assertThat(result.code()).isEqualTo(ValidateCode.WEIGHT_LESS_THAN_POSSIBLE);
            assertions.assertThat(result.args()).isEqualTo(Map.of(
                    BOX_WEIGHT_PARAMETER_NAME, BigDecimal.valueOf(45),
                    MIN_WEIGHT_PARAMETER_NAME, BigDecimal.valueOf(50.0)
            ));
            assertions.assertThat(result.getMessage()).isEqualTo("Вес посылки 45 меньше минимального веса 50.0");
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/validation-dimensions/immutable.xml")
    public void testMaxWeightExceeded() {
        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(30005)
                .boxWidth(new BigDecimal("12.0"))
                .boxHeight(new BigDecimal("6.0"))
                .boxLength(new BigDecimal("18.0"))
                .carrierName("DPD")
                .carrierCode("123456")
                .operationDayId(18262L)
                .build();

        ValidateResult result = validationService.validateDimensions("1", boxInfo);

        assertSoftly(assertions -> {
            assertions.assertThat(result.isFailed()).isTrue();
            assertions.assertThat(result.code()).isEqualTo(ValidateCode.WEIGHT_GREATER_THAN_POSSIBLE);
            assertions.assertThat(result.args()).isEqualTo(Map.of(MAX_WEIGHT_PARAMETER_NAME,
                    BigDecimal.valueOf(30000.0)));
            assertions.assertThat(result.getMessage()).isEqualTo("Превышен максимальный вес - 30000.0");
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/validation-dimensions/immutable.xml")
    public void testMaxWidthExceeded() {
        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(450)
                .boxWidth(new BigDecimal("60.2"))
                .boxHeight(new BigDecimal("6.0"))
                .boxLength(new BigDecimal("18.0"))
                .carrierName("DPD")
                .carrierCode("123456")
                .operationDayId(18262L)
                .build();

        ValidateResult result = validationService.validateDimensions("1", boxInfo);

        assertSoftly(assertions -> {
            assertions.assertThat(result.isFailed()).isTrue();
            assertions.assertThat(result.code()).isEqualTo(ValidateCode.WIDTH_GREATER_THAN_POSSIBLE);
            assertions.assertThat(result.args()).isEqualTo(Map.of(MAX_WIDTH_PARAMETER_NAME, BigDecimal.valueOf(60.0)));
            assertions.assertThat(result.getMessage()).isEqualTo("Превышена максимальная ширина - 60.0");
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/validation-dimensions/immutable.xml")
    public void testMaxHeightExceeded() {
        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(450)
                .boxWidth(new BigDecimal("12.0"))
                .boxHeight(new BigDecimal("20.2"))
                .boxLength(new BigDecimal("18.0"))
                .carrierName("DPD")
                .carrierCode("123456")
                .operationDayId(18262L)
                .build();

        ValidateResult result = validationService.validateDimensions("1", boxInfo);

        assertSoftly(assertions -> {
            assertions.assertThat(result.isFailed()).isTrue();
            assertions.assertThat(result.code()).isEqualTo(ValidateCode.HEIGHT_GREATER_THAN_POSSIBLE);
            assertions.assertThat(result.args()).isEqualTo(Map.of(MAX_HEIGHT_PARAMETER_NAME, BigDecimal.valueOf(20.0)));
            assertions.assertThat(result.getMessage()).isEqualTo("Превышена максимальная высота - 20.0");
        });
    }

    @Test
    @DatabaseSetup("/sorting/service/validation-dimensions/immutable.xml")
    public void testMaxLengthExceeded() {
        BoxInfo boxInfo = BoxInfo.builder()
                .boxWeight(450)
                .boxWidth(new BigDecimal("12.0"))
                .boxHeight(new BigDecimal("6.0"))
                .boxLength(new BigDecimal("30.1"))
                .carrierName("DPD")
                .carrierCode("123456")
                .operationDayId(18262L)
                .build();

        ValidateResult result = validationService.validateDimensions("1", boxInfo);

        assertSoftly(assertions -> {
            assertions.assertThat(result.isFailed()).isTrue();
            assertions.assertThat(result.code()).isEqualTo(ValidateCode.LENGTH_GREATER_THAN_POSSIBLE);
            assertions.assertThat(result.args()).isEqualTo(Map.of(MAX_LENGTH_PARAMETER_NAME, BigDecimal.valueOf(30.0)));
            assertions.assertThat(result.getMessage()).isEqualTo("Превышена максимальная длина - 30.0");
        });
    }
}
