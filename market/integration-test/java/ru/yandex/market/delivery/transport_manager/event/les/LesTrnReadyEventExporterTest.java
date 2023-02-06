package ru.yandex.market.delivery.transport_manager.event.les;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.service.PartnerInfoService;
import ru.yandex.market.delivery.transport_manager.service.les.exporter.LesTrnReadyEventExporter;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.tm.TrnReadyEvent;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

public class LesTrnReadyEventExporterTest extends AbstractContextualTest {

    @Autowired
    private PartnerInfoService partnerInfoService;

    @Autowired
    private LesTrnReadyEventExporter lesTrnReadyEventExporter;

    @Autowired
    private LesProducer lesProducer;

    @Autowired
    private DataFieldMaxValueIncrementer dataFieldMaxValueIncrementer;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2022-01-21T10:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    @DatabaseSetup(value = "/repository/trn/register_and_unit.xml")
    void testSenderCall() {
        Mockito.when(dataFieldMaxValueIncrementer.nextLongValue()).thenReturn(1L);
        Mockito.doReturn(PartnerType.FULFILLMENT).when(partnerInfoService)
            .getOutboundPartnerType(2L, 123L);

        var docs = List.of("path");
        lesTrnReadyEventExporter.send(123L, "ololo", docs);

        EventPayload eventPayload = new TrnReadyEvent(123L, "ololo", docs);

        Event event = new Event(
            "tm",
            "1",
            Instant.now(clock).toEpochMilli(),
            "TRN_DOCUMENTS_READY",
            eventPayload,
            "ТрН были загружены в s3"
        );
        Mockito.verify(lesProducer).send(event, "tm_out");
    }
}
