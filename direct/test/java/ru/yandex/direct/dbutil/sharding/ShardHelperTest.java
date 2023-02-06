package ru.yandex.direct.dbutil.sharding;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShardHelperTest {
    @Test
    public void getClientIdsByUids() {
        ShardSupport shardSupport = mock(ShardSupport.class);
        when(shardSupport.getValues(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(12L, 23L, null, 12L));

        ShardHelper shardHelper = new ShardHelper(shardSupport);

        assertThat(
                shardHelper.getClientIdsByUids(Arrays.asList(1L, 5L, 6L, 1L)),
                Matchers.equalTo(ImmutableMap.of(
                        1L, 12L,
                        5L, 23L
                ))
        );
    }
}
