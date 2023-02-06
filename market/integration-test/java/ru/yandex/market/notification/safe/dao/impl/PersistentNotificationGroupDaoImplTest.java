package ru.yandex.market.notification.safe.dao.impl;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

/**
 * Unit-тесты для {@link PersistentNotificationGroupDaoImpl}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationGroupDaoImplTest extends AbstractDaoCommonTest<PersistentNotificationGroupDaoImpl> {

    @Test
    public void testConstants() {
        ClassUtils.checkConstructor(PersistentNotificationGroupDaoImpl.QueryConstants.class);
    }


    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    PersistentNotificationGroupDaoImpl createDao() {
        return new PersistentNotificationGroupDaoImpl(jdbcTemplate);
    }

}
