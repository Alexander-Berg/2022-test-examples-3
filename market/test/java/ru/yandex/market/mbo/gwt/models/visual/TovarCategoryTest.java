package ru.yandex.market.mbo.gwt.models.visual;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

/**
 * @author danfertev
 * @since 17.12.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TovarCategoryTest {
    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .overrideDefaultInitialization(true)
            .build();
    }

    @Test
    public void testDeepCopyDataEqualsWithoutValues() {
        for (int i = 0; i < 1000; i++) {
            TovarCategory tovarCategory = random.nextObject(TovarCategory.class, "names", "tags");
            tovarCategory.setName("Tovar category  " + i);
            for (int j = 0; j < random.nextInt(10); j++) {
                CategoryName randomName = random.nextObject(CategoryName.class);
                tovarCategory.addName(randomName);
            }
            TovarCategory tovarCategoryCopy = tovarCategory.getDeepCopy();

            Assertions.assertThat(tovarCategory.dataEqualsWithoutValues(tovarCategoryCopy)).isTrue();
        }
    }
}
