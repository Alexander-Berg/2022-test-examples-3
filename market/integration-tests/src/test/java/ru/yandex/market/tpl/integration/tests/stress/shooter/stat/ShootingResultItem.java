package ru.yandex.market.tpl.integration.tests.stress.shooter.stat;

import lombok.Data;
import lombok.Setter;

@Data
public class ShootingResultItem {

    private long startTimestamp = System.currentTimeMillis();
    private long stopTimestamp;
    private Throwable exception;
    @Setter
    private int rps;
    @Setter
    private long durationMs;

    public void start() {
        this.startTimestamp = System.currentTimeMillis();
    }

    public void stop() {
        this.stopTimestamp = System.currentTimeMillis();
        if (durationMs == 0) {
            this.durationMs = stopTimestamp - startTimestamp;
        }
    }

    public void fail(Throwable e) {
        this.exception = e;
    }

    public boolean isFailed() {
        return exception != null;
    }

    public long getDurationMs() {
        if (durationMs != 0) {
            return durationMs;
        }
        return isFinished() ? (stopTimestamp - startTimestamp) : System.currentTimeMillis() - startTimestamp;
    }

    public boolean isFinished() {
        return stopTimestamp != 0;
    }
}
