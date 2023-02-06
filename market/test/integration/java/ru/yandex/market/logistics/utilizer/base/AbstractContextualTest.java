package ru.yandex.market.logistics.utilizer.base;

import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.logistics.utilizer.config.DbQueueProducersConfig;
import ru.yandex.market.logistics.utilizer.config.IntegrationTestConfig;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.SendUtilizationCycleFinalizationEmailProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.callticket.CallTicketDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.closing.AutoCloseDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.stockevent.SkuStocksEventDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.stockevent.UnparsedSkuStocksFromLbDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.withdraw.WithdrawFileDbqueueProducer;

@SpringBootTest(
        classes = {
                IntegrationTestConfig.class,
                DbQueueProducersConfig.class
        }
)
public abstract class AbstractContextualTest extends IntegrationTest {

    @Autowired
    protected SkuStocksEventDbqueueProducer skuStocksEventDbqueueProducer;

    @Autowired
    protected UnparsedSkuStocksFromLbDbqueueProducer unparsedSkuStocksFromLbDbqueueProducer;

    @Autowired
    protected CreateTransferDbqueueProducer createTransferDbqueueProducer;

    @Autowired
    protected SendUtilizationCycleFinalizationEmailProducer sendUtilizationCycleFinalizationEmailProducer;

    @Autowired
    protected CallTicketDbqueueProducer callTicketDbqueueProducer;

    @Autowired
    protected AutoCloseDbqueueProducer autoCloseDbqueueProducer;

    @Autowired
    protected WithdrawFileDbqueueProducer withdrawFileDbqueueProducer;

    @AfterEach
    public void resetMocks() {
        super.resetMocks();
        Mockito.reset(
                skuStocksEventDbqueueProducer,
                unparsedSkuStocksFromLbDbqueueProducer,
                createTransferDbqueueProducer,
                sendUtilizationCycleFinalizationEmailProducer,
                withdrawFileDbqueueProducer,
                callTicketDbqueueProducer,
                autoCloseDbqueueProducer
        );
    }
}
