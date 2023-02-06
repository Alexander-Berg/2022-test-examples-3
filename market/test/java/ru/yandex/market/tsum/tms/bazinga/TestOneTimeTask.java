package ru.yandex.market.tsum.tms.bazinga;

import org.joda.time.Duration;
import org.springframework.stereotype.Component;
import ru.yandex.commune.bazinga.scheduler.EmptyParameters;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.commune.bazinga.scheduler.OnetimeTaskSupport;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 01/11/16
 */
@Component
public class TestOneTimeTask extends OnetimeTaskSupport<EmptyParameters> {

    public static volatile boolean EXECUTED = false;

    public TestOneTimeTask() {
        super(new EmptyParameters());
    }

    @Override
    protected void execute(EmptyParameters parameters, ExecutionContext context) throws Exception {
        EXECUTED = true;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Duration timeout() {
        return Duration.standardSeconds(1);
    }


}
