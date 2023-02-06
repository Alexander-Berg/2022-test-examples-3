package ru.yandex.market.abo.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.common.zk.ZooClient;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author imelnikov
 */
@Configuration
public class MockZK {

    @Bean
    public ZooClient zooClient() throws Exception {
        ZooClient mock = mock(ZooClient.class);
        when(mock.exists(anyString())).thenReturn(true);
        return mock;
    }
}
