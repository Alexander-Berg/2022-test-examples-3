package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZonedDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.GetRequestDetailsQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.GetRequestDetailsPayload;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
public class GetRequestDetailsQueueConsumerTest extends IntegrationTestWithDbQueueConsumers {

    private static final ResourceId RESOURCE_ID = ResourceId.builder().setYandexId("1").build();
    private static final Task<GetRequestDetailsPayload> CONSUMER_TASK = new Task<>(
            new QueueShardId("shard"),
            new GetRequestDetailsPayload(1),
            0,
            ZonedDateTime.now(),
            "trace",
            "actor"
    );

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private GetRequestDetailsQueueConsumer getRequestDetailsQueueConsumer;

    @Test
    @DatabaseSetup(value = "classpath:db-queue/consumer/get-details/before-for-ff-orders-supply.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/get-details/after-for-ff-orders-supply.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void ffOrdersSupply() throws Exception {
        getRequestDetailsQueueConsumer.execute(CONSUMER_TASK);
        verify(fulfillmentClient).getInbound(RESOURCE_ID, new Partner(145L));
    }

    @Test
    @DatabaseSetup(value = "classpath:db-queue/consumer/get-details/before-for-ff-orders-withdraw.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/get-details/after-for-ff-orders-withdraw.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void ffOrdersWithdraw() throws Exception {
        getRequestDetailsQueueConsumer.execute(CONSUMER_TASK);
        verify(fulfillmentClient).getOutbound(RESOURCE_ID, new Partner(145L));
    }

    @Test
    @DatabaseSetup(value = "classpath:db-queue/consumer/get-details/before-for-ff-supply.xml")
    @ExpectedDatabase(value = "classpath:db-queue/consumer/get-details/after-for-ff-supply.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void ffSupply() throws Exception {
        getRequestDetailsQueueConsumer.execute(CONSUMER_TASK);
        verify(fulfillmentClient).getInbound(RESOURCE_ID, new Partner(100L));
    }

}
