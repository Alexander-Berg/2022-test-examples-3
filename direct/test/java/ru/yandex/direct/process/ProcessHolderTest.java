package ru.yandex.direct.process;

import java.time.Duration;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ProcessHolderTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testTermDoubleReport() {
        ProcessExitStatusException exc1 = null;
        try {
            try (ProcessHolder holder = new ProcessHolder(
                    new StubProcess(Duration.ZERO, Duration.ZERO, 1, 100), Duration.ZERO)) {
                holder.stop();
            }
        } catch (ProcessExitStatusException exc) {
            exc1 = exc;
        }
        Assert.assertNotNull(exc1);
        Assert.assertNull(exc1.getCause());
        Assert.assertArrayEquals(new Throwable[]{}, exc1.getSuppressed());
    }

    @Test
    public void testKillDoubleReport() {
        ProcessForciblyDestroyedException exc1 = null;
        try {
            try (ProcessHolder holder = new ProcessHolder(
                    new StubProcess(Duration.ofMinutes(1), Duration.ofMinutes(1), 1, 100), Duration.ofMillis(1))) {
                holder.stop();
            }
        } catch (ProcessForciblyDestroyedException exc) {
            exc1 = exc;
        }
        Assert.assertNotNull(exc1);
        Assert.assertNull(exc1.getCause());
        Assert.assertArrayEquals(new Throwable[]{}, exc1.getSuppressed());
    }

    @Test
    public void testZeroStatusSelfStop() {
        try (ProcessHolder holder = new ProcessHolder(
                new StubProcess(Duration.ZERO, Duration.ZERO, 0, 100), Duration.ofMillis(1))) {
            holder.stop();
        }
    }

    @Test
    public void testNonzeroStatusSelfStop() {
        thrown.expect(ProcessExitStatusException.class);
        thrown.expectMessage("Process [] failed with exit status=1");
        try (ProcessHolder holder = new ProcessHolder(
                new StubProcess(Duration.ZERO, Duration.ZERO, 1, 100), Duration.ofMillis(1))) {
            holder.stop();
        }
    }

    @Test
    public void testInstantDestroyForcibly() {
        try (ProcessHolder holder = new ProcessHolder(
                new StubProcess(Duration.ofMinutes(1), Duration.ofMinutes(1), 1, 100), Duration.ZERO)) {
            holder.stop();
        }
    }

    @Test
    public void testForcedStop() {
        thrown.expect(ProcessForciblyDestroyedException.class);
        try (ProcessHolder holder = new ProcessHolder(
                new StubProcess(Duration.ofMinutes(1), Duration.ofMinutes(1), 1, 100), Duration.ofSeconds(1))) {
            holder.stop();
        }
    }

    @Test
    public void testZeroStatusGracefulStop() {
        try (ProcessHolder holder = new ProcessHolder(
                new StubProcess(Duration.ofMinutes(1), Duration.ZERO, 1, 0), Duration.ofSeconds(1))) {
            holder.stop();
        }
    }

    @Test
    public void testNonzeroStatusGracefulStop() {
        thrown.expect(ProcessExitStatusException.class);
        thrown.expectMessage("Process [] failed with exit status=100");
        try (ProcessHolder holder = new ProcessHolder(
                new StubProcess(Duration.ofMinutes(1), Duration.ZERO, 1, 100), Duration.ofSeconds(1))) {
            holder.stop();
        }
    }
}
