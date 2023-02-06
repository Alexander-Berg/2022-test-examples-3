package ru.yandex.market.pipelinetests.tests.lms_lom.redis;

import java.time.Instant;

import javax.annotation.Nonnull;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import ru.yandex.market.pipelinetests.tests.lms_lom.AbstractGetActualVersionTest;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в redis")
public class GetActualVersionRedisTest extends AbstractGetActualVersionTest {

    @Nonnull
    @Override
    public Instant getCurrentActualVersion() {
        return LOM_REDIS_STEPS.getActualDataVersion();
    }
}
