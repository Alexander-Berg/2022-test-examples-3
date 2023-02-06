package ru.yandex.market.common.test.db;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class DbUnitTestTest {
    @Test
    void testNoDirtyContext() {
        assertThat(DbUnitTest.class.getAnnotation(DirtiesContext.class))
                .as("На классе DbUnitTest не должно быть аннотации DirtiesContext")
                .isNull();
    }

}
