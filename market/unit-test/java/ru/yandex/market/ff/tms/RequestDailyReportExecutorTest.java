package ru.yandex.market.ff.tms;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.dbqueue.producer.RequestReportByWarehouseProducer;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.PechkinNotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestDailyReportExecutorTest {


    @Test
    public void test() {
        RequestReportByWarehouseProducer producer = mock(RequestReportByWarehouseProducer.class);
        TransactionTemplate tx = mock(TransactionTemplate.class);
        ExecutorService es = mock(ExecutorService.class);
        ConcreteEnvironmentParamService params = mock(ConcreteEnvironmentParamService.class);
        PechkinNotificationService notificationService = mock(PechkinNotificationService.class);

        when(params.getRequestDailyReportWarehouseIds())
                .thenReturn(Set.of(172L));
        when(params.getRequestDailyReportTelegramChannelName())
                .thenReturn("myChannel");
        when(params.getRequestDailyReportTelegramHeadMessageTemplate())
                .thenReturn("Loren Ipsum... _{{date}}_");

        when(tx.execute(any())).then(invocation -> {
            TransactionCallback<Integer> cb = invocation.getArgument(0);
            cb.doInTransaction(mock(TransactionStatus.class));
            return null;
        });

        when(es.submit(any(Runnable.class))).then(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        });

        RequestDailyReportExecutor reportExecutor = new RequestDailyReportExecutor(
                producer, tx, es, params, notificationService);

        reportExecutor.doJob(mock(JobExecutionContext.class));

        verify(producer, times(2)).produceSingle(any());


    }
}
