package ru.yandex.market.pers.grade.core.db;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.MockedTest;

public class UserInfoValueInterpreterTest extends MockedTest {

    @Autowired
    private UserInfoValueInterpreter userInfoValueInterpreter;

    @Test
    public void getStringValueForSberIdShouldWorkCorrectly() {
        // Замокировано в PersCoreMockFactory
        final long sberId = (1L << 61) - 1L;
        final String actual = userInfoValueInterpreter.getStringValue(sberId);
        Assert.assertTrue(StringUtils.isNotBlank(actual));
        Assert.assertTrue(actual.contains("<login>null</login>"));
        Assert.assertTrue(actual.contains("<uid>0</uid>"));
        Assert.assertTrue(actual.contains("fio"));
        Assert.assertTrue(actual.contains("country"));
        Assert.assertTrue(actual.contains("city"));
    }

    @Test
    public void getStringValueForPassportIdShouldWorkCorrectly() {
        // Замокировано в PersCoreMockFactory
        final long passportId = 1L;
        final String actual = userInfoValueInterpreter.getStringValue(passportId);
        Assert.assertTrue(StringUtils.isNotBlank(actual));
        Assert.assertTrue(actual.contains("<login>login"));
        Assert.assertTrue(actual.contains("uid"));
        Assert.assertTrue(actual.contains("fio"));
        Assert.assertTrue(actual.contains("country"));
        Assert.assertTrue(actual.contains("city"));
    }
}
