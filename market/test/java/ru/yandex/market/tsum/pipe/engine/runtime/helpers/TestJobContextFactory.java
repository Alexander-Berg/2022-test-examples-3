package ru.yandex.market.tsum.pipe.engine.runtime.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import ru.yandex.market.tsum.core.notify.common.NotificationCenter;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.job.JobExecutor;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.LaunchJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.JobContextFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.di.LaunchEntitiesFactory;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceDao;
import ru.yandex.market.tsum.pipe.engine.runtime.di.ResourceService;
import ru.yandex.market.tsum.pipe.engine.runtime.notifications.Notificator;
import ru.yandex.market.tsum.pipe.engine.runtime.state.JobProgressService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeLaunchDao;
import ru.yandex.market.tsum.pipe.engine.runtime.state.PipeStateService;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.JobState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.PipeLaunch;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 12.04.18
 */
public class TestJobContextFactory implements JobContextFactory {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private JobProgressService jobProgressService;

    @Autowired
    private ResourceDao resourceDao;

    @Autowired
    private PipeLaunchDao pipeLaunchDao;

    @Autowired
    private PipeStateService pipeStateService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private LaunchEntitiesFactory launchEntitiesFactory;

    @Autowired
    private Notificator notificator;

    @Autowired
    private NotificationCenter notificationCenter;

    @Autowired
    private SourceCodeService sourceCodeService;

    @Value("${tsum.url}")
    private String tsumUrl;

    @Override
    public JobContext createJobContext(PipeLaunch pipeLaunch, JobState jobState,
                                       Class<? extends JobExecutor> executorClass) {
        return LaunchJobContext.builder()
            .withPipeLaunch(pipeLaunch)
            .withJobState(jobState)
            .withLaunchNumber(1)
            .withExecutorClass(executorClass)
            .withMongoConverter(mongoTemplate.getConverter())
            .withJobProgressService(jobProgressService)
            .withResourceDao(resourceDao)
            .withPipeLaunchDao(pipeLaunchDao)
            .withPipeStateService(pipeStateService)
            .withTsumUrl(tsumUrl)
            .withLaunchEntitiesFactory(launchEntitiesFactory)
            .withResourceService(resourceService)
            .withNotificator(notificator)
            .withNotificationCenter(notificationCenter)
            .withSourceCodeEntityService(sourceCodeService)
            .build();
    }
}
