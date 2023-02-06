package ru.yandex.market.billing.geobase;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThatCode;

public class SyncGeoBaseRegionsJobTest extends FunctionalTest {

    @Autowired
    SyncGeoBaseRegionsJob syncGeoBaseRegionsJob;

    @Test
    void testSqls() {
        assertThatCode(() -> syncGeoBaseRegionsJob.saveRegionsInTransaction(
                Collections.emptyList(),
                Map.of()
        )).doesNotThrowAnyException();
    }
}
