package ru.yandex.market.mboc.common.masterdata.parsing.validator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ValidationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ValueCommentBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.ItemBlockValidationData;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields.GUARANTEE_PERIOD;
import static ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields.LIFE_TIME;
import static ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields.SHELF_LIFE;

/**
 * @author dmserebr
 * @date 02/04/2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MasterDataInCategoryValidationTest extends MasterDataValidationTest {

    private static final long CATEGORY_ID_1 = 11;
    private static final long CATEGORY_ID_2 = 12;
    private static final long CATEGORY_ID_3 = 13;

    private static void assertErrorInfoNotInRange(List<ErrorInfo> errors, String header,
                                                  String value, String min, String max, SoftAssertions softly) {
        softly.assertThat(errors).hasSize(1);
        ErrorInfo error = errors.iterator().next();
        softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_NOT_IN_RANGE_ERROR_CODE);
        softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
        softly.assertThat(error.toString()).isEqualTo(
            MbocErrors.get().excelValueMustBeInRange(header, value, min, max).toString());
    }

    @Before
    public void before() {
        super.initData();
    }

    @Test
    public void whenValidateTimeUnitsInCategory() {
        initCategorySettings();

        assertSoftly(softly -> {
            // Shelf life
            List<ErrorInfo> result = validateShelfLife(
                new TimeInUnits(9, TimeInUnits.TimeUnit.DAY), CATEGORY_ID_1);
            assertErrorInfoNotInRange(result, SHELF_LIFE, "9 дней", "10 дней", "100 дней", softly);

            result = validateShelfLife(new TimeInUnits(10, TimeInUnits.TimeUnit.DAY), CATEGORY_ID_1);
            softly.assertThat(result).isEmpty();

            result = validateShelfLife(new TimeInUnits(100, TimeInUnits.TimeUnit.DAY), CATEGORY_ID_1);
            softly.assertThat(result).isEmpty();

            result = validateShelfLife(new TimeInUnits(101, TimeInUnits.TimeUnit.DAY), CATEGORY_ID_1);
            assertErrorInfoNotInRange(result, SHELF_LIFE, "101 день", "10 дней", "100 дней", softly);

            result = validateShelfLife(new TimeInUnits(1, TimeInUnits.TimeUnit.YEAR), CATEGORY_ID_1);
            assertErrorInfoNotInRange(result, SHELF_LIFE, "1 год", "10 дней", "100 дней", softly);

            result = validateShelfLife(new TimeInUnits(1, TimeInUnits.TimeUnit.MONTH), CATEGORY_ID_2);
            assertErrorInfoNotInRange(result, SHELF_LIFE, "1 месяц", "40 дней", "3 года", softly);

            result = validateShelfLife(new TimeInUnits(10, TimeInUnits.TimeUnit.WEEK), CATEGORY_ID_2);
            softly.assertThat(result).isEmpty();

            result = validateShelfLife(new TimeInUnits(3, TimeInUnits.TimeUnit.YEAR), CATEGORY_ID_2);
            softly.assertThat(result).isEmpty();

            result = validateShelfLife(new TimeInUnits(4, TimeInUnits.TimeUnit.YEAR), CATEGORY_ID_2);
            assertErrorInfoNotInRange(result, SHELF_LIFE, "4 года", "40 дней", "3 года", softly);

            result = validateShelfLife(new TimeInUnits(48, TimeInUnits.TimeUnit.HOUR), CATEGORY_ID_3);
            assertErrorInfoNotInRange(result, SHELF_LIFE, "48 часов", "3 дня", "10 лет", softly);

            result = validateShelfLife(new TimeInUnits(10, TimeInUnits.TimeUnit.WEEK), CATEGORY_ID_3);
            softly.assertThat(result).isEmpty();

            result = validateShelfLife(new TimeInUnits(11, TimeInUnits.TimeUnit.YEAR), CATEGORY_ID_3);
            assertErrorInfoNotInRange(result, SHELF_LIFE, "11 лет", "3 дня", "10 лет", softly);

            // Life time
            result = validateLifeTime(new TimeInUnits(12, TimeInUnits.TimeUnit.DAY), CATEGORY_ID_1);
            assertErrorInfoNotInRange(result, LIFE_TIME, "12 дней", "30 дней", "300 дней", softly);

            result = validateLifeTime(new TimeInUnits(10, TimeInUnits.TimeUnit.MONTH), CATEGORY_ID_1);
            softly.assertThat(result).isEmpty();

            result = validateLifeTime(new TimeInUnits(11, TimeInUnits.TimeUnit.MONTH), CATEGORY_ID_1);
            assertErrorInfoNotInRange(result, LIFE_TIME, "11 месяцев", "30 дней", "300 дней", softly);

            result = validateLifeTime(new TimeInUnits(100, TimeInUnits.TimeUnit.YEAR), CATEGORY_ID_2);
            assertErrorInfoNotInRange(result, LIFE_TIME, "100 лет", "1 день", "50 лет", softly);

            result = validateLifeTime(new TimeInUnits(1200, TimeInUnits.TimeUnit.DAY), CATEGORY_ID_2);
            softly.assertThat(result).isEmpty();

            // Guarantee period
            result = validateGuaranteePeriod(new TimeInUnits(1, TimeInUnits.TimeUnit.WEEK), CATEGORY_ID_1);
            assertErrorInfoNotInRange(result, GUARANTEE_PERIOD, "1 неделя", "14 дней", "180 дней",
                    softly);

            result = validateGuaranteePeriod(new TimeInUnits(120, TimeInUnits.TimeUnit.DAY), CATEGORY_ID_1);
            softly.assertThat(result).isEmpty();

            result = validateGuaranteePeriod(new TimeInUnits(7, TimeInUnits.TimeUnit.MONTH), CATEGORY_ID_1);
            assertErrorInfoNotInRange(result, GUARANTEE_PERIOD, "7 месяцев", "14 дней", "180 дней",
                    softly);

            result = validateGuaranteePeriod(new TimeInUnits(100, TimeInUnits.TimeUnit.YEAR), CATEGORY_ID_2);
            assertErrorInfoNotInRange(result, GUARANTEE_PERIOD, "100 лет", "1 месяц", "50 лет",
                    softly);

            result = validateGuaranteePeriod(new TimeInUnits(1200, TimeInUnits.TimeUnit.DAY), CATEGORY_ID_2);
            softly.assertThat(result).isEmpty();
        });
    }

    @Test
    public void validateTimeValuesExistence() {
        initCategorySettings();

        masterData.setCategoryId(CATEGORY_ID_1);
        masterData.setShelfLife(new TimeInUnits(90));
        masterData.setLifeTime(new TimeInUnits(300));
        masterData.setGuaranteePeriod(new TimeInUnits(180));

        var context1 = new ValidationContext(CATEGORY_ID_1,
                categoryParamValueRepository.findCategoryParamValues(CATEGORY_ID_1), false, Set.of(), List.of());

        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData, context1);
        Assertions.assertThat(masterDataErrors).isEmpty();
    }

    @Test
    public void whenValidatingGuaranteePeriodExistenceShouldFail() {
        initCategorySettings();

        masterData.setCategoryId(CATEGORY_ID_1);
        masterData.setShelfLife(new TimeInUnits(90));
        masterData.setLifeTime(new TimeInUnits(300));
        masterData.setGuaranteePeriod(null);

        var context1 = new ValidationContext(CATEGORY_ID_1,
                categoryParamValueRepository.findCategoryParamValues(CATEGORY_ID_1), false, Set.of(), List.of());

        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData, context1);

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).hasSize(1);

            ErrorInfo error = masterDataErrors.get(0);
            softly.assertThat(error.getErrorCode()).isEqualTo(VALUE_REQUIRED_ERROR_CODE);
            softly.assertThat(error.getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(error.toString()).contains(GUARANTEE_PERIOD);
        });
    }

    @Test
    public void whenValidatingNotRequiredShelfLifeExistenceShouldFail() {
        initCategorySettings();

        masterData.setCategoryId(CATEGORY_ID_1);
        masterData.setShelfLife(null);
        masterData.setLifeTime(new TimeInUnits(300));
        masterData.setGuaranteePeriod(new TimeInUnits(180));

        var context1 = new ValidationContext(CATEGORY_ID_1,
                categoryParamValueRepository.findCategoryParamValues(CATEGORY_ID_1), false, Set.of(), List.of());

        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData, context1);
        Assertions.assertThat(masterDataErrors).isEmpty();
    }

    @Test
    public void whenValidatingAllTimeValuesExistenceShouldFail() {
        initCategorySettings();

        masterData.setCategoryId(CATEGORY_ID_2);
        masterData.setShelfLife(null);
        masterData.setLifeTime(null);
        masterData.setGuaranteePeriod(null);

        var context2 = new ValidationContext(CATEGORY_ID_2,
                categoryParamValueRepository.findCategoryParamValues(CATEGORY_ID_2), false, Set.of(), List.of());

        List<ErrorInfo> masterDataErrors = masterDataValidator.validateMasterData(masterData, context2);

        assertSoftly(softly -> {
            softly.assertThat(masterDataErrors).hasSize(3);

            softly.assertThat(masterDataErrors.get(0).getErrorCode()).isEqualTo(VALUE_REQUIRED_ERROR_CODE);
            softly.assertThat(masterDataErrors.get(0).getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(masterDataErrors.get(0).toString()).contains(SHELF_LIFE);

            softly.assertThat(masterDataErrors.get(1).getErrorCode()).isEqualTo(VALUE_REQUIRED_ERROR_CODE);
            softly.assertThat(masterDataErrors.get(1).getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(masterDataErrors.get(1).toString()).contains(LIFE_TIME);

            softly.assertThat(masterDataErrors.get(2).getErrorCode()).isEqualTo(VALUE_REQUIRED_ERROR_CODE);
            softly.assertThat(masterDataErrors.get(2).getLevel()).isEqualTo(ErrorInfo.Level.ERROR);
            softly.assertThat(masterDataErrors.get(2).toString()).contains(GUARANTEE_PERIOD);
        });
    }

    private void initCategorySettings() {
        // min-max values
        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MIN_LIMIT_SHELF_LIFE, 10));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT,
                TimeInUnits.TimeUnit.DAY));

        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MAX_LIMIT_SHELF_LIFE, 100));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MAX_LIMIT_SHELF_LIFE_UNIT,
                TimeInUnits.TimeUnit.DAY));

        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(CATEGORY_ID_2, KnownMdmParams.MIN_LIMIT_SHELF_LIFE, 40));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(CATEGORY_ID_2, KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT,
                TimeInUnits.TimeUnit.DAY));

        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(CATEGORY_ID_2, KnownMdmParams.MAX_LIMIT_SHELF_LIFE, 3));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(CATEGORY_ID_2, KnownMdmParams.MAX_LIMIT_SHELF_LIFE_UNIT,
                TimeInUnits.TimeUnit.YEAR));

        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MIN_LIMIT_LIFE_TIME, 30));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MIN_LIMIT_LIFE_TIME_UNIT,
                TimeInUnits.TimeUnit.DAY));

        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MAX_LIMIT_LIFE_TIME, 300));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MAX_LIMIT_LIFE_TIME_UNIT,
                TimeInUnits.TimeUnit.DAY));

        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MIN_LIMIT_GUARANTEE_PERIOD, 14));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MIN_LIMIT_GUARANTEE_PERIOD_UNIT,
                TimeInUnits.TimeUnit.DAY));

        categoryParamValueRepository.insert(
            createNumericCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MAX_LIMIT_GUARANTEE_PERIOD, 180));
        categoryParamValueRepository.insert(
            createTimeUnitCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.MAX_LIMIT_GUARANTEE_PERIOD_UNIT,
                TimeInUnits.TimeUnit.DAY));


        // applicability values
        categoryParamValueRepository.insert(
                createEnumCategoryParamValue(CATEGORY_ID_1, KnownMdmParams.EXPIRATION_DATES_APPLY,
                        KnownMdmParams.EXPIRATION_DATES_MAY_USE_OPTION));

        parameterValueCachingServiceMock.addCategoryParameterValues(CATEGORY_ID_1,
            MboParameters.ParameterValue.newBuilder()
            .setParamId(KnownMdmMboParams.LIFE_TIME_USE_PARAM_ID)
            .setOptionId(KnownMdmMboParams.LIFE_TIME_NOT_ALLOWED_OPTION_ID).build());

        parameterValueCachingServiceMock.addCategoryParameterValues(CATEGORY_ID_1,
            MboParameters.ParameterValue.newBuilder()
            .setParamId(KnownMdmMboParams.GUARANTEE_PERIOD_USE_PARAM_ID)
            .setOptionId(KnownMdmMboParams.GUARANTEE_PERIOD_REQUIRED_OPTION_ID).build());

        categoryParamValueRepository.insert(
                createEnumCategoryParamValue(CATEGORY_ID_2, KnownMdmParams.EXPIRATION_DATES_APPLY,
                        KnownMdmParams.EXPIRATION_DATES_REQUIRED_OPTION));

        parameterValueCachingServiceMock.addCategoryParameterValues(CATEGORY_ID_2,
            MboParameters.ParameterValue.newBuilder()
            .setParamId(KnownMdmMboParams.LIFE_TIME_USE_PARAM_ID)
            .setOptionId(KnownMdmMboParams.LIFE_TIME_REQUIRED_OPTION_ID).build());

        parameterValueCachingServiceMock.addCategoryParameterValues(CATEGORY_ID_2,
            MboParameters.ParameterValue.newBuilder()
            .setParamId(KnownMdmMboParams.GUARANTEE_PERIOD_USE_PARAM_ID)
            .setOptionId(KnownMdmMboParams.GUARANTEE_PERIOD_REQUIRED_OPTION_ID).build());
    }

    private CategoryParamValue createNumericCategoryParamValue(long categoryId, long paramId, int value) {
        CategoryParamValue paramValue = new CategoryParamValue();
        paramValue.setCategoryId(categoryId)
            .setMdmParamId(paramId)
            .setNumeric(BigDecimal.valueOf(value));
        return paramValue;
    }

    private CategoryParamValue createEnumCategoryParamValue(long categoryId, long paramId, MdmParamOption value) {
        CategoryParamValue paramValue = new CategoryParamValue();
        paramValue.setCategoryId(categoryId)
                .setMdmParamId(paramId)
                .setOption(value);
        return paramValue;
    }

    private CategoryParamValue createTimeUnitCategoryParamValue(long categoryId, long paramId,
                                                                TimeInUnits.TimeUnit timeUnit) {
        CategoryParamValue paramValue = new CategoryParamValue();
        Long timeUnitId = KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(timeUnit);
        paramValue.setCategoryId(categoryId)
            .setMdmParamId(paramId)
            .setOption(new MdmParamOption().setId(timeUnitId));
        return paramValue;
    }

    private List<ErrorInfo> validateShelfLife(TimeInUnits shelfLife, long categoryId) {
        ItemBlockValidationData validationData = createValidationData(
          categoryId,
          List.of(createShelfLifeBlock(shelfLife))
        );
        return shelfLifeBlockValidator.findAndValidateBlock(validationData).getErrorInfos();
    }

    private List<ErrorInfo> validateLifeTime(TimeInUnits lifeTime, long categoryId) {
        ItemBlockValidationData validationData = createValidationData(
            categoryId,
            List.of(createLifeTimeBlock(lifeTime))
        );
        return lifeTimeBlockValidator.findAndValidateBlock(validationData).getErrorInfos();
    }

    private List<ErrorInfo> validateGuaranteePeriod(TimeInUnits guaranteePeriod, long categoryId) {
        ItemBlockValidationData validationData = createValidationData(
            categoryId,
            List.of(createGuaranteePeriodBlock(guaranteePeriod))
        );
        return guaranteePeriodBlockValidator.findAndValidateBlock(validationData).getErrorInfos();
    }

    private ItemBlockValidationData createValidationData(long categoryId, List<ItemBlock> blocks) {
        return new ItemBlockValidationData(
            blocks,
            categoryParamValueRepository.findCategoryParamValues(categoryId).stream()
                .collect(Collectors.toMap(CategoryParamValue::getMdmParamId, Function.identity())),
            Set.of(),
            categoryId
        );
    }

    private ValueCommentBlock createShelfLifeBlock(TimeInUnits value) {
        return createValueCommentBlock(
            ItemBlock.BlockType.SHELF_LIFE,
            KnownMdmParams.SHELF_LIFE,
            KnownMdmParams.SHELF_LIFE_UNIT,
            KnownMdmParams.SHELF_LIFE_COMMENT,
            value
        );
    }

    private ValueCommentBlock createLifeTimeBlock(TimeInUnits value) {
        return createValueCommentBlock(
            ItemBlock.BlockType.LIFE_TIME,
            KnownMdmParams.LIFE_TIME,
            KnownMdmParams.LIFE_TIME_UNIT,
            KnownMdmParams.LIFE_TIME_COMMENT,
            value
        );
    }

    private ValueCommentBlock createGuaranteePeriodBlock(TimeInUnits value) {
        return createValueCommentBlock(
            ItemBlock.BlockType.GUARANTEE_PERIOD,
            KnownMdmParams.GUARANTEE_PERIOD,
            KnownMdmParams.GUARANTEE_PERIOD_UNIT,
            KnownMdmParams.GUARANTEE_PERIOD_COMMENT,
            value
        );
    }

    private ValueCommentBlock createValueCommentBlock(ItemBlock.BlockType blockType,
                                         long valueParamId,
                                         long unitParamId,
                                         long commentParamId,
                                         TimeInUnits value) {
        var result = new ValueCommentBlock(
            blockType,
            mdmParamCache.get(valueParamId),
            mdmParamCache.get(unitParamId),
            mdmParamCache.get(commentParamId)
        );
        result.fromSskuMasterData(value, "", Instant.EPOCH);
        return result;
    }
}
