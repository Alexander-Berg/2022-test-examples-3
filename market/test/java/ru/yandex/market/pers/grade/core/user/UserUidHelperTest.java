package ru.yandex.market.pers.grade.core.user;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.MockedTest;
import ru.yandex.market.sdk.userinfo.service.ResolveUidService;

public class UserUidHelperTest extends MockedTest {

    private static final long SBER_ID = (1L << 61) - 1L;

    @Autowired
    private ResolveUidService resolveUidService;

    @Test
    public void isSberbankUid() {
        Assert.assertFalse(UserUidHelper.isSberbankUid(resolveUidService, null));
        Assert.assertFalse(UserUidHelper.isSberbankUid(resolveUidService, Long.valueOf(1L)));
        Assert.assertFalse(UserUidHelper.isSberbankUid(resolveUidService, 1L));
        Assert.assertTrue(UserUidHelper.isSberbankUid(resolveUidService, SBER_ID));
    }

    @Test
    public void isPassportUid() {
        Assert.assertTrue(UserUidHelper.isPassportUid(resolveUidService, 1L));
        Assert.assertFalse(UserUidHelper.isPassportUid(resolveUidService, SBER_ID));
    }
}
