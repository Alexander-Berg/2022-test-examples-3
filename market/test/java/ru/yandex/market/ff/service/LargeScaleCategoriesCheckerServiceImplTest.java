package ru.yandex.market.ff.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.ff.base.IntegrationTest;

@ActiveProfiles("LargeScaleCategoriesCheckerServiceImplTest")
public class LargeScaleCategoriesCheckerServiceImplTest extends IntegrationTest {

    @Autowired
    private LargeScaleCategoriesChecker checker;

    /**
     * Проверяет, что для категории из списка вернется true.
     */
    @Test
    @DatabaseSetup("classpath:service/large-scale-categories/large-scale-categories.xml")
    public void testExceptionalCategory() {
        assertions.assertThat(checker.isLargeScaleCategory(1337L)).isTrue();
    }

    /**
     * Проверяет, что для категории не из списка вернется false.
     */
    @Test
    @DatabaseSetup("classpath:service/large-scale-categories/large-scale-categories.xml")
    public void testNotExceptionalCategory() {
        assertions.assertThat(checker.isLargeScaleCategory(1338L)).isFalse();
    }

    @Test
    public void testNullIsNotExceptionalCategory() {
        assertions.assertThat(checker.isLargeScaleCategory(null)).isFalse();
    }
}
