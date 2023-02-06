package ru.yandex.market.tpl.integration.tests.stress.shooter;

public class ConstStressShooter extends StepwiseStressShooter {

    public ConstStressShooter(int rps, int durationSec) {
        super(rps, rps, 1, durationSec);
    }
}
