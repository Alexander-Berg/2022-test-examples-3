package ru.yandex.market.mbo.mdm.common.masterdata.validator.block;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.ShelfLifeBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.ItemBlockValidationResult;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class ShelfLifeBlockValidatorTest extends MdmBaseDbTestClass {
    @Autowired
    private CategoryCachingServiceMock categoryCachingService;
    @Autowired
    private MdmParamCache mdmParamCache;

    private ShelfLifeBlockValidator validator;

    @Before
    public void setUp() {
        validator = new ShelfLifeBlockValidator(categoryCachingService);
    }

    @Test
    public void whenNoptFitInCategoryLimitsCreateError() {
        // given
        long categoryId = 100;
        categoryCachingService.addCategory(categoryId, "Молоко");

        MdmParamOption dayOption =
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.DAY));
        Map<Long, CategoryParamValue> categoryLimits = Stream.of(
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.MIN_LIMIT_SHELF_LIFE)
                .setNumeric(BigDecimal.ONE),
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT)
                .setOption(dayOption),
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.MAX_LIMIT_SHELF_LIFE)
                .setNumeric(BigDecimal.TEN),
            (CategoryParamValue) new CategoryParamValue()
                .setCategoryId(categoryId)
                .setMdmParamId(KnownMdmParams.MAX_LIMIT_SHELF_LIFE_UNIT)
                .setOption(dayOption)
        ).collect(Collectors.toMap(MdmParamValue::getMdmParamId, Function.identity()));

        TimeInUnits time = new TimeInUnits(11, TimeInUnits.TimeUnit.DAY);
        ShelfLifeBlock shelfLifeBlock = new ShelfLifeBlock(
            mdmParamCache.get(KnownMdmParams.SHELF_LIFE),
            mdmParamCache.get(KnownMdmParams.SHELF_LIFE_UNIT),
            mdmParamCache.get(KnownMdmParams.SHELF_LIFE_COMMENT)
        );
        shelfLifeBlock.fromSskuMasterData(time, "Хранить в холодильнике", Instant.now());

        ItemBlockValidationData itemBlockValidationData = new ItemBlockValidationData(
            List.of(shelfLifeBlock),
            categoryLimits,
            Set.of(),
            categoryId
        );

        // when
        ItemBlockValidationResult validationResult = validator.findAndValidateBlock(itemBlockValidationData);

        // then
        Assertions.assertThat(validationResult.isSuccess()).isFalse();
        Assertions.assertThat(validationResult.getItemBlock()).isEqualTo(shelfLifeBlock);
        Assertions.assertThat(validationResult.getErrorInfos())
            .map(ErrorInfo::toString)
            .containsExactly(
                "Значение '11 дней' для колонки 'Срок годности'" +
                    " для товара из категории 'Молоко'" +
                    " должно быть в диапазоне 1 день - 10 дней"
            );
    }
}
