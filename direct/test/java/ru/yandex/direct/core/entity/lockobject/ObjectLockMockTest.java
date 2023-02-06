package ru.yandex.direct.core.entity.lockobject;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.lockobject.repository.LockObjectRepository;
import ru.yandex.direct.core.entity.lockobject.service.LockObjectService;
import ru.yandex.direct.core.entity.model.LockObjectInfo;
import ru.yandex.direct.core.entity.model.LockObjectType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.lockobject.PpcDictObjectLockBuilder.DEFAULT_HALF_MD5_HASH;
import static ru.yandex.direct.core.entity.lockobject.PpcDictObjectLockBuilder.DEFAULT_LOCK_DURATION;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(MockitoJUnitRunner.class)
public class ObjectLockMockTest {

    private PpcDictObjectLockBuilder objectLockBuilder;
    private Long objectId;
    private LockObjectInfo lockObjectInfo;

    @SuppressWarnings("unused")
    @Mock
    private LockObjectRepository lockObjectRepository;

    @InjectMocks
    @Spy
    private LockObjectService lockObjectService;

    @Captor
    private ArgumentCaptor<LockObjectInfo> argumentCaptor;

    @Before
    public void initTestData() {
        objectLockBuilder = lockObjectService.newCampaignLockBuilder();
        clearInvocations(lockObjectService);
        objectId = RandomNumberUtils.nextPositiveLong();

        doReturn(true)
                .when(lockObjectService).addLockObject(any());

        lockObjectInfo = new LockObjectInfo()
                .withObjectId(objectId)
                .withObjectType(LockObjectType.CAMPAIGN)
                .withHalfMd5Hash(DEFAULT_HALF_MD5_HASH)
                .withDuration(DEFAULT_LOCK_DURATION);
    }


    @Test
    public void checkGetLock() {
        ObjectLock lock = objectLockBuilder.createLock(objectId);

        boolean result = lock.lock();
        assertThat(result).isTrue();

        verify(lockObjectService).deleteLockObject(lockObjectInfo.getObjectId(), lockObjectInfo.getObjectType(), null);
        verify(lockObjectService).addLockObject(argumentCaptor.capture());
        verifyNoMoreInteractions(lockObjectService);

        assertThat(argumentCaptor.getValue())
                .is(matchedBy(beanDiffer(lockObjectInfo)));
    }

    @Test
    public void checkGetLock_withCustomParams() {
        lockObjectInfo
                .withObjectType(LockObjectType.USER)
                .withDuration(Duration.ofSeconds(RandomNumberUtils.nextPositiveInteger()))
                .withHalfMd5Hash(RandomNumberUtils.nextPositiveBigInteger());
        long lockId = RandomNumberUtils.nextPositiveLong();
        ObjectLock lock = lockObjectService.newUserLockBuilder()
                .withDuration(lockObjectInfo.getDuration())
                .withHalfMd5Hash(lockObjectInfo.getHalfMd5Hash())
                .withLockId(lockId)
                .createLock(objectId);

        lock.lock();

        verify(lockObjectService).deleteLockObject(lockObjectInfo.getObjectId(), lockObjectInfo.getObjectType(), lockId);
        verify(lockObjectService).addLockObject(argumentCaptor.capture());
        verify(lockObjectService).newUserLockBuilder();
        verifyNoMoreInteractions(lockObjectService);

        assertThat(argumentCaptor.getValue())
                .is(matchedBy(beanDiffer(lockObjectInfo)));
    }

    @Test
    public void checkUnlock() {
        ObjectLock lock = objectLockBuilder.createLock(objectId);

        doReturn(true)
                .when(lockObjectService).deleteLockObject(any(), any(), any());

        boolean result = lock.unlock();
        assertThat(result).isTrue();

        verify(lockObjectService)
                .deleteLockObject(lockObjectInfo.getObjectId(), lockObjectInfo.getObjectType(), lockObjectInfo.getLockId());
        verifyNoMoreInteractions(lockObjectService);
    }

    @Test
    public void checkIsLocked() {
        ObjectLock lock = objectLockBuilder.createLock(objectId);

        doReturn(new LockObjectInfo().withIsLocked(true))
                .when(lockObjectService).getLockObject(any(), any(), any());

        boolean result = lock.isLocked();
        assertThat(result).isTrue();

        verify(lockObjectService)
                .getLockObject(lockObjectInfo.getObjectId(), lockObjectInfo.getObjectType(), lockObjectInfo.getLockId());
        verifyNoMoreInteractions(lockObjectService);
    }

    @Test
    public void checkGetLock_whenServiceReturnFalse() {
        ObjectLock lock = objectLockBuilder.createLock(objectId);

        doReturn(false)
                .when(lockObjectService).addLockObject(any());

        boolean result = lock.lock();
        assertThat(result).isFalse();
    }

    @Test
    public void checkUnlock_whenServiceReturnFalse() {
        ObjectLock lock = objectLockBuilder.createLock(objectId);

        doReturn(false)
                .when(lockObjectService).deleteLockObject(any(), any(), any());

        boolean result = lock.unlock();
        assertThat(result).isFalse();
    }

    @Test
    public void checkIsLocked_whenServiceReturnFalse() {
        ObjectLock lock = objectLockBuilder.createLock(objectId);

        doReturn(new LockObjectInfo().withIsLocked(false))
                .when(lockObjectService).getLockObject(any(), any(), any());

        boolean result = lock.isLocked();
        assertThat(result).isFalse();
    }

}
