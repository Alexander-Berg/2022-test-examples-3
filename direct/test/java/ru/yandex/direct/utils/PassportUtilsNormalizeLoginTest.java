package ru.yandex.direct.utils;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnitParamsRunner.class)
public class PassportUtilsNormalizeLoginTest {
    public static Object parametersForPositiveTest() {
        return new Object[][]{
                {"", ""},
                {" ", ""},
                {"zhurs", "zhurs"},
                {" \t zhurs \n ", "zhurs"},
                {" LogIn", "login"},
                {" yndx.log.super-Reader ", "yndx-log-super-reader"},
                {" yndx.log.super-Reader@sitE.com ", "yndx.log.super-reader@site.com"},
        };
    }

    @Test
    @Parameters
    public void positiveTest(String login, String result) {
        assertThat(PassportUtils.normalizeLogin(login), is(result));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void normalizeLogin_failsForNull() {
        PassportUtils.normalizeLogin(null);
    }

}
