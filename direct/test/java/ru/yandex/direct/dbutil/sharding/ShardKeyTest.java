package ru.yandex.direct.dbutil.sharding;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ShardKeyTest {
    @Test
    public void byNameClientId() {
        assertThat(ShardKey.byName("ClientID"), is(ShardKey.CLIENT_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void byNameThrowsException() {
        ShardKey.byName("NoSuchID");
    }

    @Test
    public void isRootForShard() {
        assertThat(ShardKey.SHARD.isRoot(), is(true));
    }

    @Test
    public void isRootForClientId() {
        assertThat(ShardKey.CLIENT_ID.isRoot(), is(false));
    }

    @Test
    public void isRootForUid() {
        assertThat(ShardKey.UID.isRoot(), is(false));
    }

    @Test
    public void valueFieldForClientId() {
        assertThat(ShardKey.CLIENT_ID.getValueField().getName(), is("shard"));
    }

    @Test
    public void valueFieldForLogin() {
        assertThat(ShardKey.LOGIN.getValueField().getName(), is("uid"));
    }
}
