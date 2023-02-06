package ru.yandex.direct.ytwrapper.chooser;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.JUnitSoftAssertions;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class WorkModeChooserTest {
    private static final String WORK_MODE_DIR = "work_mode";
    private static final EnvironmentType ENV_TESTING = EnvironmentType.TESTING;

    private final List<String> workModeValues = List.of("first", "second", "third");

    @Rule
    public JunitLocalYt localYt = new JunitLocalYt(YPath.simple("//home"));
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    @Rule
    public Timeout timeout = new Timeout(2, TimeUnit.MINUTES);

    private YtClusterConfig ytConfig;

    // full yt lock path is //home/WorkModeChooserTest__{methodName}/work_mode/testing

    @Before
    public void before() {
        ytConfig = localYt.getYtClusterConfig();
    }

    @Test
    public void locksTest() {
        WorkModeChooser<String> workModeChooser = new WorkModeChooser<>(ytConfig,
                WORK_MODE_DIR, ENV_TESTING, new TestAppDestroyer(), workModeValues);

        WorkModeChooser<String> workModeChooser2 = new WorkModeChooser<>(ytConfig,
                WORK_MODE_DIR, ENV_TESTING, new TestAppDestroyer(), workModeValues);

        var workMode = workModeChooser.getOrRequestWorkMode();
        var workMode2 = workModeChooser2.getOrRequestWorkMode();

        softly.assertThat(workMode).isEqualTo(workModeValues.get(0));
        softly.assertThat(workMode2).isEqualTo(workModeValues.get(1));

        // return locks back
        workModeChooser.destroy();
        workModeChooser2.destroy();
    }

    @Test
    public void repeatingRequestTest() {
        WorkModeChooser<String> chooser = new WorkModeChooser<>(ytConfig,
                WORK_MODE_DIR, ENV_TESTING, new TestAppDestroyer(), workModeValues);

        var workMode1 = chooser.getOrRequestWorkMode();
        var workMode2 = chooser.getOrRequestWorkMode();

        assertThat(workMode1).isEqualTo(workMode2).isEqualTo(workModeValues.get(0));

        // return locks back
        chooser.destroy();
    }

    @Test
    public void exceptionIsThrownWhenHasError() {
        var destroyer = new TestAppDestroyer();
        var testException = new RuntimeException("test exception");
        destroyer.destroy(testException);

        WorkModeChooser<String> chooser = new WorkModeChooser<>(ytConfig, WORK_MODE_DIR, ENV_TESTING, destroyer,
                workModeValues);

        assertThatCode(chooser::getOrRequestWorkMode)
                .isInstanceOf(IllegalStateException.class)
                .hasCause(testException);
        chooser.destroy();
    }

    @Test
    public void exceptionIsThrownWhenDestroyed() {
        WorkModeChooser<String> chooser = new WorkModeChooser<>(ytConfig,
                WORK_MODE_DIR, ENV_TESTING, new TestAppDestroyer(), workModeValues);
        chooser.destroy();
        assertThatCode(chooser::getOrRequestWorkMode).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void exceptionIsThrownWhenDestroyedAfterLockWasGot() {
        WorkModeChooser<String> chooser = new WorkModeChooser<>(ytConfig,
                WORK_MODE_DIR, ENV_TESTING, new TestAppDestroyer(), workModeValues);
        chooser.getOrRequestWorkMode();
        chooser.destroy();
        assertThatCode(chooser::getOrRequestWorkMode).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void destroyWithoutLock() {
        WorkModeChooser<String> chooser = new WorkModeChooser<>(ytConfig,
                WORK_MODE_DIR, ENV_TESTING, new TestAppDestroyer(), workModeValues);
        assertThatCode(chooser::destroy).doesNotThrowAnyException();
    }

    private static class TestAppDestroyer implements AppDestroyer {
        private Exception exception = null;

        @Override
        public void destroy(Exception exception) {
            this.exception = exception;
        }

        @Override
        public boolean hasError() {
            return exception != null;
        }

        @Nullable
        @Override
        public Throwable getError() {
            return exception;
        }
    }
}
