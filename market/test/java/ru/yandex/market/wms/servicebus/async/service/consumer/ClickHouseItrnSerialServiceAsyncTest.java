package ru.yandex.market.wms.servicebus.async.service.consumer;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.core.JmsTemplate;

import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.async.dto.ItrnSerialDto;
import ru.yandex.market.wms.servicebus.repository.ClickHouseItrnSerialDaoTemplate;
import ru.yandex.market.wms.shared.libs.async.jms.QueueNameConstants;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

public class ClickHouseItrnSerialServiceAsyncTest extends IntegrationTest {

    @MockBean
    @Autowired
    private ClickHouseItrnSerialDaoTemplate daoTemplate;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    @SpyBean
    private ClickHouseItrnSerialServiceAsync clickHouseItrnSerialServiceAsync;

    @AfterEach
    public void resetMocks() {
        Mockito.reset(daoTemplate, clickHouseItrnSerialServiceAsync);
    }

    @Test
    public void whenSendMessageThenDoInsert() {

        doNothing().when(daoTemplate).insert(any());

        final ItrnSerialDto itrnSerial = ItrnSerialDto.builder()
                .itrnSerialKey("0000000401")
                .itrnKey("0000000301")
                .storerKey("STORER")
                .sku("SKU")
                .lot("LOT01")
                .id("DRP0000001")
                .loc("PACK")
                .serialNumber("0010000001")
                .qty(BigDecimal.valueOf(1))
                .tranType("MV")
                .addWho("TEST")
                .editWho("TEST")
                .build();

        jmsTemplate.convertAndSend(QueueNameConstants.CREATE_ITRN_SERIAL, itrnSerial);

        Mockito.verify(daoTemplate, Mockito.timeout(1000).times(1)).insert(itrnSerial);
    }

    @Test
    public void whenSendMessageThenDoUpdate() {

        doNothing().when(daoTemplate).update(any());

        final ItrnSerialDto itrnSerial = ItrnSerialDto.builder()
                .itrnSerialKey("0000000401")
                .itrnKey("0000000301")
                .storerKey("STORER")
                .sku("SKU")
                .lot("LOT01")
                .id("DRP0000001")
                .loc("PACK")
                .serialNumber("0010000001")
                .qty(BigDecimal.valueOf(1))
                .tranType("MV")
                .addWho("TEST")
                .editWho("TEST")
                .build();

        jmsTemplate.convertAndSend(QueueNameConstants.UPDATE_ITRN_SERIAL, itrnSerial);

        Mockito.verify(daoTemplate, Mockito.timeout(1000).times(1)).update(itrnSerial);
    }

    @Test
    public void whenSendMessageIsFailThenRetryInsert() {

        doThrow(RuntimeException.class).doCallRealMethod().when(daoTemplate).insert(any());
        doNothing().when(daoTemplate).update(any());

        final ItrnSerialDto itrnSerial = ItrnSerialDto.builder()
                .itrnSerialKey("0000000401")
                .itrnKey("0000000301")
                .storerKey("STORER")
                .sku("SKU")
                .lot("LOT01")
                .id("DRP0000001")
                .loc("PACK")
                .serialNumber("0010000001")
                .qty(BigDecimal.valueOf(1))
                .tranType("MV")
                .addWho("TEST")
                .editWho("TEST")
                .build();

        jmsTemplate.convertAndSend(QueueNameConstants.CREATE_ITRN_SERIAL, itrnSerial);
        Mockito.verify(daoTemplate, Mockito.timeout(1000).times(1)).insert(itrnSerial);
    }

    @Test
    public void whenSendMessageIsFailThenRetryUpdate() {

        doThrow(RuntimeException.class).doCallRealMethod().when(daoTemplate).update(any());
        doNothing().when(daoTemplate).update(any());

        final ItrnSerialDto itrnSerial = ItrnSerialDto.builder()
                .itrnSerialKey("0000000401")
                .itrnKey("0000000301")
                .storerKey("STORER")
                .sku("SKU")
                .lot("LOT01")
                .id("DRP0000001")
                .loc("PACK")
                .serialNumber("0010000001")
                .qty(BigDecimal.valueOf(1))
                .tranType("MV")
                .addWho("TEST")
                .editWho("TEST")
                .build();

        jmsTemplate.convertAndSend(QueueNameConstants.UPDATE_ITRN_SERIAL, itrnSerial);
        Mockito.verify(daoTemplate, Mockito.timeout(1000).times(1)).update(itrnSerial);
    }
}
