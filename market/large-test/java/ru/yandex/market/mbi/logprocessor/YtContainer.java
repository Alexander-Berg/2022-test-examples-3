package ru.yandex.market.mbi.logprocessor;


import java.io.IOException;

import org.eclipse.jetty.io.RuntimeIOException;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Контейнер на основе yt local
 */
public class YtContainer extends DockerComposeContainer<YtContainer> {
    private YtContainer() throws IOException {
        super(new ClassPathResource("docker/docker-compose.yml").getFile());
        waitingFor("ytbackend", Wait.forLogMessage(".*Local YT started.*", 1));
    }

    public static YtContainer create() {
        try {
            return new YtContainer();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }
}
