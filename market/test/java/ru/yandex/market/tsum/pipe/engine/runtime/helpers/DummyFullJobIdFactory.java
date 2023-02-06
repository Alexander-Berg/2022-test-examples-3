package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import ru.yandex.commune.bazinga.impl.FullJobId;
import ru.yandex.commune.bazinga.impl.JobId;
import ru.yandex.commune.bazinga.impl.TaskId;

import java.util.UUID;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 11.05.17
 */
public class DummyFullJobIdFactory {
    public static FullJobId create() {
        return new FullJobId(new TaskId("pipe"), new JobId(UUID.randomUUID()));
    }
}
