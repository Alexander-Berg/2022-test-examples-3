package ru.yandex.direct.core.units.storage;

import java.time.Duration;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class LettuceStorageRetryTest {
    private final String testString = "test";
    private final int result = 42;
    private final int maxAttempts = 3;
    private RedisAdvancedClusterCommands<String, String> commands;
    private Storage storage;

    @Before
    public void setUp() {
        LettuceConnectionProvider connectionProvider = mock(LettuceConnectionProvider.class);
        StatefulRedisClusterConnection<String, String> connection = mock(StatefulRedisClusterConnection.class);
        commands = mock(RedisAdvancedClusterCommands.class);
        when(connectionProvider.getConnection()).thenReturn(connection);
        when(connectionProvider.getMaxAttempts()).thenReturn(maxAttempts);
        when(connection.sync()).thenReturn(commands);
        when(connection.getTimeout()).thenReturn(Duration.ofSeconds(1));
        storage = new LettuceStorage(connectionProvider);
    }

    @Test
    public void retrySuccessOnRedisExceptionTest() {
        when(commands.get(any()))
                .thenThrow(new RedisCommandTimeoutException())
                .thenReturn(Integer.toString(result));
        Assert.assertEquals(Integer.valueOf(result), storage.get(testString));
        verify(commands, times(2)).get(testString);
    }

    @Test
    public void retryFailOnRedisExceptionTest() {
        when(commands.get(any())).thenThrow(new RedisException("Fail"));
        assertThatThrownBy(() -> storage.get(testString)).isInstanceOf(StorageErrorException.class);
        verify(commands, times(maxAttempts)).get(testString);
    }

    @Test
    public void failOnNonRedisExceptionTest() {
        when(commands.get(any())).thenThrow(new RuntimeException());
        assertThatThrownBy(() -> storage.get(testString)).isInstanceOf(StorageErrorException.class);
        verify(commands, times(1)).get(testString);
    }
}
