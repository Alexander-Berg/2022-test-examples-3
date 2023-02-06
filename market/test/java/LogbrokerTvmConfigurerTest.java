package ru.yandex.market.starter.tvmlogbroker;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.starter.tvmlogbroker.LogbrokerTvmConfigurer;

public class LogbrokerTvmConfigurerTest {

    @Test
    public void logbrokerTvmIdTest() {
        final Set<Integer> logbrokerTvmClientId = Collections.singleton(1234);
        final LogbrokerTvmConfigurer configurer = new LogbrokerTvmConfigurer(logbrokerTvmClientId);

        Assertions.assertEquals(logbrokerTvmClientId, configurer.getDestinations());
    }
}
