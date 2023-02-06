package ru.yandex.market.pipelinetests.tests.lms_lom;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nonnull;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
public abstract class AbstractGetActualVersionTest extends AbstractLmsLomTest {

    private static final Duration MAX_DURATION = Duration.of(1, ChronoUnit.HOURS);

    @Test
    @DisplayName("Данные отстают от реальных не более, чем на час")
    void versionIsActual() {
        softly.assertThat(Duration.between(getCurrentActualVersion(), Instant.now()).compareTo(MAX_DURATION))
            .isNegative();
    }

    @Nonnull
    public abstract Instant getCurrentActualVersion();
}
