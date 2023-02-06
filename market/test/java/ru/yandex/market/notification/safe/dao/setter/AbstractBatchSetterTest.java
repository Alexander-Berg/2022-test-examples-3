package ru.yandex.market.notification.safe.dao.setter;

import javax.annotation.Nonnull;

import org.junit.Ignore;

import ru.yandex.market.notification.safe.model.data.PersistentBinaryData;

/**
 * Базовый класс для тестов {@link BatchSetterWithKeyHolder}.
 *
 * @author Vladislav Bauer
 */
@Ignore
abstract class AbstractBatchSetterTest {

    @Nonnull
    PersistentBinaryData createBinaryData() {
        return new PersistentBinaryData(new byte[]{1,2,3}, "TEST");
    }

}
