package ru.yandex.market.delivery.mdbapp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.delivery.mdbapp.configuration.ClockConfig;
import ru.yandex.market.delivery.mdbapp.configuration.LatchTaskListenerConfig;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MdbApplication.class, ClockConfig.class, LatchTaskListenerConfig.class},
    properties = {
        "mdb.zookeeper.connectionString=#{testZK.connectString}",
        "mdb.zookeeper.connectionTimeout=10000",
        "mdb.zookeeper.sessionTimeout=120000",
        "mdb.zookeeper.maxRetryCount=5",
        "mdb.zookeeper.retryInterval=5000",
        "mdb.zookeeper.namespace=yandex/market/delivery/mdb",
        "mdb.poller.shipments.enabled=true",
        "feature.accept-fake-order-events-enabled=false",
        "mdb.poller.failover.maxRetryCount=5",
        "mock.healthManager=false",
        "queue.mailsender.retryInterval=100000",
        "queue.mailsender.noTaskTimeout=100000",
        "queue.mailsender.betweenTaskTimeout=100000",
        "queue.cancel.order.retryInterval=100000",
        "queue.cancel.order.noTaskTimeout=100000",
        "queue.cancel.order.betweenTaskTimeout=100000"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("integration-test")
public @interface IntegrationTest {
}
