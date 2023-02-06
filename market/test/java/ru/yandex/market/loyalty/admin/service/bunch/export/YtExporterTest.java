package ru.yandex.market.loyalty.admin.service.bunch.export;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestException;
import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestSubstatusHandler;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestDao;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName;
import ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.admin.service.bunch.export.YtExporter.YtExporterState.COMPLETE;
import static ru.yandex.market.loyalty.admin.service.bunch.export.YtExporter.YtExporterState.DATA_START;
import static ru.yandex.market.loyalty.admin.service.bunch.export.YtExporter.YtExporterState.ERRORS_START;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.NO_AUTH;
import static ru.yandex.market.loyalty.api.model.TableFormat.CSV;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestParamName.COIN_GENERATOR_TYPE;
import static ru.yandex.market.loyalty.core.dao.coin.BunchGenerationRequestStatus.PREPARED;

public class YtExporterTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private YtExporter ytExporter;
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    private BunchGenerationRequestDao bunchRequestDao;

    @Test
    public void test() throws BunchRequestException {
        final BunchRequestSubstatusHandler<YtExporter.YtExporterState> dummyHandler = handlerDummy();

        final long requestId = createSomeCoinRequest();

        bunchRequestDao.markRequestPrepared(requestId);

        exportPreparedCoins(requestId, dummyHandler);

        Mockito.verify(dummyHandler).accept(eq(DATA_START), any(BunchGenerationRequest.class));
        Mockito.verify(dummyHandler).accept(eq(ERRORS_START), any(BunchGenerationRequest.class));
        Mockito.verify(dummyHandler).accept(eq(COMPLETE), any(BunchGenerationRequest.class));

        assertStatus(requestId, PREPARED, COMPLETE);
    }

    @Test
    public void testResume() throws BunchRequestException {
        final long requestId = createSomeCoinRequest();

        bunchRequestDao.markRequestPrepared(requestId);

        final BunchRequestSubstatusHandler<YtExporter.YtExporterState> errorHandler =
                handlerThrowingError(ERRORS_START);
        exportPreparedCoins(requestId, errorHandler);

        Mockito.verify(errorHandler).accept(eq(DATA_START), any(BunchGenerationRequest.class));
        Mockito.verify(errorHandler).accept(eq(ERRORS_START), any(BunchGenerationRequest.class));
        Mockito.verifyNoMoreInteractions(errorHandler);
        assertStatus(requestId, PREPARED, ERRORS_START);

        final BunchRequestSubstatusHandler<YtExporter.YtExporterState> dummyHandler = handlerDummy();
        exportPreparedCoins(requestId, dummyHandler);

        Mockito.verify(dummyHandler).accept(eq(ERRORS_START), any(BunchGenerationRequest.class));
        Mockito.verify(dummyHandler).accept(eq(COMPLETE), any(BunchGenerationRequest.class));
        Mockito.verifyNoMoreInteractions(dummyHandler);

        assertStatus(requestId, PREPARED, COMPLETE);
    }

    private void assertStatus(
            long requestId, BunchGenerationRequestStatus status, YtExporter.YtExporterState substatus
    ) {
        assertEquals(
                status,
                bunchRequestService.getRequest(requestId).getStatus()
        );
        assertEquals(
                substatus,
                YtExporter.YtExporterState.valueOf(bunchRequestDao.getRequestSubstatus(requestId, PREPARED))
        );
    }

    @SuppressWarnings("unchecked")
    private BunchRequestSubstatusHandler<YtExporter.YtExporterState> handlerThrowingError(YtExporter.YtExporterState problemState) throws BunchRequestException {
        final BunchRequestSubstatusHandler<YtExporter.YtExporterState> mock =
                Mockito.mock(BunchRequestSubstatusHandler.class);
        Mockito.doThrow(new HandlerException()).when(mock).accept(Mockito.eq(problemState), any());
        for (YtExporter.YtExporterState s : YtExporter.YtExporterState.values()) {
            if (s == problemState) {
                continue;
            }
            when(mock.accept(eq(s), any())).thenReturn(true);
        }

        return mock;
    }

    @SuppressWarnings("unchecked")
    private BunchRequestSubstatusHandler<YtExporter.YtExporterState> handlerDummy() throws BunchRequestException {
        final BunchRequestSubstatusHandler mock = Mockito.mock(BunchRequestSubstatusHandler.class);
        when(mock.accept(any(), any())).thenReturn(true);
        return mock;
    }

    private BunchRequestSubstatusHandler<YtExporter.YtExporterState> exportPreparedCoins(long requestId,
                                                                                         BunchRequestSubstatusHandler<YtExporter.YtExporterState> handler) throws BunchRequestException {
        try {
            ytExporter.exportPreparedCoins(
                    bunchRequestService.getRequest(requestId),
                    handler
            );
        } catch (HandlerException e) {

        }

        return handler;
    }

    private static class HandlerException extends RuntimeException {

    }

    private long createSomeCoinRequest() {
        final BunchGenerationRequest request = BunchGenerationRequest.scheduled(
                0L,
                "coin",
                100,
                "mail@mail.mail",
                CSV,
                null,
                GeneratorType.COIN,
                ImmutableMap.<BunchGenerationRequestParamName<?>, String>builder()
                        .put(COIN_GENERATOR_TYPE, NO_AUTH.getCode())
                        .build()
        );
        return bunchRequestService.scheduleRequest(request);
    }

}
