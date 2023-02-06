package ru.yandex.market.tpl.integration.tests.stress.shooter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.RequiredArgsConstructor;

import ru.yandex.market.tpl.integration.tests.stress.shooter.stat.ShootingResult;
import ru.yandex.market.tpl.integration.tests.stress.shooter.stat.StatDecorator;

@RequiredArgsConstructor
public class StepwiseStressShooter implements Shooter<ShootingResult> {
    /**
     * Начальная нагрузка, с которой начинаются стрельбы.
     */
    private final int minRps;
    /**
     * Максимальная нагрузка, до которой дойдут стрельбы.
     */
    private final int maxRps;
    /**
     * На сколько будет увеличиваться нагрузка каждый шаг.
     */
    private final int stepRps;
    /**
     * Длительность нагрузки на каждом шагу.
     */
    private final int stepDurationSec;
    List<Future<?>> futures;
    ShootingResult shootingResult;

    @Override
    public ShootingResult shoot(List<Runnable> actions) {
        shootAsync(actions);
        return waitAllFinish();
    }

    public void shootAsync(List<Runnable> actions) {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(Math.min(maxRps * 3, 100));
        this.futures = new ArrayList<>(actions.size());
        this.shootingResult = new ShootingResult();
        int ri = 0;
        int stepsCount = stepsCount();
        long durationSec = stepDurationSec * stepsCount;
        for (long sec = 0; sec < durationSec; sec++) {
            long startMs = sec * 1000L;
            int stepNumber = stepNumber(sec);
            int rps = minRps + stepRps * stepNumber;
            for (int rr = 0; rr < rps; rr++) {
                if (actions.size() <= ri) {
                    break;
                }
                var action = actions.get(ri++);
                var shootingResultItem = shootingResult.create();
                shootingResultItem.setRps(rps);
                var runnable = new StatDecorator(action, shootingResultItem);
                ScheduledFuture<?> future = pool.schedule(runnable, startMs + 1000 / rps, TimeUnit.MILLISECONDS);
                futures.add(future);
            }
        }
    }

    public ShootingResult waitAllFinish() {
        for (var future : futures) {
            try {
                future.get(120, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                e.printStackTrace();
                break;
            }
        }
        return shootingResult;
    }

    public int stepsCount() {
        return (int) ((maxRps - minRps) / stepRps + 1);
    }

    public int stepNumber(long secondsFromStart) {
        return (int) (secondsFromStart / stepDurationSec);
    }

    public int maxActionsCount() {
        int i = minRps;
        int result = 0;
        while (i <= maxRps) {
            result += i;
            i += stepRps;
        }
        return result * stepDurationSec;
    }
}
