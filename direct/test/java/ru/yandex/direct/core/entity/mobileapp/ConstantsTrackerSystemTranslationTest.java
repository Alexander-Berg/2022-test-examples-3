package ru.yandex.direct.core.entity.mobileapp;

import java.util.Collection;
import java.util.EnumSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ConstantsTrackerSystemTranslationTest {

    @Parameterized.Parameter()
    public MobileAppTrackerTrackingSystem trackerTrackingSystem;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<MobileAppTrackerTrackingSystem> data() {
        return EnumSet.allOf(MobileAppTrackerTrackingSystem.class);
    }

    @Test
    public void trackingSystemHasTranslation() {
        // Если тест падает, нужно добавить название трекера в TRACKER_NAMES, соотвествующее
        // новому значению в MobileAppTrackerTrackingSystem
        // Это название, в итоге, будет выдаваться пользователю.
        // Если есть сомнения какое оно должно быть, стоит обратиться к @zakhar или к @andreyka
        assertThat(Constants.TRACKER_NAMES).containsKeys(trackerTrackingSystem);
    }
}
