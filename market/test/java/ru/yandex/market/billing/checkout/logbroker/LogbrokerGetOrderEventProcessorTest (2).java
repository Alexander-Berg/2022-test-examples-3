package ru.yandex.market.billing.checkout.logbroker;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.checkout.EventProcessorSupportFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = {"../db/cpa_order_status.csv", "../db/datasource.csv",
        "../db/deliveryTypes.csv", "db/LogbrokerGetOrderEventProcessorTest.csv"
})
class LogbrokerGetOrderEventProcessorTest extends FunctionalTest {

    @Mock
    private MessageBatch messageBatchMock;
    @Mock
    private MessageData messageDataMock;

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    private ObjectMapper objectMapper;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private EventProcessorSupportFactory supportFactory;

    private LogbrokerOrderEventsProcessor logbrokerOrderEventsDataProcessor;

    @BeforeEach
    void setup() {
        Mockito.when(messageBatchMock.getMessageData())
                .thenReturn(List.of(messageDataMock));
        logbrokerOrderEventsDataProcessor = new LogbrokerOrderEventsProcessor(
                objectMapper,
                environmentService,
                supportFactory
        );
        logbrokerOrderEventsDataProcessor.initProcessor();
    }

    @Test
    @DisplayName("???????? ???? ?????????????????? ?????????????? ???? LB ???????????? ??????????????????")
    @DbUnitDataSet(
            before = "db/LogbrokerGetOrderEventProcessorTest.readLogbrokerEvent.before.csv",
            after = "db/LogbrokerGetOrderEventProcessorTest.readLogbrokerEvent.after.csv"
    )
    void readLogbrokerEventTest() {
        setupMock("logbroker-new-order-event.json");
        logbrokerOrderEventsDataProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("???????? ???? ?????????????????? ?????????????? ???? LB ???????????? ?????????????????? c directShopInShop item-?????? (?? order.properties)")
    @DbUnitDataSet(
            before = "db/LogbrokerGetOrderEventProcessorTest.readLogbrokerEvent.before.csv",
            after = "db/LogbrokerGetOrderEventProcessorTest.readLbWithDSiSItems.after.csv"
    )
    void readLogbrokerEventWithDirectShopInShopItemsTest() {
        setupMock("lb-new-order-with-dsis-items.json");
        logbrokerOrderEventsDataProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("???????? ???? ?????????????????? ?????????????????? ????????????")
    @DbUnitDataSet(
            before = "db/LogbrokerGetOrderEventProcessorTest.saveArchivedEvent.before.csv",
            after = "db/LogbrokerGetOrderEventProcessorTest.saveArchivedEvent.after.csv"
    )
    void saveArchivedEventTest() {
        setupMock("lb-archived.json");
        logbrokerOrderEventsDataProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("???????? ???? ?????????????????? ???????????????????????? ????????????")
    @DbUnitDataSet(
            before = "db/LogbrokerGetOrderEventProcessorTest.saveDearchivedEvent.before.csv",
            after = "db/LogbrokerGetOrderEventProcessorTest.saveDearchivedEvent.after.csv"
    )
    void saveDearchivedEventTest() {
        setupMock("lb-dearchived.json");
        logbrokerOrderEventsDataProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("???????? ???? ?????????????? ?????????????? ?? ?????????????? ?????? ????????????????")
    @DbUnitDataSet(before = "db/LogbrokerGetOrderEventProcessorTest.skipExceptionOnParsingEvent.before.csv")
    void skipExceptionOnParsingEventTest() {
        setupMock("error-event.json");
        logbrokerOrderEventsDataProcessor.process(messageBatchMock);
    }

    @Test()
    @DisplayName("???????? ???? ???????????????????? ?????????????????? ??????????????")
    @DbUnitDataSet(before = "db/LogbrokerGetOrderEventProcessorTest.eventProcessingDisabled.before.csv")
    void eventProcessingIsDisabled() {
        Exception exception = assertThrows(
                IllegalStateException.class, () -> logbrokerOrderEventsDataProcessor.process(messageBatchMock)
        );
        String expectedMessage = "Event processing is disabled";
        String actualMessage = exception.getMessage();
        assertEquals(expectedMessage, actualMessage);
    }

    private void setupMock(String jsonEvent) {
        byte[] orderEventJson;
        try {
            orderEventJson = getClass()
                    .getResourceAsStream("json/" + jsonEvent)
                    .readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Mockito.when(messageDataMock.getDecompressedData())
                .thenReturn(orderEventJson);
    }
}
