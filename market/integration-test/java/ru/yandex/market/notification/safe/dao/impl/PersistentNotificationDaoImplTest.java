package ru.yandex.market.notification.safe.dao.impl;

import java.time.Instant;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.notification.test.util.ClassUtils;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.notification.safe.model.type.NotificationStatus.NEW;
import static ru.yandex.market.notification.safe.model.type.NotificationStatus.SENDING_ERROR;
import static ru.yandex.market.notification.simple.model.type.NotificationPriority.NORMAL;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.EMAIL;

/**
 * Тесты для {@link PersistentNotificationDaoImpl}.
 *
 * @author Vladislav Bauer
 */
public class PersistentNotificationDaoImplTest extends AbstractDaoCommonTest<PersistentNotificationDaoImpl> {

    @Test
    public void testCreate() {
        //noinspection ConstantConditions
        assertThat(check(dao -> dao.create(null)), empty());
        assertThat(check(dao -> dao.create(Collections.emptySet())), empty());
    }

    @Test
    public void testFindForPreparing() {
        final int maxRetryNum = 3;
        assertThat(check(dao -> dao.findForPreparing(maxRetryNum, 0)), empty());
    }

    @Test
    public void testFindPrepared() {
        assertThat(check(dao -> dao.findPrepared(0, 0)), empty());
    }

    @Test
    public void testFindByGroupsAndTransport() {
        //noinspection ConstantConditions
        assertThat(check(dao -> dao.findByGroupsAndTransport(null, EMAIL)), empty());
        assertThat(check(dao -> dao.findByGroupsAndTransport(emptySet(), EMAIL)), empty());
        assertThat(check(dao -> dao.findByGroupsAndTransport(Collections.singleton(-1L), EMAIL)), empty());
    }

    @Test
    public void testGetSpamCheckInfo() {
        final Instant now = Instant.now();
        check(dao -> dao.getSpamCheckInfo(0, EMAIL, "", now, now, NORMAL, 1));
    }

    @Test
    public void testMarkAsError() {
        //noinspection ConstantConditions
        assertThat(check(dao -> dao.markAsError(null, null)), equalTo(false));

        //noinspection ConstantConditions
        assertThat(check(dao -> dao.markAsError(emptyMap(), null)), equalTo(false));

        check(dao -> dao.markAsError(singletonMap(-1L, Instant.now()), SENDING_ERROR));
    }

    @Test
    public void testMarkAsSend() {
        //noinspection ConstantConditions
        assertThat(check(dao -> dao.markAsSent(null)), equalTo(false));
        assertThat(check(dao -> dao.markAsSent(emptySet())), equalTo(false));
        check(dao -> dao.markAsSent(singleton(-1L)));
    }

    @Test
    public void testUpdateStatuses() {
        //noinspection ConstantConditions
        assertThat(check(dao -> dao.updateStatuses(null)), equalTo(false));
        assertThat(check(dao -> dao.updateStatuses(emptyMap())), equalTo(false));
        check(dao -> dao.updateStatuses(singletonMap(-1L, NEW)));
    }

    @Test
    public void testUpdateStatus() {
        assertThat(check(dao -> dao.updateStatus(-1L, NEW)), equalTo(false));
    }

    @Test
    public void testConstants() {
        ClassUtils.checkConstructor(PersistentNotificationDaoImpl.QueryConstants.class);
    }


    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    PersistentNotificationDaoImpl createDao() {
        return new PersistentNotificationDaoImpl(jdbcTemplate, 2);
    }

}
