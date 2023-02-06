package ru.yandex.market.core.offer.content;

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.offer.content.parameter.BooleanCategoryParameterType;
import ru.yandex.market.core.offer.content.parameter.CategoryParameterType;
import ru.yandex.market.core.offer.content.parameter.DependentTypeFamily;
import ru.yandex.market.core.offer.content.parameter.DependentTypeFamilySwitchCase;
import ru.yandex.market.core.offer.content.parameter.EnumerationCategoryParameterType;
import ru.yandex.market.core.offer.content.parameter.NumericCategoryParameterType;
import ru.yandex.market.core.offer.content.parameter.TextCategoryParameterType;
import ru.yandex.market.ir.http.PartnerContentApi;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Тесты для {@link PartnerContentConversions}
 */
@ParametersAreNonnullByDefault
class ParameterTypePartnerContentConversionsTest {
    @Nonnull
    private static Matcher<DependentTypeFamily> isExpectedDependentTypeFamily1() {
        return Matchers.allOf(
                MbiMatchers.transformedBy(
                        DependentTypeFamily::switchParameterIdSet,
                        Matchers.containsInAnyOrder(123412L, 1462144L)
                ),
                MbiMatchers.transformedBy(
                        DependentTypeFamily::switchCases,
                        Matchers.contains(Arrays.asList(
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                DependentTypeFamilySwitchCase::expectedParameterAssignments,
                                                Matchers.contains(Arrays.asList(
                                                        PartnerContentMatchers.isEnumeraionParameterAssignment(
                                                                123412L,
                                                                39457
                                                        ),
                                                        PartnerContentMatchers.isEnumeraionParameterAssignment(
                                                                1462144L,
                                                                563458
                                                        )
                                                ))
                                        ),
                                        MbiMatchers.transformedBy(
                                                DependentTypeFamilySwitchCase::resultType,
                                                isExpectedResultType1()
                                        )
                                ),
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                DependentTypeFamilySwitchCase::expectedParameterAssignments,
                                                Matchers.contains(Arrays.asList(
                                                        PartnerContentMatchers.isEnumeraionParameterAssignment(
                                                                123412L,
                                                                347274
                                                        ),
                                                        PartnerContentMatchers.isEnumeraionParameterAssignment(
                                                                1462144L,
                                                                283648
                                                        )
                                                ))
                                        ),
                                        MbiMatchers.transformedBy(
                                                DependentTypeFamilySwitchCase::resultType,
                                                isExpectedResultType2()
                                        )
                                )
                        ))
                )
        );
    }

    @Nonnull
    private static Matcher<DependentTypeFamily> isExpectedDependentTypeFamily2() {
        return Matchers.allOf(
                MbiMatchers.transformedBy(
                        DependentTypeFamily::switchParameterIdSet,
                        Matchers.containsInAnyOrder(123412L, 1462144L)
                ),
                MbiMatchers.transformedBy(
                        DependentTypeFamily::switchCases,
                        Matchers.contains(Arrays.asList(
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                DependentTypeFamilySwitchCase::expectedParameterAssignments,
                                                Matchers.contains(Arrays.asList(
                                                        PartnerContentMatchers.isEnumeraionParameterAssignment(
                                                                123412L,
                                                                39457
                                                        ),
                                                        PartnerContentMatchers.isEnumeraionParameterAssignment(
                                                                1462144L,
                                                                563458
                                                        )
                                                ))
                                        ),
                                        MbiMatchers.transformedBy(
                                                DependentTypeFamilySwitchCase::resultType,
                                                isExpectedResultType3()
                                        )
                                ),
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                DependentTypeFamilySwitchCase::expectedParameterAssignments,
                                                Matchers.contains(Arrays.asList(
                                                        PartnerContentMatchers.isEnumeraionParameterAssignment(
                                                                123412L,
                                                                347274
                                                        ),
                                                        PartnerContentMatchers.isEnumeraionParameterAssignment(
                                                                1462144L,
                                                                283648
                                                        )
                                                ))
                                        ),
                                        MbiMatchers.transformedBy(
                                                DependentTypeFamilySwitchCase::resultType,
                                                isExpectedResultType4()
                                        )
                                )
                        ))
                )
        );
    }

    private static Matcher<EnumerationCategoryParameterType> isExpectedResultType1() {
        return Matchers.allOf(
                MbiMatchers.transformedBy(
                        EnumerationCategoryParameterType::measurementUnit,
                        Matchers.is(Optional.empty())
                ),
                MbiMatchers.transformedBy(
                        EnumerationCategoryParameterType::enumerationValues,
                        Matchers.contains(Arrays.asList(
                                PartnerContentMatchers.isEnumerationValue(349867, "1/2"),
                                PartnerContentMatchers.isEnumerationValue(345698, "1/4")
                        ))
                )
        );
    }

    private static Matcher<EnumerationCategoryParameterType> isExpectedResultType2() {
        return Matchers.allOf(
                MbiMatchers.transformedBy(
                        EnumerationCategoryParameterType::measurementUnit,
                        Matchers.is(Optional.empty())
                ),
                MbiMatchers.transformedBy(
                        EnumerationCategoryParameterType::enumerationValues,
                        Matchers.contains(Arrays.asList(
                                PartnerContentMatchers.isEnumerationValue(2389462, "3/2"),
                                PartnerContentMatchers.isEnumerationValue(2374298, "3/4")
                        ))
                )
        );
    }

    private static Matcher<EnumerationCategoryParameterType> isExpectedResultType3() {
        return Matchers.allOf(
                MbiMatchers.transformedBy(
                        EnumerationCategoryParameterType::measurementUnit,
                        MbiMatchers.isPresent("mm")
                ),
                MbiMatchers.transformedBy(
                        EnumerationCategoryParameterType::enumerationValues,
                        Matchers.contains(Arrays.asList(
                                PartnerContentMatchers.isEnumerationValue(349867, "1/2"),
                                PartnerContentMatchers.isEnumerationValue(345698, "1/4")
                        ))
                )
        );
    }

    private static Matcher<EnumerationCategoryParameterType> isExpectedResultType4() {
        return Matchers.allOf(
                MbiMatchers.transformedBy(
                        EnumerationCategoryParameterType::measurementUnit,
                        MbiMatchers.isPresent("mm")
                ),
                MbiMatchers.transformedBy(
                        EnumerationCategoryParameterType::enumerationValues,
                        Matchers.contains(Arrays.asList(
                                PartnerContentMatchers.isEnumerationValue(2389462, "3/2"),
                                PartnerContentMatchers.isEnumerationValue(2374298, "3/4")
                        ))
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeBoolean() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.BOOL)
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.BOOLEAN)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                Matchers.is(Optional.empty())
                        ),
                        MbiMatchers.instanceOf(BooleanCategoryParameterType.class)
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeText() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.STRING)
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.TEXT)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                Matchers.is(Optional.empty())
                        ),
                        MbiMatchers.instanceOf(TextCategoryParameterType.class)
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeNumeric() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.NUMERIC)
                        .setNumericDomain(PartnerContentApi.NumericDomain.newBuilder()
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.NUMERIC)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                Matchers.is(Optional.empty())
                        ),
                        MbiMatchers.instanceOf(
                                NumericCategoryParameterType.class,
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::minInclusive,
                                                Matchers.is(OptionalDouble.empty())
                                        ),
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::maxInclusive,
                                                Matchers.is(OptionalDouble.empty())
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeNumericWithMeasurementUnit() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.NUMERIC)
                        .setUnit("sm")
                        .setNumericDomain(PartnerContentApi.NumericDomain.newBuilder()
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.NUMERIC)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                MbiMatchers.isPresent("sm")
                        ),
                        MbiMatchers.instanceOf(
                                NumericCategoryParameterType.class,
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::minInclusive,
                                                Matchers.is(OptionalDouble.empty())
                                        ),
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::maxInclusive,
                                                Matchers.is(OptionalDouble.empty())
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeNumericWithMinInclusive() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.NUMERIC)
                        .setNumericDomain(PartnerContentApi.NumericDomain.newBuilder()
                                .setMinValue(0.25)
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.NUMERIC)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                Matchers.is(Optional.empty())
                        ),
                        MbiMatchers.instanceOf(
                                NumericCategoryParameterType.class,
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::minInclusive,
                                                MbiMatchers.isDoublePresent(0.25)
                                        ),
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::maxInclusive,
                                                Matchers.is(OptionalDouble.empty())
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeNumericWithMaxInclusive() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.NUMERIC)
                        .setNumericDomain(PartnerContentApi.NumericDomain.newBuilder()
                                .setMaxValue(1024.25)
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.NUMERIC)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                Matchers.is(Optional.empty())
                        ),
                        MbiMatchers.instanceOf(
                                NumericCategoryParameterType.class,
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::minInclusive,
                                                Matchers.is(OptionalDouble.empty())
                                        ),
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::maxInclusive,
                                                MbiMatchers.isDoublePresent(1024.25)
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeNumericWithAll() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.NUMERIC)
                        .setUnit("kg")
                        .setNumericDomain(PartnerContentApi.NumericDomain.newBuilder()
                                .setMinValue(0.25)
                                .setMaxValue(1024.25)
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.NUMERIC)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                MbiMatchers.isPresent("kg")
                        ),
                        MbiMatchers.instanceOf(
                                NumericCategoryParameterType.class,
                                Matchers.allOf(
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::minInclusive,
                                                MbiMatchers.isDoublePresent(0.25)
                                        ),
                                        MbiMatchers.transformedBy(
                                                NumericCategoryParameterType::maxInclusive,
                                                MbiMatchers.isDoublePresent(1024.25)
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeEnumeration() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.ENUM)
                        .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                        .setId(1234154)
                                        .setName("Samsung")
                                        .build())
                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                        .setId(2864378)
                                        .setName("Apple")
                                        .build())
                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                        .setId(2547634)
                                        .setName("Asus")
                                        .build())
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.ENUMERATION)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                Matchers.is(Optional.empty())
                        ),
                        MbiMatchers.instanceOf(
                                EnumerationCategoryParameterType.class,
                                MbiMatchers.transformedBy(
                                        EnumerationCategoryParameterType::enumerationValues,
                                        Matchers.contains(Arrays.asList(
                                                PartnerContentMatchers.isEnumerationValue(1234154, "Samsung"),
                                                PartnerContentMatchers.isEnumerationValue(2864378, "Apple"),
                                                PartnerContentMatchers.isEnumerationValue(2547634, "Asus")
                                        ))
                                )
                        )
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeEnumerationWithMeasurementUnit() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.ENUM)
                        .setUnit("sm")
                        .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                        .setId(1234154)
                                        .setName("Samsung")
                                        .build())
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.ENUMERATION)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                MbiMatchers.isPresent("sm")
                        ),
                        MbiMatchers.instanceOf(
                                EnumerationCategoryParameterType.class,
                                MbiMatchers.transformedBy(
                                        EnumerationCategoryParameterType::enumerationValues,
                                        Matchers.contains(
                                                PartnerContentMatchers.isEnumerationValue(1234154, "Samsung")
                                        )
                                )
                        )
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeDependent() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.DEPENDENT_ENUM)
                        .setDependentEnumDomain(PartnerContentApi.DependentEnumDomain.newBuilder()
                                .addAllDependsOnParamId(Arrays.asList(123412L, 1462144L))
                                .addDependentSubdomain(PartnerContentApi.DependentEnumSubdomain.newBuilder()
                                        .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                .setParamId(123412L)
                                                .setType(PartnerContentApi.ParameterType.ENUM)
                                                .setEnumOptionId(39457)
                                                .build())
                                        .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                .setParamId(1462144L)
                                                .setType(PartnerContentApi.ParameterType.ENUM)
                                                .setEnumOptionId(563458)
                                                .build())
                                        .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                        .setId(349867)
                                                        .setName("1/2")
                                                        .build())
                                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                        .setId(345698)
                                                        .setName("1/4")
                                                        .build())
                                                .build())
                                        .build())
                                .addDependentSubdomain(PartnerContentApi.DependentEnumSubdomain.newBuilder()
                                        .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                .setParamId(123412L)
                                                .setType(PartnerContentApi.ParameterType.ENUM)
                                                .setEnumOptionId(347274)
                                                .build())
                                        .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                .setParamId(1462144L)
                                                .setType(PartnerContentApi.ParameterType.ENUM)
                                                .setEnumOptionId(283648)
                                                .build())
                                        .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                        .setId(2389462)
                                                        .setName("3/2")
                                                        .build())
                                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                        .setId(2374298)
                                                        .setName("3/4")
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.DEPENDENT_FAMILY)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                Matchers.is(Optional.empty())
                        ),
                        MbiMatchers.instanceOf(
                                DependentTypeFamily.class, isExpectedDependentTypeFamily1()
                        )
                )
        );
    }

    /**
     * Тест для {@link PartnerContentConversions#toCategoryParameterType(PartnerContentApi.ParameterDomain)}.
     */
    @Test
    void testToCategoryParameterTypeDependentWithMeasurementUnit() {
        PartnerContentApi.ParameterDomain irType =
                PartnerContentApi.ParameterDomain.newBuilder()
                        .setType(PartnerContentApi.ParameterType.DEPENDENT_ENUM)
                        .setUnit("mm")
                        .setDependentEnumDomain(PartnerContentApi.DependentEnumDomain.newBuilder()
                                .addAllDependsOnParamId(Arrays.asList(123412L, 1462144L))
                                .addDependentSubdomain(PartnerContentApi.DependentEnumSubdomain.newBuilder()
                                        .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                .setParamId(123412L)
                                                .setType(PartnerContentApi.ParameterType.ENUM)
                                                .setEnumOptionId(39457)
                                                .build())
                                        .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                .setParamId(1462144L)
                                                .setType(PartnerContentApi.ParameterType.ENUM)
                                                .setEnumOptionId(563458)
                                                .build())
                                        .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                        .setId(349867)
                                                        .setName("1/2")
                                                        .build())
                                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                        .setId(345698)
                                                        .setName("1/4")
                                                        .build())
                                                .build())
                                        .build())
                                .addDependentSubdomain(PartnerContentApi.DependentEnumSubdomain.newBuilder()
                                        .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                .setParamId(123412L)
                                                .setType(PartnerContentApi.ParameterType.ENUM)
                                                .setEnumOptionId(347274)
                                                .build())
                                        .addPredicatedBy(PartnerContentApi.ParameterValue.newBuilder()
                                                .setParamId(1462144L)
                                                .setType(PartnerContentApi.ParameterType.ENUM)
                                                .setEnumOptionId(283648)
                                                .build())
                                        .setEnumDomain(PartnerContentApi.EnumDomain.newBuilder()
                                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                        .setId(2389462)
                                                        .setName("3/2")
                                                        .build())
                                                .addEnumOption(PartnerContentApi.EnumOption.newBuilder()
                                                        .setId(2374298)
                                                        .setName("3/4")
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build();
        CategoryParameterType type = PartnerContentConversions.toCategoryParameterType(irType);
        MatcherAssert.assertThat(
                type,
                Matchers.allOf(
                        MbiMatchers.transformedBy(
                                CategoryParameterType::kind,
                                Matchers.is(CategoryParameterType.TypeKind.DEPENDENT_FAMILY)
                        ),
                        MbiMatchers.transformedBy(
                                CategoryParameterType::measurementUnit,
                                Matchers.is(Optional.empty())
                        ),
                        MbiMatchers.instanceOf(
                                DependentTypeFamily.class, isExpectedDependentTypeFamily2()
                        )
                )
        );
    }
}
