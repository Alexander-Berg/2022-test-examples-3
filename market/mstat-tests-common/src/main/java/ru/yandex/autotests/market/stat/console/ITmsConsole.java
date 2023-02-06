package ru.yandex.autotests.market.stat.console;

import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.beans.Job;

/**
 * Created by entarrion on 06.04.15.
 */
public interface ITmsConsole<T extends Job> {
    LocalDateTime runJob(T job);

    void stopJob(T job);

    void resumeJob(T job);

}
