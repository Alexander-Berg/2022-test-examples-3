package ru.yandex.direct.web.core.entity.mobilecontent.converter;

import java.util.Collection;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class TrackingSystemConversionTest {
    @Parameterized.Parameter()
    public MobileAppTrackerTrackingSystem trackerTrackingSystem;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<MobileAppTrackerTrackingSystem> data() {
        return EnumSet.allOf(MobileAppTrackerTrackingSystem.class);
    }

    @Test
    public void trackingSystemHasConversionRule() {
        // Если тест падает, нужно добавить появившееся значение
        // в правила конвертации TrackerConverter.TRACKING_SYSTEM_MAP
        assertThat(TrackerConverter.TRACKING_SYSTEM_MAP).containsKeys(trackerTrackingSystem);
    }
}
