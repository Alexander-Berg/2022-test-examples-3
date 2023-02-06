package ru.yandex.market.notification.safe.dao.impl;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

/**
 * Тесты для {@link PersistentNotificationDaoImplTest}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationDestinationDaoImplTest
    extends AbstractDaoCommonTest<PersistentNotificationDestinationDaoImpl> {

    @Test
    public void testCreate() {
        //noinspection ConstantConditions
        assertThat(check(dao -> dao.create(null)), empty());

        assertThat(check(dao -> dao.create(Collections.emptySet())), empty());
    }

    @Test
    public void testFindByGroup() {
        check(dao -> dao.findByGroup(-1L));
    }

    @Test
    public void testConstants() {
        ClassUtils.checkConstructor(PersistentNotificationDestinationDaoImpl.QueryConstants.class);
    }


    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    PersistentNotificationDestinationDaoImpl createDao() {
        return new PersistentNotificationDestinationDaoImpl(jdbcTemplate);
    }

}
