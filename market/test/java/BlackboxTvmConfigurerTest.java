package ru.yandex.market.starter.tvmblackbox;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BlackboxTvmConfigurerTest {

    @Test
    public void blackboxTvmIdTest() {
        final int blackboxId = 1453;
        final BlackboxInfo blackboxInfo = new BlackboxInfo("ewer", blackboxId);
        final BlackboxTvmConfigurer configurer = new BlackboxTvmConfigurer(blackboxInfo);

        Assertions.assertEquals(Set.of(blackboxId), configurer.getDestinations());
    }
}
