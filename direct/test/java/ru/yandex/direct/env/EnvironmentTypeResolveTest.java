package ru.yandex.direct.env;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.env.EnvironmentType.PRODUCTION;
import static ru.yandex.direct.env.EnvironmentType.TESTING;

public class EnvironmentTypeResolveTest {

    @Test
    public void resolve_UpperCase() throws Exception {
        assertThat(EnvironmentType.resolve(TESTING.toString()), is(EnvironmentType.TESTING));
    }

    @Test(expected = IllegalArgumentException.class)
    public void resolve_IncorrectValue_Exception() throws Exception {
        EnvironmentType.resolve("XXX");
    }

    @Test
    public void resolve_Null_Null() throws Exception {
        assertThat(EnvironmentType.resolve(null), nullValue());
    }

    @Test
    public void resolve_Empty_Null() throws Exception {
        assertThat(EnvironmentType.resolve(""), nullValue());
    }

    @Test
    public void resolve_LowerCaseWithBlanks() throws Exception {
        assertThat(EnvironmentType.resolve("  " + PRODUCTION.toString().toLowerCase() + " "), is(PRODUCTION));
    }

    @Test
    public void resolve_LegacyName() throws Exception {
        assertThat(EnvironmentType.resolve("test"), is(EnvironmentType.TESTING));
    }

}
