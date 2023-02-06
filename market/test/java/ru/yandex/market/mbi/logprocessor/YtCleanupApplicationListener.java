package ru.yandex.market.mbi.logprocessor;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import ru.yandex.market.yt.YtClusters;
import ru.yandex.market.yt.client.YtClientProxy;

/**
 * Слушатель, который запускается при закрытии Spring контекста.
 * Очищает пути в YT, которые использовались для тестирования.
 */
@ParametersAreNonnullByDefault
public class YtCleanupApplicationListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger log = LoggerFactory.getLogger(YtCleanupApplicationListener.class);

    private final String pushApiLogsTargetPath;
    private final String pushApiLogsHistoryPath;
    private final String pushApiLogsHistoryCluster;

    public YtCleanupApplicationListener(String pushApiLogsTargetPath,
                                        String pushApiLogsHistoryPath,
                                        String pushApiLogsHistoryCluster) {
        this.pushApiLogsTargetPath = pushApiLogsTargetPath;
        this.pushApiLogsHistoryPath = pushApiLogsHistoryPath;
        this.pushApiLogsHistoryCluster = pushApiLogsHistoryCluster;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Cleaning up YT: {}", pushApiLogsTargetPath);

        final var targetYtClient = event.getApplicationContext().getBean("targetYt", YtClientProxy.class);
        final var sourceYtClusters = event.getApplicationContext().getBean("sourceYtClusters", YtClusters.class);
        clean(targetYtClient, pushApiLogsTargetPath);
        for (YtClientProxy replica : targetYtClient.getReplicas()) {
            clean(replica, pushApiLogsTargetPath);
        }
        sourceYtClusters.getClientOrPrimary(pushApiLogsHistoryCluster).deletePath(pushApiLogsHistoryPath);
    }

    private void clean(YtClientProxy ytClient, String path) {
        try {
            ytClient.unsafe().deletePath(path);
        } catch (Exception e) {
            log.error("[{}] Error when cleaning {}", ytClient.getClusterName(), path, e);
        }
    }
}
