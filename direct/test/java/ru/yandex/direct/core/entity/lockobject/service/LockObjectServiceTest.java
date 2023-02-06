package ru.yandex.direct.core.entity.lockobject.service;

import java.time.Duration;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.lockobject.repository.LockObjectRepository;
import ru.yandex.direct.core.entity.model.LockObjectInfo;
import ru.yandex.direct.core.entity.model.LockObjectType;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(JUnitParamsRunner.class)
public class LockObjectServiceTest {

    private static Object[] testCases() {
        long objectId = RandomNumberUtils.nextPositiveLong();
        return new Object[][]{
                {"campaignLock", objectId, LockObjectType.CAMPAIGN, null, false},
                {"campaignLock with lockId", objectId, LockObjectType.CAMPAIGN, RandomNumberUtils.nextPositiveLong(), false},
                {"bannerLock", objectId, LockObjectType.BANNER, null, false},
                {"null objectId", null, LockObjectType.CAMPAIGN, null, true},
                {"null objectType", objectId, null, null, true},
        };
    }

    private LockObjectService lockObjectService;
    private LockObjectRepository lockObjectRepository;

    @Before
    public void initTestData() {
        lockObjectRepository = mock(LockObjectRepository.class);
        lockObjectService = new LockObjectService(lockObjectRepository);
    }


    @Test
    @Parameters(method = "testCases")
    @TestCaseName("check getLockObject for {0}")
    public void checkGetLockObject(@SuppressWarnings("unused") String testDescription,
                                   Long objectId, LockObjectType objectType, Long lockId, boolean expectException) {
        if (expectException) {
            assertThatThrownBy(() -> lockObjectService.getLockObject(objectId, objectType, lockId))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyZeroInteractions(lockObjectRepository);
        } else {
            lockObjectService.getLockObject(objectId, objectType, lockId);
            verify(lockObjectRepository).getLockObject(objectId, objectType, lockId);
        }
    }

    @Test
    @Parameters(method = "testCases")
    @TestCaseName("check deleteLockObject for {0}")
    public void checkDeleteLockObject(@SuppressWarnings("unused") String testDescription,
                                      Long objectId, LockObjectType objectType, Long lockId, boolean expectException) {
        if (expectException) {
            assertThatThrownBy(() -> lockObjectService.deleteLockObject(objectId, objectType, lockId))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyZeroInteractions(lockObjectRepository);
        } else {
            lockObjectService.deleteLockObject(objectId, objectType, lockId);
            verify(lockObjectRepository).deleteLockObject(objectId, objectType, lockId);
        }
    }

    @Test
    @Parameters(method = "testCases")
    @TestCaseName("check addLockObject for {0}")
    public void checkAddLockObject(@SuppressWarnings("unused") String testDescription,
                                   Long objectId, LockObjectType objectType, Long lockId, boolean expectException) {
        var lockObjectInfo = new LockObjectInfo()
                .withObjectId(objectId)
                .withObjectType(objectType)
                .withLockId(lockId)
                .withDuration(Duration.ofSeconds(RandomNumberUtils.nextPositiveInteger()));

        if (expectException) {
            assertThatThrownBy(() -> lockObjectService.addLockObject(lockObjectInfo))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyZeroInteractions(lockObjectRepository);
        } else {
            lockObjectService.addLockObject(lockObjectInfo);
            verify(lockObjectRepository).addLockObject(lockObjectInfo);
        }
    }

}
