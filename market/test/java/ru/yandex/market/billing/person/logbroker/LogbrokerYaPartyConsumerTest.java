package ru.yandex.market.billing.person.logbroker;

import java.io.IOException;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logbroker.consumer.LogbrokerDataProcessor;

/**
 * Тест на обработку события из LB топика yaparty
 */
@ExtendWith(MockitoExtension.class)
public class LogbrokerYaPartyConsumerTest extends FunctionalTest {

    @Mock
    private MessageBatch messageBatchMock;
    @Mock
    private MessageData messageDataMock;

    @Autowired
    private LogbrokerDataProcessor logbrokerYaPartyConsumer;

    @BeforeEach
    void setup() {
        Mockito.when(messageBatchMock.getMessageData())
                .thenReturn(List.of(messageDataMock));
    }

    @Test
    @DisplayName("Положительный тест на обработку события об ИП")
    @DbUnitDataSet(after = "db/LogbrokerPersonProcessorTest.readIndividualEvent.after.csv")
    void readLogbrokerIndividualEventTest() {
        setupMock("customer-individual-event.json");
        logbrokerYaPartyConsumer.process(messageBatchMock);
    }

    @Test
    @DisplayName("Положительный тест на обработку события о самозанятом")
    @DbUnitDataSet(after = "db/LogbrokerPersonProcessorTest.readSelfemployedEvent.after.csv")
    void readLogbrokerSelfemployedEventTest() {
        setupMock("customer-selfemployed-event.json");
        logbrokerYaPartyConsumer.process(messageBatchMock);
    }

    @Test
    @DisplayName("Положительный тест на обработку события об организации")
    @DbUnitDataSet(after = "db/LogbrokerPersonProcessorTest.readOrganizationEvent.after.csv")
    void readLogbrokerOrganizationEventTest() {
        setupMock("customer-organization-event.json");
        logbrokerYaPartyConsumer.process(messageBatchMock);
    }

    @Test
    @DisplayName("Отрицательный тест на обработку события не об организации")
    @DbUnitDataSet(after = "db/LogbrokerPersonProcessorTest.empty.after.csv")
    void readLogbrokerContractEventTest() {
        setupMock("contract-event.json");
        logbrokerYaPartyConsumer.process(messageBatchMock);
    }

    @Test
    @DisplayName("Отрицательный тест на обработку события о физ. лице")
    @DbUnitDataSet(after = "db/LogbrokerPersonProcessorTest.empty.after.csv")
    void readLogbrokerPersonEventTest() {
        setupMock("customer-person-event.json");
        logbrokerYaPartyConsumer.process(messageBatchMock);
    }

    @Test
    @DisplayName("Положительный тест на обработку события об существующей организации")
    @DbUnitDataSet(
            before = "db/LogbrokerPersonProcessorTest.readExistingOrganizationEvent.before.csv",
            after = "db/LogbrokerPersonProcessorTest.readExistingOrganizationEvent.after.csv")
    void readLogbrokerExistingOrganizationEventTest() {
        setupMock("customer-organization-event.json");
        logbrokerYaPartyConsumer.process(messageBatchMock);
    }

    @Test
    @DisplayName("Положительный тест на обработку события об существующей непроверенной организации")
    @DbUnitDataSet(
            before = "db/LogbrokerPersonProcessorTest.readExistingUncheckedOrganizationEvent.before.csv",
            after = "db/LogbrokerPersonProcessorTest.readExistingUncheckedOrganizationEvent.after.csv")
    void readLogbrokerExistingUncheckedOrganizationEventTest() {
        setupMock("customer-organization-event.json");
        logbrokerYaPartyConsumer.process(messageBatchMock);
    }

    @Test
    @DisplayName("Тест на обработку события об существующей организации без изменения ключевой информации")
    @DbUnitDataSet(
            before = "db/LogbrokerPersonProcessorTest.readExistingOrganizationEventWoCheck.before.csv",
            after = "db/LogbrokerPersonProcessorTest.readExistingOrganizationEventWoCheck.after.csv")
    void readLogbrokerExistingOrganizationWoCheckEventTest() {
        setupMock("customer-organization-event.json");
        logbrokerYaPartyConsumer.process(messageBatchMock);
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

        Mockito.when(messageDataMock.getRawData())
                .thenReturn(orderEventJson);
    }
}
