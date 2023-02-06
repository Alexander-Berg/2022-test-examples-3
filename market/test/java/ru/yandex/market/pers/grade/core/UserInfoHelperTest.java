package ru.yandex.market.pers.grade.core;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.framework.user.UserInfo;
import ru.yandex.market.pers.grade.core.user.SberlogUserInfo;
import ru.yandex.market.sdk.userinfo.domain.SberlogInfo;

public class UserInfoHelperTest {

    @Test
    public void toXmlWithNullShouldWorkCorrectly() {
        Assert.assertEquals("", UserInfoHelper.toXml(null));
    }

    @Test
    public void toXmlWithSberIdShouldWorkCorrectly() {
        final UserInfo userInfo = new SberlogUserInfo(new SberlogInfo());
        final String expected = "<login>null</login>\n" +
                "<uid>0</uid>\n" +
                "<fio></fio>\n" +
                "<country></country>\n" +
                "<city></city>\n";
        Assert.assertEquals(expected, UserInfoHelper.toXml(userInfo));
    }
}
