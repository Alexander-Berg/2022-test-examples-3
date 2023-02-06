package ru.yandex.market.health;

import com.google.common.collect.ObjectArrays;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;

@Configuration
@PropertySource("classpath:integration-tests.properties")
public class DockerConfiguration {
    @Value("${use-local-compose}")
    private boolean useLocalCompose;

    @Value("${clickhouse.host}")
    private String clickhouseHost;

    private static final String COMPOSE_FILE = Paths.get("docker/docker-compose.yml").toAbsolutePath().toString();
    private static final String[] COMPOSE_TEMPLATE = new String[]{"docker-compose", "-f", COMPOSE_FILE};
    private static final Logger log = LogManager.getLogger(DockerConfiguration.class);

    @Bean(initMethod = "startDockerEnv", destroyMethod = "stopDockerEnv")
    public ComposeRunner composeRunner() {
        return new ComposeRunner(useLocalCompose, clickhouseHost);
    }

    public static class ComposeRunner {
        private final boolean enabled;
        private final String clickhouseHost;

        ComposeRunner(boolean enabled, String clickhouseHost) {
            this.enabled = enabled;
            this.clickhouseHost = clickhouseHost;
        }

        void startDockerEnv() throws IOException {
            if (enabled) {
                log.info("Start local docker environment");
                doCompose("up", "-d");
            } else {
                // Проверяю, что окружение для тестов стартануло
                log.info("Check clickhouse server available");
                String clickhouseUrl = "http://" + clickhouseHost + ":8123";
                URL url = new URL(clickhouseUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.getResponseCode();
            }
        }

        void stopDockerEnv() {
            if (enabled) {
                log.info("Stop local docker environment");
                doCompose("down");
            }
        }

        private void doCompose(String... commands) {
            String[] resultCommand = ObjectArrays.concat(COMPOSE_TEMPLATE, commands, String.class);
            try {
                log.info("Try to execute " + Arrays.toString(resultCommand));
                Process process = Runtime.getRuntime().exec(resultCommand);
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException("Error during execute command", e);
            }
        }
    }
}
