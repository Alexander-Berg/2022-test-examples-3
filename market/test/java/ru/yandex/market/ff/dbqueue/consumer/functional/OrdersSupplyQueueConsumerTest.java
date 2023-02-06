package ru.yandex.market.ff.dbqueue.consumer.functional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.base.IntegrationTestWithDbQueueConsumers;
import ru.yandex.market.ff.dbqueue.consumer.ValidateCommonRequestQueueConsumer;
import ru.yandex.market.ff.model.dbqueue.ValidateRequestPayload;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class OrdersSupplyQueueConsumerTest extends IntegrationTestWithDbQueueConsumers {

    private static final Address ADDRESS = Address.newBuilder()
        .settlement("Котельники")
        .street("Яничкин проезд")
        .house("7")
        .comment("терминал БД-6")
        .build();
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ValidateCommonRequestQueueConsumer consumer;

    // заявка 10-го типа сваливается в 5-ый статус если лог. точка не найдена
    @Test
    @DatabaseSetup({"classpath:service/shop-request-validation/fulfillment-service.xml",
            "classpath:service/shop-request-validation/logistics-point-empty.xml",
            "classpath:service/shop-request-validation/before-orders-supply.xml"})
    @ExpectedDatabase(value = "classpath:service/shop-request-validation/after-orders-supply-invalid.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void ordersSupplyInvalidBecauseLogisticsPointNotFound() {
        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }

    // заявка 10-го типа успешно проходит валидацию если лог. точка уже прикопана
    @Test
    @DatabaseSetup({"classpath:service/shop-request-validation/fulfillment-service.xml",
            "classpath:service/shop-request-validation/logistics-point-found.xml",
            "classpath:service/shop-request-validation/before-orders-supply.xml"})
    @ExpectedDatabase(value = "classpath:service/shop-request-validation/after-orders-supply-valid.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void ordersSupplyValidLogisticsPointFoundLocally() {
        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }

    // заявка 10-го типа успешно проходит валидацию если есть лог. точка в ЛМС, прикапываем (апдейтим)
    @Test
    @DatabaseSetup({"classpath:service/shop-request-validation/fulfillment-service.xml",
            "classpath:service/shop-request-validation/logistics-point-empty.xml",
            "classpath:service/shop-request-validation/before-orders-supply.xml"})
    @ExpectedDatabase(value = "classpath:service/shop-request-validation/after-orders-supply-valid.xml",
            assertionMode = NON_STRICT_UNORDERED)
    @ExpectedDatabase(value = "classpath:service/shop-request-validation/logistics-point-found.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void ordersSupplyValidLogisticsPointFoundRemotely() {
        LogisticsPointResponse response = LogisticsPointResponse.newBuilder()
                .active(true)
                .address(ADDRESS)
                .externalId("a-b1")
                .partnerId(100L)
                .build();
        when(lmsClient.getLogisticsPoint(12341L)).thenReturn(Optional.of(response));
        TaskExecutionResult result = executeTask();
        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }

    private TaskExecutionResult executeTask() {
        ValidateRequestPayload payload = new ValidateRequestPayload(1);
        Task<ValidateRequestPayload> task = new Task<>(new QueueShardId("shard"), payload, 0,
                ZonedDateTime.now(ZoneId.systemDefault()), null, null);
        return transactionTemplate.execute(status -> consumer.execute(task));
    }

}
