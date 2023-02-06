package ru.yandex.market.notification.safe.model.vo;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

/**
 * Unit-тесты для {@link SpamCheckInfo}.
 *
 * @author Vladislav Bauer
 */
public class SpamCheckInfoTest extends AbstractModelTest {

    @Test
    public void testBasicMethods() {
        final SpamCheckInfo data = new SpamCheckInfo(1, 2);
        final SpamCheckInfo sameData = new SpamCheckInfo(1, 2);
        final SpamCheckInfo otherData = new SpamCheckInfo(2, 1);

        checkBasicMethods(data, sameData, otherData);
    }

}
