package ru.yandex.market.fulfillment.stockstorage;


import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.service.stocks.unfreezing.ForceUnfreezeStockConsumer;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.unfreezing.ForceUnfreezeStockProducer;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ForceUnfreezeStockJobTest extends AbstractContextualTest {

    @Autowired
    private ForceUnfreezeStockConsumer consumer;

    @Autowired
    private ForceUnfreezeStockProducer producer;

    @Test
    @DatabaseSetup("classpath:database/states/order_service/1/before_force_unfreeze.xml")
    @ExpectedDatabase(value = "classpath:database/states/order_service/1/after_force_unfreeze.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void forceUnfreezeTest() {
        consumer.consume();
    }

    /**
     * На заказ 123456 есть 3 записи в stock_freeze, при этом для 1 записи есть анфриз джоба,
     * для 1 записи нет анфриз джобы, а еще один фриз удален (deleted = true и есть исполненная анфриз джоба).
     * В результате должна исполниться анфриз джоба по первой записи, исполниться анфриз по второй записи с генерацией
     * анфриз джобы и ничего не должно произойти с третьей записью и ее стоком.
     */
    @Test
    @DatabaseSetup("classpath:database/states/order_service/4/before_force_unfreeze_partially_deleted_freeze.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/order_service/4/after_force_unfreeze_partially_deleted_freeze.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void forceUnfreezeTestForPartiallyDeletedFreeze() {
        consumer.consume();
    }

    /**
     * На заказ 123456 есть 3 записи в stock_freeze и фризы для всех уже удалены.
     * В результате исполнения ничего не должно измениться в фризах и стоках.
     */
    @Test
    @DatabaseSetup("classpath:database/states/order_service/5/before_force_unfreeze_fully_deleted_freeze.xml")
    @ExpectedDatabase(
            value = "classpath:database/states/order_service/5/after_force_unfreeze_fully_deleted_freeze.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void forceUnfreezeTestForFullyDeletedFreeze() {
        consumer.consume();
    }

    @Test
    @ExpectedDatabase(value = "classpath:database/states/order_service/2/after_force_unfreeze.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void successInsertProduceForceUnfreezeTask() {
        producer.push("123456", "comment");
    }

    @Test
    @DatabaseSetup("classpath:database/states/order_service/3/before_force_unfreeze.xml")
    @ExpectedDatabase(value = "classpath:database/states/order_service/3/after_force_unfreeze.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void insertDuplicateProduceForceUnfreezeTask() {
        producer.push("123456", "comment");
    }

}
