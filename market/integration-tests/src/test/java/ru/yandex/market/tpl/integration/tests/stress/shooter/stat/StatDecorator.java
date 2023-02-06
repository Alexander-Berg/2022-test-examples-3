package ru.yandex.market.tpl.integration.tests.stress.shooter.stat;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StatDecorator implements Runnable {
    private final Runnable runnable;
    private final ShootingResultItem shootingResultItem;

    @Override
    public void run() {
        shootingResultItem.start();
        try {
            runnable.run();
        } catch (Throwable e) {
            shootingResultItem.fail(e);
            throw e;
        } finally {
            shootingResultItem.stop();
        }
    }
}
