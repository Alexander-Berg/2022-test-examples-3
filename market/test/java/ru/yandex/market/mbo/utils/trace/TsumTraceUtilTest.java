package ru.yandex.market.mbo.utils.trace;

import org.apache.http.HttpException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;
import ru.yandex.market.request.trace.RequestLogRecordBuilder;
import ru.yandex.market.request.trace.RequestLogRecordEnricher;
import ru.yandex.market.request.trace.RequestTraceUtil;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(MockitoJUnitRunner.class)
public class TsumTraceUtilTest {
    RequestLogRecordEnricher logRecordEnricher = Mockito.mock(RequestLogRecordEnricher.class);

    @Captor
    ArgumentCaptor<RequestLogRecordBuilder> recordCaptor;

    @Before
    public void setUp() {
        RequestTraceUtil.addRecordEnricher(logRecordEnricher);
    }

    @Test
    public void trace() throws HttpException, IOException {
        RequestContextHolder.createNewContext();
        AtomicBoolean hasInvocation = new AtomicBoolean(false);

        TsumTraceUtil.trace(Module.PGAAS, "testOperation", () -> hasInvocation.set(true));

        Assert.assertTrue(hasInvocation.get());
        Mockito.verify(logRecordEnricher).enrich(recordCaptor.capture());
        String tskvRecord = recordCaptor.getValue().build();
        Assert.assertTrue(tskvRecord.contains("protocol=http"));
        Assert.assertTrue(tskvRecord.contains("http_method=GET"));
        Assert.assertTrue(tskvRecord.contains("target_module=pgaas"));
        Assert.assertTrue(tskvRecord.contains("request_method=/execute"));
        Assert.assertTrue(tskvRecord.contains("query_params=/execute?operation=testOperation"));
        return;
    }
}
