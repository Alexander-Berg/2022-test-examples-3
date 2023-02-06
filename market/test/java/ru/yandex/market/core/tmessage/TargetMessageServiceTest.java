package ru.yandex.market.core.tmessage;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.tmessage.model.TargetMessage;
import ru.yandex.market.core.tmessage.service.TargetMessageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

public class TargetMessageServiceTest extends FunctionalTest {
    @Autowired
    @Qualifier("targetMessageServiceWithMockedPartnerBotRestClient")
    public TargetMessageService targetMessageService;
    @Autowired
    public Clock clock;

    private static final long USER_UUID = 1180878297L;
    private static final String TELEGRAM_USERNAME = "slavadurin";

    @BeforeEach
    void init() {
        Instant mockTime = LocalDateTime.of(2020, 10, 20, 17, 5).toInstant(ZoneOffset.ofHours(3));
        when(clock.instant()).thenReturn(mockTime);
    }

    @Test
    @DisplayName("Тестирует обработку файла и сохранение данных в БД")
    @DbUnitDataSet(
            before = "targetMessageUpload.creation.before.csv",
            after = "targetMessageUpload.creation.after.csv")
    public void testProcessFileAndSaveData() throws IOException {
        String DATA_FILE_PATH = getClass().getResource("targetMessageDataFile.csv").getFile();

        TargetMessage info = targetMessageService.processFile(DATA_FILE_PATH);
        assertThat(info.getTotalRecipientsCount()).isEqualTo(3);
        assertThat(info.getData().getRecipients()).hasSize(3);
        assertThat(info.getData().getVariables()).hasSize(2);
        assertThat(info.getData().getTestRecipientIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("Тестирует получение информации о сообщение по его ID")
    @DbUnitDataSet(
            before = "targetMessageUpload.get.remove.before.csv")
    public void testFetchTargetMessageById() {
        TargetMessage info = targetMessageService.getById(10L);
        assertThat(info).isNotNull();
        assertThat(info.getId()).isNotNull();
        assertThat(info.getTotalRecipientsCount()).isEqualTo(3);
        assertThat(info.getTemplate()).isEqualTo("Мой супер {{com}}");
    }

    @Test
    @DisplayName("Тестирует получение истории рассылок за последние 30 дней")
    @DbUnitDataSet(
            before = "targetMessage.fetch.history.before.csv")
    public void testFetchHistoryForPastNDays() {
        // Используется MAX_DAY_COUNT_THRESHOLD=500 на данный момент
        Collection<TargetMessage> messages = targetMessageService
                .findTargetMessageForPast(TargetMessageService.MAX_DAY_COUNT_THRESHOLD);
        assertThat(messages).hasSize(4);
    }

    @Test
    @DisplayName("Тестирует удаление таргетированного сообщения по его ID")
    @DbUnitDataSet(
            before = "targetMessageUpload.get.remove.before.csv",
            after = "targetMessageUpload.creation.before.csv")
    public void testRemoveTargetMessageById() {
        targetMessageService.removeById(10L);
    }

    @Test
    @DisplayName("Тестирует обновление текста шаблона и статуса перед постановкой в очередь")
    @DbUnitDataSet(
            before = "targetMessageUpload.get.remove.before.csv",
            after = "targetMessageUpload.ready.to.send.csv")
    public void testTargetMessageUpdatedAndQueued() {
        targetMessageService.send(10L, "Новый шаблон {{comp}}");
    }

    @Test
    @DisplayName("Тестирует удаление старых, неотправленных сообщений")
    @DbUnitDataSet(
            before = "targetMessage.remove.old.before.csv",
            after = "targetMessage.remove.old.after.csv")
    public void testNotSentTargetMessageRemovedAfterSomeTime() {
        targetMessageService.removeOldNotSent();
    }

    @Test
    @DisplayName("Тестирует тестовую отправку напрямую в телеграм")
    @DbUnitDataSet(
            before = "targetMessage.trial.send.before.csv",
            after = "targetMessage.trial.send.after.csv")
    public void testTrialSend() {
        String username = targetMessageService.findTelegramUsernameByUUID(USER_UUID);
        assertThat(username).isEqualTo(TELEGRAM_USERNAME);
        targetMessageService.sendToTelegramDirectly(10L, username, "Новый текст. Привет {{partner_id}}. Тестовый тест {{cmp}}");
    }

    @Test
    @DisplayName("Тестирует отправку таргетированных сообщений через common-notifier")
    @DbUnitDataSet(
            before = "targetMessage.process.new.before.csv",
            after = "targetMessage.process.new.after.csv")
    public void testSendQueuedMessage() {
        targetMessageService.processNewMessagesQueue();

        verifySentNotificationType(partnerNotificationClient, 3, 1999632906L);
    }

    @Test
    @DisplayName("Тестирует обновление состояния сообщений со статусом IN_PROGRESS")
    @DbUnitDataSet(
            before = "targetMessage.update.messages.before.csv",
            after = "targetMessage.update.messages.after.csv")
    public void testUpdateMessagesAfterSending() {
        targetMessageService.updateMessagesInProgress();

        verifyNoInteractions(partnerNotificationClient);
    }
}
