package ru.yandex.direct.dbutil.sharding;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AutoIncrementKeyTest {

    @Test
    public void keyFieldForPhid() {
        assertThat(AutoIncrementKey.PHID.getKeyField().getName(), is("phid"));
    }
}
