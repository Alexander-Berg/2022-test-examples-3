package ru.yandex.market.mbo.db.params;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParameterValueBuilder;

import java.util.Collections;

import static ru.yandex.market.mbo.db.params.IParameterLoaderService.GLOBAL_ENTITIES_HID;

/**
 * @author dmserebr
 * @date 10/04/2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MdmCategorySettingsServiceTest {

    private static final long CATEGORY_1 = 100L;

    private static final long NOT_APPLICABLE_OPTION_ID = 10L;
    private static final long NOT_MANDATORY_OPTION_ID = 11L;
    private static final long MANDATORY_OPTION_ID = 12L;

    private MdmCategorySettingsService mdmCategorySettingsService;
    private ParameterLoaderServiceStub parameterLoaderService;
    private CategoryParameterValuesService categoryParameterValuesService;

    private CategoryParam lifeShelfParam;

    @Before
    public void before() {
        categoryParameterValuesService = Mockito.mock(CategoryParameterValuesService.class);
        parameterLoaderService = new ParameterLoaderServiceStub();

        parameterLoaderService.addCategoryParam(
            CategoryParamBuilder.newBuilder().setXslName("testParam").setCategoryHid(GLOBAL_ENTITIES_HID).build());

        lifeShelfParam = CategoryParamBuilder.newBuilder().setXslName("LifeShelf")
            .setCategoryHid(GLOBAL_ENTITIES_HID).build();
        parameterLoaderService.addCategoryParam(lifeShelfParam);
        parameterLoaderService.addCategoryParam(
            CategoryParamBuilder.newBuilder().setXslName("ExpirationDatesApply").setCategoryHid(GLOBAL_ENTITIES_HID)
                .addOption(OptionBuilder.newBuilder(MANDATORY_OPTION_ID).addName("применим, обязателен").build())
                .addOption(OptionBuilder.newBuilder(NOT_MANDATORY_OPTION_ID).addName("применим, не обязателен").build())
                .addOption(OptionBuilder.newBuilder(NOT_APPLICABLE_OPTION_ID).addName("не применим").build())
                .build());

        mdmCategorySettingsService = new MdmCategorySettingsService(
            categoryParameterValuesService, parameterLoaderService);
    }

    @Test
    public void testMdmTimeParamApplicableForCategoryIfMandatory() {
        Mockito.when(categoryParameterValuesService.loadCategoryParameterValues(Mockito.eq(CATEGORY_1)))
            .thenReturn(Collections.singletonList(
                ParameterValues.of(ParameterValueBuilder.newBuilder().xslName("ExpirationDatesApply")
                    .optionId(MANDATORY_OPTION_ID).build())));

        Assertions.assertThat(
            mdmCategorySettingsService.isMdmTimeParamApplicableForCategory(lifeShelfParam, CATEGORY_1)).isTrue();
    }

    @Test
    public void testMdmTimeParamApplicableForCategoryIfNotMandatory() {
        Mockito.when(categoryParameterValuesService.loadCategoryParameterValues(Mockito.eq(CATEGORY_1)))
            .thenReturn(Collections.singletonList(
                ParameterValues.of(ParameterValueBuilder.newBuilder().xslName("ExpirationDatesApply")
                    .optionId(MANDATORY_OPTION_ID).build())));

        Assertions.assertThat(
            mdmCategorySettingsService.isMdmTimeParamApplicableForCategory(lifeShelfParam, CATEGORY_1)).isTrue();
    }

    @Test
    public void testMdmTimeParamNotApplicableForCategory() {
        Mockito.when(categoryParameterValuesService.loadCategoryParameterValues(Mockito.eq(CATEGORY_1)))
            .thenReturn(Collections.singletonList(
                ParameterValues.of(ParameterValueBuilder.newBuilder().xslName("ExpirationDatesApply")
                    .optionId(NOT_APPLICABLE_OPTION_ID).build())));

        Assertions.assertThat(
            mdmCategorySettingsService.isMdmTimeParamApplicableForCategory(lifeShelfParam, CATEGORY_1)).isFalse();
    }

    @Test
    public void testNonMdmTimeParamNotApplicableForCategory() {
        CategoryParam testParam = CategoryParamBuilder.newBuilder().setXslName("testParam")
            .setCategoryHid(GLOBAL_ENTITIES_HID).build();

        Mockito.when(categoryParameterValuesService.loadCategoryParameterValues(Mockito.eq(CATEGORY_1)))
            .thenReturn(Collections.singletonList(
                ParameterValues.of(ParameterValueBuilder.newBuilder().xslName("testParam")
                    .optionId(MANDATORY_OPTION_ID).build())));

        Assertions.assertThat(
            mdmCategorySettingsService.isMdmTimeParamApplicableForCategory(testParam, CATEGORY_1)).isFalse();
    }

    @Test
    public void testApplicabilityParamMissingInCacheThenNotApplicableForCategory() {
        CategoryParam testParam = CategoryParamBuilder.newBuilder().setXslName("WarrantyPeriod")
            .setCategoryHid(GLOBAL_ENTITIES_HID).build();

        Mockito.when(categoryParameterValuesService.loadCategoryParameterValues(Mockito.eq(CATEGORY_1)))
            .thenReturn(Collections.emptyList());

        Assertions.assertThat(
            mdmCategorySettingsService.isMdmTimeParamApplicableForCategory(testParam, CATEGORY_1)).isFalse();
    }
}
