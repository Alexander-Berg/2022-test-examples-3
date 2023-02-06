package ru.yandex.market.mboc.common.dict;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author s-ermakov
 */
public class SupplierEqualsTest {

    private static final long SEED = 15486;

    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .overrideDefaultInitialization(true)
        .build();


    @Test
    public void testCopyWorksCorrectly() {
        for (int i = 0; i < 100; i++) {
            Supplier supplier = getRandomValue();
            Supplier copy = new Supplier(supplier);

            Assertions.assertThat(copy).isEqualTo(supplier);
            Assertions.assertThat(copy.hashCode()).isEqualTo(supplier.hashCode());
        }
    }

    @Test
    public void testSuppliersComparatorWillCorrectlyCompareDifferentObjects() {
        Supplier prevValue = getRandomValue();
        for (int i = 0; i < 100; i++) {
            Supplier supplier = getRandomValue();

            Assertions.assertThat(supplier).isNotEqualTo(prevValue);
            prevValue = supplier;
        }
    }

    private Supplier getRandomValue() {
        return random.nextObject(Supplier.class);
    }
}
