package ru.yandex.direct.env;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.env.EnvironmentType.PRODUCTION;
import static ru.yandex.direct.env.EnvironmentType.TESTING;

public class EnvironmentTypeGetLegacyNameTest {
    @Test
    public void getLegacyName_MatchesWithName() {
        assertThat(PRODUCTION.getLegacyName(), is("production"));
    }

    @Test
    public void getLegacyName_DiffersFromName() {
        assertThat(TESTING.getLegacyName(), is("test"));
    }
}
