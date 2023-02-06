package ru.yandex.direct.core.entity.lockobject;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.lockobject.repository.LockObjectRepository;
import ru.yandex.direct.core.entity.lockobject.service.LockObjectService;
import ru.yandex.direct.core.entity.model.LockObjectInfo;
import ru.yandex.direct.core.entity.model.LockObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ObjectLockTest {

    private Long objectId;
    private BigInteger halfMd5Hash;
    private ObjectLock lock;

    @Autowired
    private LockObjectService lockObjectService;

    @Autowired
    private LockObjectRepository lockObjectRepository;

    @Before
    public void initTestData() {
        objectId = RandomNumberUtils.nextPositiveLong();
        halfMd5Hash = RandomNumberUtils.nextPositiveBigInteger();
        lock = lockObjectService.newCampaignLockBuilder()
                .withHalfMd5Hash(halfMd5Hash)
                .createLock(objectId);
    }


    @Test
    public void checkGetLock() {
        boolean result = lock.lock();
        assertThat(result).isTrue();
        assertThat(lock.isLocked()).isTrue();

        var lockObjectInfo = lockObjectRepository.getLockObject(objectId, LockObjectType.CAMPAIGN, null);
        //noinspection ConstantConditions
        var expectedLockObjectInfo = new LockObjectInfo()
                .withLockId(lockObjectInfo.getLockId())
                .withObjectId(objectId)
                .withObjectType(LockObjectType.CAMPAIGN)
                .withHalfMd5Hash(halfMd5Hash)
                .withIsLocked(true);

        assertThat(lockObjectInfo)
                .is(matchedBy(beanDiffer(expectedLockObjectInfo)));
    }

    @Test
    public void checkUnlock() {
        boolean result = lock.lock();
        assertThat(result).isTrue();

        result = lock.unlock();
        assertThat(result).isTrue();
        assertThat(lock.isLocked()).isFalse();

        var lockObjectInfo = lockObjectRepository.getLockObject(objectId, LockObjectType.CAMPAIGN, null);
        assertThat(lockObjectInfo).isNull();
    }

}
