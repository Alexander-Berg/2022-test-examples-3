package ru.yandex.market.tsum.pipe.engine.runtime.test_data.common;

import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.PipeTester;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;

import java.util.UUID;
import java.util.concurrent.Semaphore;

import static org.junit.Assert.fail;

/**
 * Джоба для тестирования отмены джоб. Предполагаемый сценарий использования:
 * 1. Тест запускает пайплайн с этой джобой в отдельном потоке ({@link PipeTester#runScheduledJobsToCompletionAsync()}).
 * 2. Тест вызывает semaphore.acquire() чтобы дождаться запуска этой джобы.
 * 3. Эта джоба запускается, вызывает semaphore.release() чтобы пропустить тест дальше и повисает надолго.
 * 4. Тест делает что-то, что должно привести к отмене этой джобы, и ждёт завершения всех джоб
 * (Thread#join на треде из п. 1).
 * 5. Если всё ок, то эта джоба отменяется. Если не ок, то эта джоба упадёт с AssertionError. Тест должен проверить
 * {@link JobState#getLastStatusChangeType()}
 * 6. Если что-то, что должно привести к отмене этой джобы, из п. 4 должно также привести к созданию второго запуска, то
 * этот второй запуск завершится мгновенно и успешно.
 *
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 26.07.2018
 */
public class WaitingForInterruptOnceJob implements JobExecutor {
    @Autowired
    private Semaphore semaphore;

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("7ac04b6f-d9a9-4ffe-b3c3-2d08de9716d7");
    }

    @Override
    public void execute(JobContext context) throws InterruptedException {
        if (context.getJobState().getLaunches().size() > 1) {
            // Первый запуск ждёт отмену, последующие завершаются сразу
            return;
        }

        // Разрешаем тесту начинать рестартить первую джобу и тем самым отменять эту.
        semaphore.release();

        // Ждём пока тест отменяет эту джобу.
        Thread.sleep(20000);

        // Если зашли сюда, то значит эта джоба не отменилась
        fail(getClass().getName() + " wasn't interrupted");
    }

    @Override
    public boolean interrupt(JobContext context, Thread executorThread) throws InterruptedException {
        executorThread.interrupt();
        executorThread.join();
        return true;
    }
}
