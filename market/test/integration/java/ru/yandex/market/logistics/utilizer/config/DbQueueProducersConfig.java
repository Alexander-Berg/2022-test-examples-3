package ru.yandex.market.logistics.utilizer.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.utilizer.dbqueue.task.events.SendUtilizationCycleFinalizationEmailProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.callticket.CallTicketDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.closing.AutoCloseDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.stockevent.SkuStocksEventDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.stockevent.UnparsedSkuStocksFromLbDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueueProducer;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.withdraw.WithdrawFileDbqueueProducer;

@Configuration
public class DbQueueProducersConfig {

    @Bean
    public SkuStocksEventDbqueueProducer skuStocksEventDbqueueProducer() {
        return Mockito.mock(SkuStocksEventDbqueueProducer.class);
    }

    @Bean
    public UnparsedSkuStocksFromLbDbqueueProducer unparsedSkuStocksFromLbDbqueueProducer() {
        return Mockito.mock(UnparsedSkuStocksFromLbDbqueueProducer.class);
    }

    @Bean
    public CreateTransferDbqueueProducer createTransferDbqueueProducer() {
        return Mockito.mock(CreateTransferDbqueueProducer.class);
    }

    @Bean
    public SendUtilizationCycleFinalizationEmailProducer sendUtilizationCycleFinalizationEmailProducer() {
        return Mockito.mock(SendUtilizationCycleFinalizationEmailProducer.class);
    }

    @Bean
    public CallTicketDbqueueProducer callTicketDbqueueProducer() {
        return Mockito.mock(CallTicketDbqueueProducer.class);
    }

    @Bean
    public WithdrawFileDbqueueProducer createWithdrawFileDbqueueProducer() {
        return Mockito.mock(WithdrawFileDbqueueProducer.class);
    }

    @Bean
    public AutoCloseDbqueueProducer autoCloseDbqueueProducer() {
        return Mockito.mock(AutoCloseDbqueueProducer.class);
    }
}
