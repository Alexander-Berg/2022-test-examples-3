package ru.yandex.chemodan.app.psbilling.core.synchronization;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.util.LockService;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class LockServiceTest extends AbstractPsBillingCoreTest {
    @Autowired
    private LockService lockService;

    @Test
    public void lockDoNotFailOnAbsentUser_Group() {
        PassportUid uid = PassportUid.MAX_VALUE;
        Group group = psBillingGroupsFactory.createGroup();
        Integer result = lockService.doWithLockedInTransaction(group.getId(), uid, g -> 1);
        Assert.equals(1, result);
    }

    @Test
    public void lockDoNotFailOnAbsentUser_T() {
        PassportUid uid = PassportUid.MAX_VALUE;
        Integer result = lockService.doWithUserLockedInTransaction(uid.toString(), () -> 1);
        Assert.equals(1, result);
    }

    @Test
    public void lockDoNotFailOnAbsentUser_void() {
        PassportUid uid = PassportUid.MAX_VALUE;
        AtomicBoolean value = new AtomicBoolean(false);
        lockService.doWithUserLockedInTransaction(uid.toString(), () -> value.set(true));
        Assert.equals(value.get(), new AtomicBoolean(true).get());
    }

    @Test
    public void lockReentrant() {
        PassportUid uid = PassportUid.MIN_VALUE;
        AtomicBoolean value = new AtomicBoolean(false);
        userInfoService.findOrCreateUserInfo(uid);
        lockService.doWithUserLockedInTransaction(uid.toString(), () -> {
            lockService.doWithUserLockedInTransaction(uid.toString(), () -> {
                value.set(true);
            });
        });
        Assert.equals(value.get(), new AtomicBoolean(true).get());
    }
}
