package ru.yandex.direct.core.entity.lockobject.repository;

import java.math.BigInteger;
import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.model.LockObjectInfo;
import ru.yandex.direct.core.entity.model.LockObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class LockObjectRepositoryTest {

    private static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(5);
    private static final BigInteger DEFAULT_HALF_MD5_HASH = BigInteger.ZERO;

    @Autowired
    private LockObjectRepository lockObjectRepository;

    private LockObjectInfo lockObjectInfo;

    @Before
    public void initTestData() {
        lockObjectInfo = new LockObjectInfo()
                .withObjectId(RandomNumberUtils.nextPositiveLong())
                .withObjectType(LockObjectType.CAMPAIGN)
                .withDuration(DEFAULT_LOCK_DURATION)
                .withHalfMd5Hash(DEFAULT_HALF_MD5_HASH);
    }


    @Test
    public void checkAddLockObject() {
        assertThat(getLockObjectInfo()).isNull();

        boolean result = lockObjectRepository.addLockObject(lockObjectInfo);
        assertThat(result).isTrue();
    }

    @Test
    public void checkAddLockObjectParams() {
        lockObjectRepository.addLockObject(lockObjectInfo);

        var actualLockObjectInfo = getLockObjectInfo();
        var expectedLockObjectInfo = toExpectedInfo(actualLockObjectInfo.getLockId(), lockObjectInfo, true);
        assertThat(actualLockObjectInfo)
                .is(matchedBy(beanDiffer(expectedLockObjectInfo)));
    }

    @Test
    public void checkGetLockObjectInfo_whenIsLockedFalse() {
        // проставляем отрицательный интервал, чтобы lock_time в базе стал меньше now()
        lockObjectInfo.setDuration(Duration.ofSeconds(-3));
        lockObjectRepository.addLockObject(lockObjectInfo);

        var actualLockObjectInfo = getLockObjectInfo();
        var expectedLockObjectInfo = toExpectedInfo(actualLockObjectInfo.getLockId(), lockObjectInfo, false);
        assertThat(actualLockObjectInfo)
                .is(matchedBy(beanDiffer(expectedLockObjectInfo)));
    }

    @Test
    public void checkDeleteLockObject() {
        boolean result = lockObjectRepository.addLockObject(lockObjectInfo);
        assertThat(result).isTrue();

        result = lockObjectRepository.deleteLockObject(lockObjectInfo.getObjectId(), lockObjectInfo.getObjectType(), null);
        assertThat(result).isTrue();

        assertThat(getLockObjectInfo()).isNull();
    }

    @Test
    public void checkDeleteLockObject_withLockId() {
        boolean result = lockObjectRepository.addLockObject(lockObjectInfo);
        assertThat(result).isTrue();

        result = lockObjectRepository.deleteLockObject(lockObjectInfo.getObjectId(), lockObjectInfo.getObjectType(), lockObjectInfo.getLockId());
        assertThat(result).isTrue();

        assertThat(getLockObjectInfo()).isNull();
    }

    private LockObjectInfo getLockObjectInfo() {
        return lockObjectRepository.getLockObject(lockObjectInfo.getObjectId(), lockObjectInfo.getObjectType(), lockObjectInfo.getLockId());
    }

    private static LockObjectInfo toExpectedInfo(long lockId, LockObjectInfo lockObjectInfo, boolean isLocked) {
        return new LockObjectInfo()
                .withLockId(lockId)
                .withObjectId(lockObjectInfo.getObjectId())
                .withObjectType(lockObjectInfo.getObjectType())
                .withHalfMd5Hash(lockObjectInfo.getHalfMd5Hash())
                .withIsLocked(isLocked);
    }

}
