package ru.yandex.market.notification.common.model;

import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ModelConstants}.
 *
 * @author Vladislav Bauer
 */
public class ModelConstantsTest {

    @Test
    public void testConstructorContract() {
        ClassUtils.checkConstructor(ModelConstants.class);
    }

    /**
     * XXX(vbauer): Если константы изменятся, то необходимо мигрировать данные в БД.
     */
    @Test
    public void testConstants() {
        assertThat(ModelConstants.ADDRESS, equalTo("address"));
        assertThat(ModelConstants.ATTACHMENT, equalTo("attachment"));
        assertThat(ModelConstants.CONTENT, equalTo("content"));
        assertThat(ModelConstants.DESTINATION, equalTo("destination"));
    }

}
