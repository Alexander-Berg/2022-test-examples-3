package ru.yandex.market.ocrm.module.yadelivery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.module.metric.MetricsService;
import ru.yandex.market.jmf.script.ScriptValueStrategy;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;
import ru.yandex.market.logistics.lom.model.dto.EventDto;
import ru.yandex.market.logistics.lom.model.enums.DeliveryType;
import ru.yandex.market.logistics.lom.model.enums.PaymentMethod;
import ru.yandex.market.ocrm.module.yadelivery.domain.YaDeliveryOrder;
import ru.yandex.market.ocrm.module.yadelivery.impl.LomConsumerConfig;
import ru.yandex.market.ocrm.module.yadelivery.impl.LomMessageParser;
import ru.yandex.market.ocrm.module.yadelivery.impl.LomOrderEventsConsumer;
import ru.yandex.market.ocrm.module.yadelivery.impl.LomOrderEventsHandler;
import ru.yandex.market.ocrm.module.yadelivery.impl.LomScriptServiceApi;
import ru.yandex.market.ocrm.module.yadelivery.test.YaDeliveryTestUtils;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = ModuleYaDeliveryTestConfiguration.class)
public class LomConsumerTest {

    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private LomMessageParser messageParser;
    @Inject
    private LomOrderEventsHandler orderHandler;
    @Inject
    private YaDeliveryTestUtils testUtils;
    @Inject
    private TxService txService;
    @Inject
    private LomScriptServiceApi lomScriptServiceApi;
    @Inject
    private ScriptValueStrategy<JsonNode> jsonNodeScriptValueStrategy;
    @Inject
    private ObjectSerializeService objectSerializeService;

    private LomOrderEventsConsumer consumer;

    @BeforeAll
    public void setUp() {
        consumer = new LomOrderEventsConsumer(Mockito.mock(LomConsumerConfig.class), messageParser, orderHandler,
                Mockito.mock(MetricsService.class));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(lomScriptServiceApi);
    }

    @Test
    @Transactional
    public void editAfterDeleteItemsAndWaybillSegments() {
        var testCreateData = testUtils.testData("/lom_create.json");
        var testEditData = testUtils.testData("/lom_edit.json");

        prepareLomScriptApiMock(testCreateData, testEditData);
        consumer.accept(List.of(testCreateData, testEditData, testEditData));

        var gid = YaDeliveryOrder.gidOf("2020-03-31T21:21:21.212121Z", 29121987L);
        YaDeliveryOrder order = entityStorageService.get(gid);
        Assertions.assertNotNull(order);
    }

    @Test
    @Transactional
    public void createAndEdit() {
        final var gid = YaDeliveryOrder.gidOf("2020-03-31T21:21:21.212121Z", 29121988L);
        txService.runInNewTx(() -> {
            var testData = testUtils.testData("/lom_create_2.json");
            prepareLomScriptApiMock(testData);

            consumer.accept(List.of(testData));
        });

        txService.runInNewTx(() -> {
            YaDeliveryOrder order = entityStorageService.get(gid);
            Assertions.assertNotNull(order);

            assertOrder(order, OffsetDateTime.parse("2020-04-01T00:21:21.212+03:00"), "Создан");
            assertSender(order);
        });

        txService.runInNewTx(() -> {
            var testData = testUtils.testData("/lom_edit_skip.json");
            prepareLomScriptApiMock(testData);

            consumer.accept(List.of(testData));
        });

        txService.runInNewTx(() -> {
            YaDeliveryOrder order = entityStorageService.get(gid);
            Assertions.assertNotNull(order);

            assertOrder(order, OffsetDateTime.parse("2020-04-01T00:21:21.212+03:00"), "Создан");
        });

        txService.runInNewTx(() -> {
            var testData = testUtils.testData("/lom_edit_2.json");
            prepareLomScriptApiMock(testData);

            consumer.accept(List.of(testData));
        });

        txService.runInNewTx(() -> {
            YaDeliveryOrder order = entityStorageService.get(gid);
            Assertions.assertNotNull(order);

            assertOrder(order, OffsetDateTime.parse("2020-04-02T00:21:21.212+03:00"), "Получен трек от СД");
            assertSender(order, "test name2");
        });
    }

