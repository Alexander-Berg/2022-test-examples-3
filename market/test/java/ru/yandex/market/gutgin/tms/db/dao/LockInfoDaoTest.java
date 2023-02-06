package ru.yandex.market.gutgin.tms.db.dao;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.enums.LockStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.LockInfo;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SERVICE_INSTANCE;

public class LockInfoDaoTest  extends DBDcpStateGenerator {

    LockInfoDao lockInfoDao;
    ServiceInstanceDao serviceInstanceDao;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        this.lockInfoDao = new LockInfoDao(configuration);
        this.serviceInstanceDao = new ServiceInstanceDao(configuration);
    }

    @Test
    public void SuccessLockTest() {
        Optional<Long> serviceInstanceIdOption = serviceInstanceDao.insert("test", 1234);
        if (!serviceInstanceIdOption.isPresent()) {
            fail("Cannot insert instance");
            return;
        }
        long serviceInstanceId = serviceInstanceIdOption.get();

        List<Long> lockIds = Arrays.asList(100L, 101L, 102L, 103L, 104L);

        lockIds.forEach(id->accept(id, LockStatus.FREE));
        List<Long> successLocksIds = lockInfoDao.setLockedByService(serviceInstanceId, lockIds);
        assertThat(successLocksIds).containsAll(lockIds);
        List<LockInfo> lockInfoList = lockInfoDao.fetchById(lockIds.toArray(new Long[0]));
        assertThat(lockInfoList)
            .allMatch(lockInfo -> lockInfo.getStatus().equals(LockStatus.LOCKED));
        assertThat(lockInfoList.stream().map(LockInfo::getId).collect(Collectors.toList()))
            .containsAll(lockIds);
    }

    @Test
    public void notSuccessLockTest() {
        Optional<Long> serviceInstanceIdOption = serviceInstanceDao.insert("test", 1234);
        if (!serviceInstanceIdOption.isPresent()) {
            fail("Cannot insert instance");
            return;
        }
        long serviceInstanceId = serviceInstanceIdOption.get();

        List<Long> lockIds = Arrays.asList(100L, 101L, 102L, 103L, 104L);

        lockIds.forEach(id->accept(id, LockStatus.LOCKED));
        List<Long> successLocksIds = lockInfoDao.setLockedByService(serviceInstanceId, lockIds);
        assertThat(successLocksIds)
            .doesNotContainAnyElementsOf(lockIds)
            .isEmpty();
    }

    @Test
    public void partialSuccessLockTest() {
        Optional<Long> serviceInstanceIdOption = serviceInstanceDao.insert("test", 1234);
        if (!serviceInstanceIdOption.isPresent()) {
            fail("Cannot insert instance");
            return;
        }
        long serviceInstanceId = serviceInstanceIdOption.get();

        List<Long> successLockIds = Arrays.asList(100L, 102L, 104L);
        List<Long> notSuccessLockIds = Arrays.asList(101L, 103L);

        successLockIds.forEach(id->accept(id, LockStatus.FREE));

        notSuccessLockIds.forEach(id->accept(id, LockStatus.LOCKED));

        List<Long> successLockedIds = lockInfoDao.setLockedByService(serviceInstanceId,
            Stream.concat(successLockIds.stream(), notSuccessLockIds.stream()).collect(Collectors.toList()));
        assertThat(successLockedIds)
            .doesNotContainAnyElementsOf(notSuccessLockIds)
            .containsAll(successLockIds)
            ;

        List<LockInfo> lockInfoList = lockInfoDao.fetchById(successLockIds.toArray(new Long[0]));
        assertThat(lockInfoList)
            .allMatch(lockInfo -> lockInfo.getStatus().equals(LockStatus.LOCKED));
        assertThat(lockInfoList.stream().map(LockInfo::getId).collect(Collectors.toList()))
            .containsAll(successLockIds);
    }

    private void accept(Long id, LockStatus lockStatus) {
        final LockInfo lockInfo = new LockInfo();
        lockInfo.setId(id);
        lockInfo.setStatus(lockStatus);
        lockInfo.setCreateTime(Timestamp.valueOf(LocalDateTime.now()));
        lockInfoDao.insert(lockInfo);
    }
}
