package ru.yandex.market.tpl.integration.tests.stress.shooter;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepwiseStressShooterTest {

    @Test
    void shoot() {
        Queue<Long> timesQueue = new ConcurrentLinkedQueue<>();
        List<Runnable> runnables = IntStream.range(0, 200)
                .mapToObj(i -> (Runnable) () -> timesQueue.add(System.currentTimeMillis()))
                .collect(Collectors.toList());
        new StepwiseStressShooter(15, 45, 15, 1).shoot(runnables);

        List<Long> times = new ArrayList<>(timesQueue);
        // проверяем что задержка произошла после 15 вызова
        assertThat(times.get(14) - times.get(13)).isLessThan(100);
        assertThat(times.get(15) - times.get(14)).isGreaterThan(500);
        assertThat(times.get(16) - times.get(15)).isLessThan(100);
        // проверяем что задержка произошла после 45 вызова (15 + 30)
        assertThat(times.get(44) - times.get(43)).isLessThan(100);
        assertThat(times.get(45) - times.get(44)).isGreaterThan(500);
        assertThat(times.get(46) - times.get(45)).isLessThan(100);
        // проверяем что задержка произошла после 90 вызова (15 + 30 + 45)
        assertThat(times.get(89) - times.get(88)).isLessThan(100);
    }

    @Test
    void stepsCount() {
        assertThat(new StepwiseStressShooter(3, 9, 2, 1).stepsCount()).isEqualTo(4);
        assertThat(new StepwiseStressShooter(3, 9, 3, 1).stepsCount()).isEqualTo(3);
        assertThat(new StepwiseStressShooter(3, 9, 6, 1).stepsCount()).isEqualTo(2);
        assertThat(new StepwiseStressShooter(1, 5, 1, 1).stepsCount()).isEqualTo(5);
    }

    @Test
    void maxActionsCount() {
        assertThat(new StepwiseStressShooter(3, 9, 2, 1).maxActionsCount()).isEqualTo(24);
        assertThat(new StepwiseStressShooter(3, 9, 2, 3).maxActionsCount()).isEqualTo(72);
        assertThat(new StepwiseStressShooter(3, 9, 3, 1).maxActionsCount()).isEqualTo(18);
        assertThat(new StepwiseStressShooter(3, 9, 3, 2).maxActionsCount()).isEqualTo(36);
        assertThat(new StepwiseStressShooter(1, 10, 1, 1).maxActionsCount()).isEqualTo(55);
    }
}
