package ru.yandex.market.admin.model.convert;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.randomizers.misc.EnumRandomizer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.Preconditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.report.tabular.model.CampaignResolver;
import ru.yandex.common.report.tabular.model.Cell;
import ru.yandex.common.report.tabular.model.ReportMetaData;
import ru.yandex.common.report.tabular.model.StringCell;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.outlet.fileupload.Row;
import ru.yandex.market.core.feature.model.cutoff.CommonCutoffs;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.message.model.CampaignList;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.param.model.StringParamValue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.mbi.util.Functional.constantSupplier;

/**
 * Функциональные тесты для {@link ReflectDataModelConverter}.
 *
 * @author Vladislav Bauer
 */
@TestInstance(Lifecycle.PER_CLASS)
class ReflectDataModelConverterTest extends FunctionalTest {

    private static final EnhancedRandom OBJECT_GENERATOR = createObjectGenerator();

    @Autowired
    private List<ReflectDataModelConverter> converters;

    private static EnhancedRandom createObjectGenerator() {
        final Supplier<Object> empty = constantSupplier(null);

        return EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .randomize(Cell.class, constantSupplier(new StringCell(StringUtils.EMPTY)))
                .randomize(ru.yandex.market.core.report.model.Cell.class, constantSupplier(new StringCell(StringUtils.EMPTY)))
                .randomize(ReportMetaData.class, empty)
                .randomize(CampaignResolver.class, empty)
                .randomize(ru.yandex.common.report.tabular.model.CampaignResolver.class, empty)
                .randomize(JdbcTemplate.class, empty)
                .randomize(CampaignList.class, empty)
                .randomize(Class.class, empty)
                .randomize(FeatureCustomCutoffType.class, new EnumRandomizer<>(CommonCutoffs.class))
                .randomize(ParamValue.class, constantSupplier(new StringParamValue(ParamType.NULL, 0, StringUtils.EMPTY)))
                .randomize(Row.class, constantSupplier(Row.withIndex(0)))
                .randomize(Pattern.class, empty)
                .build();
    }

    @SuppressWarnings("all")
    @ParameterizedTest(name = "testFromCoreToUI {0} - {1}")
    @MethodSource("converters")
    void testFromCoreToUI(
            final String coreTypeClassName, final String uiTypeClassName,
            final ReflectDataModelConverter converter
    ) {
        final Class coreTypeClass = Preconditions.notNull(converter.coreTypeClass, "Type must be defined");
        final Object coreModel = OBJECT_GENERATOR.nextObject(coreTypeClass);

        assertThat(converter.fromCoreToUI(coreModel), notNullValue());
        assertThat(converter.fromCoreToUI(coreModel, true), notNullValue());
        assertThat(converter.fromCoreToUI(coreModel, false), notNullValue());
        assertThat(converter.fromCoreToUI(Collections.singleton(coreModel)).isEmpty(), equalTo(false));
    }

    private Stream<Arguments> converters() {
        return converters.stream()
                .map(converter -> Arguments.of(
                        converter.coreTypeClass.getSimpleName(),
                        converter.uiTypeClass.getSimpleName(),
                        converter
                ));
    }

}
