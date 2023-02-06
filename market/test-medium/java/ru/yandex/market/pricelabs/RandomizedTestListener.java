package ru.yandex.market.pricelabs;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import ru.yandex.misc.net.HostnameUtils;

/**
 * <p>
 * Класс позволяет сконфигурировать необходимые префиксы для подстановки в таблицы YT и схему PostgreSQL.
 *
 * <p>
 * Префиксы могут быть как полностью случайными, так и переиспользуемыми (в таком режиме нам не нужно
 * многократно инициализировать YT и схему PostgreSQL, поэтому все работает быстрее)
 */
@Slf4j
@Order(0)
public class RandomizedTestListener implements TestExecutionListener {

    public static final String YT_PATH_PREFIX = "//home/market/development/pricelabs/unit-tests";
    @Nullable
    public static final String REUSE_INSTANCE = System.getenv("REUSE_INSTANCE");
    public static final String RANDOM;

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final int MAX_PARALLEL_TESTS = 10;
    private static final ServerSocket BOUND_SOCKET;

    static {
        String value;
        if (REUSE_INSTANCE != null) {
            BOUND_SOCKET = lockRunningInstance(Integer.parseInt(REUSE_INSTANCE));
            value = HostnameUtils.localHostname() + "-reuse-" + BOUND_SOCKET.getLocalPort();
        } else {
            BOUND_SOCKET = null;
            value = UUID.randomUUID().toString();
        }
        RANDOM = value.replace('-', '_');
        log.info("Using PREFIX: {}", RANDOM);
    }

    private boolean configured;

    @Override
    public void beforeTestClass(TestContext testContext) {
        if (configured) {
            return; // ---
        }

        System.setProperty("YT_PATH_PREFIX", YT_PATH_PREFIX);
        System.setProperty("RANDOM", RANDOM);
        configured = true;
    }

    private static ServerSocket lockRunningInstance(int startFromPort) {
        for (int i = 0; i < MAX_PARALLEL_TESTS; i++) {
            var port = startFromPort + i;
            try {
                var socket = new ServerSocket();
                socket.setReuseAddress(false);
                socket.bind(new InetSocketAddress("localhost", port));
                log.info("Port {} is ready to use: {} ({}:{})", port, socket.isBound(),
                        socket.getInetAddress(), socket.getLocalPort());
                return socket;
            } catch (IOException e) {
                log.info("Port {} is locked by other process, try next one: {}", port, e.getMessage());
            }
        }
        throw new IllegalStateException("Unable to lock port for running tests");
    }
}