    private void assertOrder(@Nonnull YaDeliveryOrder order, OffsetDateTime updated, String status) {
        Assertions.assertEquals(
                OffsetDateTime.parse("2020-04-01T00:21:21.212+03:00").toInstant(),
                order.getCreated().toInstant()
        );
        Assertions.assertEquals(updated.toInstant(), order.getUpdated().toInstant());
        Assertions.assertEquals(Long.valueOf(29121988), order.getLomOrderId());
        Assertions.assertEquals("test id", order.getExternalId());
        Assertions.assertEquals("test barcode", order.getBarcode());
        Assertions.assertEquals(DeliveryType.PICKUP.name(), order.getDeliveryType().getCode());

        Assertions.assertEquals(LocalDate.parse("2020-04-10"), order.getDeliveryIntervalDateMin());
        Assertions.assertEquals(LocalDate.parse("2020-04-10"), order.getDeliveryIntervalDateMax());
        Assertions.assertEquals(LocalTime.parse("08:00"), order.getDeliveryIntervalStartTime());
        Assertions.assertEquals(LocalTime.parse("20:00"), order.getDeliveryIntervalEndTime());

        Assertions.assertEquals("test first name", order.getRecipientFirstName());
        Assertions.assertEquals("test last name", order.getRecipientLastName());
        Assertions.assertEquals("test middle name", order.getRecipientMiddleName());
        Assertions.assertEquals("test country", order.getRecipientCountry());
        Assertions.assertEquals("test federal district", order.getRecipientFederalDistrict());
        Assertions.assertEquals("test region", order.getRecipientRegion());
        Assertions.assertEquals("test locality", order.getRecipientLocality());
        Assertions.assertEquals("test sub region", order.getRecipientSubRegion());
        Assertions.assertEquals("test settlement", order.getRecipientSettlement());
        Assertions.assertEquals("test street", order.getRecipientStreet());
        Assertions.assertEquals("test house", order.getRecipientHouse());
        Assertions.assertEquals("test building", order.getRecipientBuilding());
        Assertions.assertEquals("test housing", order.getRecipientHousing());
        Assertions.assertEquals("test room", order.getRecipientRoom());
        Assertions.assertEquals("test zip code", order.getRecipientZipCode());
        Assertions.assertEquals("test porch", order.getRecipientPorch());
        Assertions.assertEquals("1", order.getRecipientFloor());
        Assertions.assertEquals("test metro", order.getRecipientMetro());
        Assertions.assertEquals(BigDecimal.valueOf(54.1000323), order.getRecipientLatitude());
        Assertions.assertEquals(BigDecimal.valueOf(54.1020189), order.getRecipientLongitude());
        Assertions.assertEquals("5704910", order.getRecipientGeoId());
        Assertions.assertEquals("test intercom", order.getRecipientIntercom());
        Assertions.assertEquals("test@mail.yandex.ru", order.getRecipientEmail());
        Assertions.assertEquals("+79123456789#1", order.getRecipientPhone().getNormalized());

        Assertions.assertEquals(status, order.getDetailedStatus());

        Assertions.assertEquals(PaymentMethod.CASH.name(), order.getCostPaymentMethod().getCode());
        Assertions.assertEquals(BigDecimal.valueOf(0.0123), order.getCostCashServicePercent());
        Assertions.assertEquals(BigDecimal.valueOf(2645171), order.getCostAssessedValue());
        Assertions.assertEquals(BigDecimal.valueOf(5479409), order.getCostItemsSum());
        Assertions.assertEquals(BigDecimal.valueOf(8233250), order.getCostDeliveryForCustomer());
        Assertions.assertEquals(BigDecimal.valueOf(9363363), order.getCostManualDeliveryForCustomer());
        Assertions.assertEquals(true, order.getCostIsFullyPrepaid());
        Assertions.assertEquals(BigDecimal.valueOf(1740293), order.getCostTotal());

        Assertions.assertEquals("test comment", order.getComment());
        Assertions.assertFalse(order.getFake());
        Assertions.assertEquals(Long.valueOf(1), order.getPlatformClientId());
    }

    private void assertSender(@Nonnull YaDeliveryOrder order) {
        assertSender(order, "test name");
    }

    private void assertSender(@Nonnull YaDeliveryOrder order, String senderName) {
        var sender = order.getSender();
        Assertions.assertNotNull(sender);

        Assertions.assertEquals(Long.valueOf(1761959), sender.getLomSenderId());
        Assertions.assertEquals(senderName, sender.getName());
        Assertions.assertEquals("+79123456789#123", sender.getPhone().getNormalized());
        Assertions.assertEquals("https://test.yandex.ru", sender.getUrl());
        Assertions.assertEquals(List.of("test@mail.yandex.ru", "test@mail.ru"), sender.getEmails());
        Assertions.assertEquals("test first name", sender.getFirstName());
        Assertions.assertEquals("test middle name", sender.getMiddleName());
        Assertions.assertEquals("test last name", sender.getLastName());
    }

    @Test
    @Transactional
    public void oldOrder() {
        var testData = testUtils.testData("/lom_old_order.json");
        prepareLomScriptApiMock(testData);

        consumer.accept(List.of(testData));

        Assertions.assertNull(entityStorageService.get(YaDeliveryOrder.gidOf("2020-02-02T20:20:02.02Z", 29121987L)));
    }

    @Test
    @Transactional
    public void updateSenderFromLastOrder() {
        var firstMessage = testUtils.testData("/lom_create.json");
        var secondMessage = testUtils.testData("/lom_edit.json");
        prepareLomScriptApiMock(firstMessage, secondMessage);

        consumer.accept(List.of(firstMessage, secondMessage));

        var gid = YaDeliveryOrder.gidOf("2020-03-31T21:21:21.212121Z", 29121987L);
        YaDeliveryOrder order = entityStorageService.get(gid);
        Assertions.assertNotNull(order);

        assertSender(order, "test name2");
    }

    private void prepareLomScriptApiMock(byte[]... jsonEvents) {
        for (byte[] jsonEvent : jsonEvents) {
            EventDto event = objectSerializeService.deserialize(jsonEvent, EventDto.class);
            JsonNode snapshot = event.getSnapshot();
            Object order = jsonNodeScriptValueStrategy.wrap(snapshot);

            Mockito.when(lomScriptServiceApi.getOrder(event.getEntityId()))
                    .thenReturn(order);
        }
    }
}
