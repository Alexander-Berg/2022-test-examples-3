package ru.yandex.market.notification.simple.model.data;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

/**
 * Unit-тесты для {@link OrderInfo}.
 *
 * @author Vladislav Bauer
 */
public class OrderInfoTest extends AbstractModelTest {

    @Test
    public void testBasicMethods() {
        final OrderInfo data = OrderInfo.create(3L);
        final OrderInfo sameData = OrderInfo.create(3L);
        final OrderInfo otherData = OrderInfo.create(5L);

        checkBasicMethods(data, sameData, otherData);
    }

}
