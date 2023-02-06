package ru.yandex.market.pricelabs.tms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import ru.yandex.market.pricelabs.RandomizedTestListener;
import ru.yandex.market.yt.client.YtClientProxy;

@Slf4j
class YtCleanupContextListener {

    YtCleanupContextListener(ConfigurableApplicationContext context) {
        context.addApplicationListener(new ApplicationListener<ContextClosedEvent>() {

            @Override
            public void onApplicationEvent(ContextClosedEvent event) {
                if (event.getApplicationContext() != context) {
                    return; // ---
                }
                String path = RandomizedTestListener.YT_PATH_PREFIX + "/" + RandomizedTestListener.RANDOM;

                if (RandomizedTestListener.REUSE_INSTANCE != null) {
                    log.info("Skip cleaning up reusable YT: {}", path);
                    return; // ---
                }

                log.info("Cleaning up YT: {}", path);

                var ytClient = event.getApplicationContext().getBean("targetYt", YtClientProxy.class);
                clean(ytClient, path);
                for (YtClientProxy replica : ytClient.getReplicas()) {
                    clean(replica, path);
                }
            }

            private void clean(YtClientProxy ytClient, String path) {
                try {
                    ytClient.unsafe().deletePath(path);
                } catch (Exception e) {
                    log.error("[{}] Error when cleaning {}", ytClient.getClusterName(), path, e);
                }
            }
        });

    }

}
