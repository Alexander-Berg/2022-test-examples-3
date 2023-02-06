package ru.yandex.market.delivery.transport_manager.service.xdoc;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.logbroker.LogbrokerProducerProperties;
import ru.yandex.market.delivery.transport_manager.domain.dto.CalendaringStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.tag.TagCode;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

@Slf4j
class XDocSlotMetaUpdateLogbrokerWriterTest extends AbstractContextualTest {
    @Autowired
    private XDocSlotMetaUpdateLogbrokerWriter xDocSlotMetaUpdateLogbrokerWriter;

    @Autowired
    private LogbrokerClientFactory logbrokerClientFactory;

    @Autowired
    @Qualifier("csMetaUpdateProducerProperties")
    private LogbrokerProducerProperties csMetaUpdateProducerProperties;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2020-08-24T09:00:00.00Z"), ZoneOffset.UTC);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport_with_slot.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/transportation/xdoc_to_ff_transportations.xml",
        "/repository/transportation/xdoc_to_dc_transportations.xml",
    })
    @DatabaseSetup(
        value = "/repository/transportation/update/set_null_subtype.xml",
        type = DatabaseOperation.UPDATE)
    @Test
    void write() throws InterruptedException, ExecutionException, TimeoutException {
        AsyncProducer asyncProducer = Mockito.mock(AsyncProducer.class);
        CompletableFuture<ProducerInitResponse> init = Mockito.mock(CompletableFuture.class);

        Mockito.when(csMetaUpdateProducerProperties.isEnabled()).thenReturn(true);
        Mockito.when(logbrokerClientFactory.asyncProducer(Mockito.any()))
            .thenReturn(asyncProducer);
        Mockito.when(asyncProducer.init()).thenReturn(init);
        Mockito.when(init.get(Mockito.anyLong(), Mockito.any())).thenReturn(new ProducerInitResponse(
            1, "", 1, ""
        ));

        xDocSlotMetaUpdateLogbrokerWriter.computeAndWriteSlotMetaChanges(
            11L,
            TagCode.FFWF_INCLUDED_REQUEST_ID_PLAN,
            TransportationUnitType.OUTBOUND,
            CalendaringStatus.ACCEPTED_BY_XDOC_SERVICE
        );

        Mockito.verify(logbrokerClientFactory).asyncProducer(Mockito.any());
        Mockito.verify(asyncProducer)
            .write(Mockito.argThat(new JsonArgumentMatcher(IntegrationTestUtils.extractFileContent(
                "logbroker/x-dock-slot-meta-update-message.json"
            ))));
        Mockito.verify(asyncProducer).init();
        Mockito.verify(init).get(Mockito.anyLong(), Mockito.any());
        Mockito.verify(asyncProducer).close();
        Mockito.verifyNoMoreInteractions(logbrokerClientFactory, asyncProducer);
    }

    @DatabaseSetup({
        "/repository/transportation/xdoc_transport_with_slot.xml",
        "/repository/tag/xdoc_transport_plan.xml",
        "/repository/transportation/xdoc_to_ff_transportations_break_bulk_xdock.xml",
    })
    @Test
    void writeBreakBulkXdock() throws InterruptedException, ExecutionException, TimeoutException {
        AsyncProducer asyncProducer = Mockito.mock(AsyncProducer.class);
        CompletableFuture<ProducerInitResponse> init = Mockito.mock(CompletableFuture.class);

        Mockito.when(csMetaUpdateProducerProperties.isEnabled()).thenReturn(true);
        Mockito.when(logbrokerClientFactory.asyncProducer(Mockito.any()))
            .thenReturn(asyncProducer);
        Mockito.when(asyncProducer.init()).thenReturn(init);
        Mockito.when(init.get(Mockito.anyLong(), Mockito.any())).thenReturn(new ProducerInitResponse(
            1, "", 1, ""
        ));

        xDocSlotMetaUpdateLogbrokerWriter.computeAndWriteSlotMetaChanges(
            11L,
            TagCode.FFWF_INCLUDED_REQUEST_ID_PLAN,
            TransportationUnitType.OUTBOUND,
            CalendaringStatus.ACCEPTED_BY_XDOC_SERVICE
        );

        Mockito.verify(logbrokerClientFactory).asyncProducer(Mockito.any());
        Mockito.verify(asyncProducer)
            .write(Mockito.argThat(new JsonArgumentMatcher(IntegrationTestUtils.extractFileContent(
                "logbroker/x-dock-slot-meta-update-message-break-bulk-xdock.json"
            ))));
        Mockito.verify(asyncProducer).init();
        Mockito.verify(init).get(Mockito.anyLong(), Mockito.any());
        Mockito.verify(asyncProducer).close();
        Mockito.verifyNoMoreInteractions(logbrokerClientFactory, asyncProducer);
    }

    @Value
    private static class JsonArgumentMatcher implements ArgumentMatcher<byte[]> {
        String expected;

        @SneakyThrows
        @Override
        public boolean matches(byte[] argument) {
            String actual = new String(argument);
            JSONCompareResult result = JSONCompare.compareJSON(expected, actual, JSONCompareMode.STRICT);
            if (result.failed()) {
                log.error(result.getMessage());
            }
            return !result.failed();
        }
    }
}
