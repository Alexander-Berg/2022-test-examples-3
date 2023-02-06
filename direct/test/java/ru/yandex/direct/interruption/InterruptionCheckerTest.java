package ru.yandex.direct.interruption;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.interruption.InterruptionChecker.checkInterruption;
import static ru.yandex.direct.interruption.InterruptionChecker.clear;
import static ru.yandex.direct.interruption.InterruptionChecker.enterProtectedSection;
import static ru.yandex.direct.interruption.InterruptionChecker.leaveProtectedSection;

public class InterruptionCheckerTest {

    @Before
    public void before() {
        Thread.interrupted();
        clear();
    }

    @Test
    public void checkInterruption_NotProtectedAndThreadNotInterrupted_DontThrow() {
        checkInterruption();
    }

    @Test(expected = ThreadInterruptedException.class)
    public void checkInterruption_NotProtectedAndThreadInterrupted_Throws() {
        Thread.currentThread().interrupt();
        checkInterruption();
    }

    @Test
    public void checkInterruption_NotProtectedAndThreadInterrupted_ClearsInterruptionStatus() {
        Thread.currentThread().interrupt();
        try {
            checkInterruption();
        } catch (ThreadInterruptedException e) {
            //
        } finally {
            assertThat(Thread.currentThread().isInterrupted(), is(false));
        }
    }

    @Test
    public void checkInterruption_ProtectedAndThreadNotInterrupted_DontThrow() {
        enterProtectedSection();
        checkInterruption();
    }

    @Test
    public void checkInterruption_ProtectedAndThreadInterrupted_DontThrow() {
        enterProtectedSection();
        Thread.currentThread().interrupt();
        checkInterruption();
    }

    @Test
    public void checkInterruption_ProtectedAndThreadInterrupted_DontClearInterruptionStatus() {
        enterProtectedSection();
        Thread.currentThread().interrupt();
        try {
            checkInterruption();
        } catch (ThreadInterruptedException e) {
            //
        } finally {
            assertThat(Thread.currentThread().isInterrupted(), is(true));
        }
    }

    @Test
    public void checkInterruption_ProtectedDepth2AndThreadInterrupted_DontThrow() {
        enterProtectedSection();
        enterProtectedSection();
        Thread.currentThread().interrupt();
        checkInterruption();
    }

    @Test
    public void checkInterruption_LeaveToProtectedDepth1AndThreadInterrupted_DontThrow() {
        enterProtectedSection();
        enterProtectedSection();
        leaveProtectedSection();
        Thread.currentThread().interrupt();
        checkInterruption();
    }

    @Test(expected = ThreadInterruptedException.class)
    public void checkInterruption_LeaveLastProtectedSectionAndThreadInterrupted_Throws() {
        enterProtectedSection();
        leaveProtectedSection();
        Thread.currentThread().interrupt();
        checkInterruption();
    }

    @Test
    public void enterProtectedSection_ThreadNotInterrupted_DontThrow() {
        enterProtectedSection();
    }

    @Test
    public void enterProtectedSection_ThreadInterrupted_DontThrow() {
        Thread.currentThread().interrupt();
        enterProtectedSection();
    }

    @Test
    public void leaveProtectedSection_LeaveLastSectionAndThreadNotInterrupted_DontThrow() {
        enterProtectedSection();
        leaveProtectedSection();
    }

    @Test(expected = ThreadInterruptedException.class)
    public void leaveProtectedSection_LeaveLastSectionAndThreadInterruptedInProtectedSection_Throws() {
        enterProtectedSection();
        enterProtectedSection();
        Thread.currentThread().interrupt();
        leaveProtectedSection();
        leaveProtectedSection();
    }

    @Test(expected = ThreadInterruptedException.class)
    public void leaveProtectedSection_LeaveLastSectionAndThreadInterruptedBeforeEnteringProtectedSections_Throws() {
        Thread.currentThread().interrupt();
        enterProtectedSection();
        leaveProtectedSection();
    }

    @Test
    public void leaveProtectedSection_LeaveLastSectionAndThreadInterrupted_ClearsInterruptionStatus() {
        Thread.currentThread().interrupt();
        enterProtectedSection();
        try {
            leaveProtectedSection();
        } catch (ThreadInterruptedException e) {
            //
        } finally {
            assertThat(Thread.currentThread().isInterrupted(), is(false));
        }
    }

    @Test
    public void leaveProtectedSection_LeaveNotLastSectionAndThreadInterrupted_DontThrow() {
        enterProtectedSection();
        enterProtectedSection();
        Thread.currentThread().interrupt();
        leaveProtectedSection();
    }

    @Test
    public void leaveProtectedSection_LeaveNotLastSectionAndThreadInterrupted_DontClearInterruptionStatus() {
        enterProtectedSection();
        enterProtectedSection();
        Thread.currentThread().interrupt();
        try {
            leaveProtectedSection();
        } catch (ThreadInterruptedException e) {
            //
        } finally {
            assertThat(Thread.currentThread().isInterrupted(), is(true));
        }
    }

    @Test
    public void leaveProtectedSection_LeftMoreThanEnter_DontFail() {
        enterProtectedSection();
        enterProtectedSection();
        leaveProtectedSection();
        leaveProtectedSection();
        leaveProtectedSection();
    }
}
