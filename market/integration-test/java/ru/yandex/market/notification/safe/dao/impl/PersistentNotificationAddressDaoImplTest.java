package ru.yandex.market.notification.safe.dao.impl;

import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.notification.safe.model.type.NotificationAddressStatus;
import ru.yandex.market.notification.test.util.ClassUtils;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Тесты для {@link PersistentNotificationAddressDaoImpl}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationAddressDaoImplTest
    extends AbstractDaoCommonTest<PersistentNotificationAddressDaoImpl> {

    @Test
    public void testCreate() {
        //noinspection ConstantConditions
        assertThat(check(dao -> dao.create(null)), empty());
        assertThat(check(dao -> dao.create(Collections.emptySet())), empty());
    }

    @Test
    public void testFindUnprocessed() {
        assertThat(check(dao -> dao.findUnprocessed(null)), empty());
        assertThat(check(dao -> dao.findUnprocessed(Collections.emptySet())), empty());
        assertThat(check(dao -> dao.findUnprocessed(Collections.singleton(-1L))), empty());
    }

    @Test
    public void testUpdateStatus() {
        assertThat(check(dao -> dao.updateStatus(-1L, NotificationAddressStatus.ERROR)), equalTo(false));
    }

    @Test
    public void testConstants() {
        ClassUtils.checkConstructor(PersistentNotificationAddressDaoImpl.QueryConstants.class);
    }


    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    PersistentNotificationAddressDaoImpl createDao() {
        return new PersistentNotificationAddressDaoImpl(jdbcTemplate);
    }

}
