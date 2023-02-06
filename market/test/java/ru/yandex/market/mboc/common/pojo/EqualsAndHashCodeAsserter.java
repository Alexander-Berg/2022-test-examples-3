package ru.yandex.market.mboc.common.pojo;

import java.util.function.Supplier;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.SoftAssertions;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author masterj
 */
final class EqualsAndHashCodeAsserter<P> {
    private static final int WHILE_ITERATIONS = 32;
    private static final long SEED = 17442;
    private final Class<P> pojoClass;
    private final Supplier<P> newPojoSupplier;
    private final AssertHelper<P> assertHelper;

    EqualsAndHashCodeAsserter(Class<P> pojoClass, Supplier<P> newPojoSupplier,
                              String... ignoredForEqualsAndHashCodeFields) {
        this.pojoClass = pojoClass;
        this.newPojoSupplier = newPojoSupplier;
        this.assertHelper = new AssertHelper<>(pojoClass, ignoredForEqualsAndHashCodeFields);
    }

    void assertThatEqualsAndHashCodeAreDefined(SoftAssertions softly) {

        softly.assertThat(newPojoSupplier.get())
            .isEqualTo(newPojoSupplier.get());
        softly.assertThat(newPojoSupplier.get())
            .isEqualTo(newPojoSupplier.get());
    }

    void assertThatAllFieldsAreUsedInEqualsAndHashCode(SoftAssertions softly) {
        assertThat(pojoClass.getSuperclass())
            .isEqualTo(Object.class); // мы не поддерживаем наследование в этой реализации теста

        EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .randomizationDepth(5)
            .overrideDefaultInitialization(true)
            .seed(SEED)
            .build();

        for (AssertHelper.FieldExclusion fieldExclusion : assertHelper.excludeFieldsFromPojoByOne()) {
            String includedField = fieldExclusion.getIncludedField();
            String[] excludedFields = fieldExclusion.getExcludedFields();

            final P emptyPojo = newPojoSupplier.get();

            P pojoWithOneField;
            int whileIterations = WHILE_ITERATIONS;
            boolean shouldGenerateAnother;
            do {
                pojoWithOneField = enhancedRandom.nextObject(pojoClass, excludedFields);
                whileIterations--;
                shouldGenerateAnother = (whileIterations > 0) && assertHelper.areSameByField(
                    emptyPojo, pojoWithOneField, includedField
                );
            } while (shouldGenerateAnother);
            softly.assertThat(whileIterations).isPositive();

            softly.assertThat(emptyPojo)
                .withFailMessage("equals() failed for field '" + includedField + "' of " + pojoClass)
                .isNotEqualTo(pojoWithOneField);
            softly.assertThat(emptyPojo.hashCode())
                .withFailMessage("hashCode() failed for field '" + includedField + "' of " + pojoClass)
                .isNotEqualTo(pojoWithOneField.hashCode());
        }
    }
}
