package ru.yandex.market.pers.grade.core.user;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.market.sdk.userinfo.domain.SberlogInfo;

public class SberlogUserInfoTest {

    @Test
    public void getDefaultValuesShouldWorkCorrectly() {
        final UserInfo userInfo = new SberlogUserInfo(new SberlogInfo());
        Assert.assertEquals(0, userInfo.getUserId());
        Assert.assertNull(userInfo.getLogin());
        Assert.assertEquals("", userInfo.getValue(UserInfoField.FIO));
        Assert.assertEquals("0", userInfo.getValue(UserInfoField.SEX));
        Assert.assertNull(userInfo.getValue(UserInfoField.BIRTH_DATE));
        Assert.assertNull(userInfo.getValue(UserInfoField.REG_DATE));
        Assert.assertNull(userInfo.getValue(UserInfoField.COUNTRY));
        Assert.assertNull(userInfo.getValue(UserInfoField.REGION));
        Assert.assertNull(userInfo.getValue(UserInfoField.CITY));
        Assert.assertNull(userInfo.getValue(UserInfoField.LOGIN));
        Assert.assertNull(userInfo.getValue(UserInfoField.EMAIL));
    }
}
