package ru.yandex.market.wms.servicebus.async.service;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.tts.TtsWebClient;
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants;
import ru.yandex.market.wms.shared.libs.employee.perfomance.model.ScanningOperationDto;

public class ScanningOperationServiceTest extends IntegrationTest {
    @Autowired
    private JmsTemplate jmsTemplate;

    @MockBean
    private TtsWebClient ttsWebClient;

    @Autowired
    @SpyBean
    @InjectMocks
    private ScanningOperationService scanningOperationService;

    @Test
    public void checkDeserialization() {
        var dto = ScanningOperationDto.builder()
                .warehouse("sof")
                .env("test")
                .user("user1")
                .operationType("operation1")
                .operationDay(LocalDate.of(2022, 04, 26))
                .operationDateTime(Instant.parse("2022-04-26T05:00:00Z"))
                .sku("sku1")
                .storerKey("storerKey1")
                .lot("lot1")
                .qty(10)
                .fromLoc("fromLoc1")
                .toLoc("toLoc1")
                .fromId("fromId1")
                .toId("toId1")
                .sourceKey("sourceKey1")
                .build();

        jmsTemplate.sendAndReceive(QueueNameConstants.SCANNING_OPERATION,
                session -> jmsTemplate.getMessageConverter().toMessage(dto, session));

        Mockito.verify(scanningOperationService).consumeScanningOperation(Mockito.eq(dto), Mockito.any());
    }
}
