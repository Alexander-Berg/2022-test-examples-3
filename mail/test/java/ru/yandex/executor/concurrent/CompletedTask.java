package ru.yandex.executor.concurrent;

public class CompletedTask {
    private final FakeTask task;
    private final long timeTaken;
    private final int number;

    public CompletedTask(
        final FakeTask task,
        final long timeTaken,
        final int number)
    {
        this.task = task;
        this.timeTaken = timeTaken;
        this.number = number;
    }

    public FakeTask task() {
        return task;
    }

    public long timeTaken() {
        return timeTaken;
    }

    public int number() {
        return number;
    }
}

