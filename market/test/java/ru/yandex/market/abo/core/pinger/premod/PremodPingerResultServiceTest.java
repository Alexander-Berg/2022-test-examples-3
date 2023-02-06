package ru.yandex.market.abo.core.pinger.premod;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType;

/**
 * @author imelnikov
 * @since 14.04.2021
 */
class PremodPingerResultServiceTest extends EmptyTest {

    @Autowired
    PremodPingerResultService service;

    @Test
    void load() {
        service.loadResults(MpGeneratorType.PREMOD_CPC_CPA.getId(), LocalDateTime.now(), LocalDateTime.now());
    }
}
