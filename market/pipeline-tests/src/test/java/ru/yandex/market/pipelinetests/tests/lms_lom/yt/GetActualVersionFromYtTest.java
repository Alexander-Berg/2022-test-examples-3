package ru.yandex.market.pipelinetests.tests.lms_lom.yt;

import java.time.Instant;

import javax.annotation.Nonnull;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

import ru.yandex.market.pipelinetests.tests.lms_lom.AbstractGetActualVersionTest;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в YT")
public class GetActualVersionFromYtTest extends AbstractGetActualVersionTest {

    @Nonnull
    @Override
    public Instant getCurrentActualVersion() {
        return LOM_LMS_YT_STEPS.getActualDataVersion();
    }
}
