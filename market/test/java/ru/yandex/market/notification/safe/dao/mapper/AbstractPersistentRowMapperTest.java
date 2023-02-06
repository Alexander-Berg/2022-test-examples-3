package ru.yandex.market.notification.safe.dao.mapper;

import org.junit.Test;

import ru.yandex.market.notification.safe.dao.mapper.AbstractPersistentRowMapper.Columns;
import ru.yandex.market.notification.test.util.ClassUtils;

/**
 * Unit-тесты для {@link AbstractPersistentRowMapper}.
 *
 * @author Vladislav Bauer
 */
public class AbstractPersistentRowMapperTest {

    @Test
    public void testColumns() {
        ClassUtils.checkConstructor(Columns.class);
    }

}
