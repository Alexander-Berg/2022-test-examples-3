package ru.yandex.market.fulfillment.stockstorage;

import java.util.ArrayList;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.fulfillment.stockstorage.configuration.AsyncTestConfiguration;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.service.export.rty.availability.SkuChangeAvailabilityMessageProducer;
import ru.yandex.market.fulfillment.stockstorage.util.AsyncWaiterService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

@Import(AsyncTestConfiguration.class)
@DatabaseSetup("classpath:database/states/system_property.xml")
public class SkuChangeAvailabilityMessageProducerTest extends AbstractContextualTest {

    @Autowired
    private SkuChangeAvailabilityMessageProducer changeAvailabilityProducer;

    @Autowired
    private AsyncWaiterService asyncWaiterService;

    /**
     * Тест №1: пуш батчем из 2-х элементов при пустой execution_queue.
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_message_producer/1/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushBatchOnEmptyQueue() {
        UnitId unitId1 = new UnitId("sku1", 35947L, 147);
        UnitId unitId2 = new UnitId("sku2", 11111L, 145);
        ArrayList<UnitId> batch = Lists.newArrayList(unitId1, unitId2);

        changeAvailabilityProducer.produceIfNecessaryAsync(batch);
        asyncWaiterService.awaitTasks();
    }

    /**
     * Тест №2: пуш одного элемента при пустой execution_queue.
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_message_producer/2/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushOnEmptyQueue() {
        UnitId unitId1 = new UnitId("sku1", 35947L, 147);
        ArrayList<UnitId> batch = Lists.newArrayList(unitId1, unitId1);

        changeAvailabilityProducer.produceIfNecessaryAsync(batch);
        asyncWaiterService.awaitTasks();
    }

    /**
     * Тест №3: пуш батчем из 2-х элементов при непустой execution_queue.
     * <p>
     * Проверяем, что уже существующая запись не будет заменена.
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/change_availability_message_producer/3/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_message_producer/3/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushBatchOnNonEmptyQueue() {
        UnitId unitId1 = new UnitId("sku1", 35947L, 147);
        UnitId unitId2 = new UnitId("sku2", 11111L, 145);
        ArrayList<UnitId> batch = Lists.newArrayList(unitId1, unitId2);

        changeAvailabilityProducer.produceIfNecessaryAsync(batch);
        asyncWaiterService.awaitTasks();
    }

    /**
     * Тест №4: пуш элемента, который уже содержится в execution_queue.
     * <p>
     * Проверяем, что уже существующая запись не будет заменена.
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/change_availability_message_producer/4/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_message_producer/4/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushOnNonEmptyQueue() {
        UnitId unitId1 = new UnitId("sku1", 35947L, 147);
        ArrayList<UnitId> batch = Lists.newArrayList(unitId1, unitId1);

        changeAvailabilityProducer.produceIfNecessaryAsync(batch);
        asyncWaiterService.awaitTasks();
    }

    /**
     * Тест №5: проверка корректности пуша пустого батча
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/change_availability_message_producer/4/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_message_producer/4/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushEmptyBatch() {
        changeAvailabilityProducer.produceIfNecessaryAsync(Collections.emptyList());
    }


    /**
     * Тест №6: проверка корректности пуша по складам как с дефолтной стратегией, так и со стратегией doNothing.
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/change_availability_message_producer/6/warehouse_property.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_message_producer/6/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushBatchWithDefaultAndDoNothingStrategies() {
        UnitId unitId1 = new UnitId("sku1", 47L, 147);
        UnitId unitId2 = new UnitId("sku2", 47L, 163);
        UnitId unitId3 = new UnitId("sku3", 42L, 145);

        changeAvailabilityProducer.produceIfNecessaryAsync(ImmutableList.of(unitId1, unitId2, unitId3));
        asyncWaiterService.awaitTasks();
    }

    /**
     * Тест №7: проверка корректности пуша по складам только со стратегией doNothing.
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/change_availability_message_producer/7/warehouse_property.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_message_producer/7/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushBatchWithDoNothingStrategy() {
        UnitId unitId1 = new UnitId("sku1", 47L, 162);
        UnitId unitId2 = new UnitId("sku2", 47L, 163);
        UnitId unitId3 = new UnitId("sku3", 42L, 161);

        changeAvailabilityProducer.produceIfNecessaryAsync(ImmutableList.of(unitId1, unitId2, unitId3));
        asyncWaiterService.awaitTasks();
    }

    /**
     * Тест №8: проверка, что при ENABLE_RTY_NOTIFICATIONS_ABOUT_SKU_AVAILABILITY_CHANGING=false
     * ничего не попадет в очередь
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/change_availability_message_producer/8/warehouse_property.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_message_producer/8/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushBatchWithDisabledStocksAmountNotification() {
        UnitId unitId1 = new UnitId("sku1", 47L, 147);
        UnitId unitId2 = new UnitId("sku2", 47L, 163);
        UnitId unitId3 = new UnitId("sku3", 42L, 145);

        changeAvailabilityProducer.produceIfNecessaryAsync(ImmutableList.of(unitId1, unitId2, unitId3));
        asyncWaiterService.awaitTasks();
    }
}
