package ru.yandex.market.fintech.banksint.job;

public class TestJobImpl extends AbstractJob {


    private boolean executed;

    @Override
    public void doJob() {
        executed = true;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void resetExecuted() {
        executed = false;
    }
}
