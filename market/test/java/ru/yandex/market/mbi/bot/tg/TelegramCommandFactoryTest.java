package ru.yandex.market.mbi.bot.tg;

import java.util.Optional;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.api.client.entity.telegram.NotificationMetaDto;
import ru.yandex.market.mbi.bot.tg.action.command.ManageSubscriptionCommand;
import ru.yandex.market.mbi.bot.tg.action.command.ResubscribeCommand;
import ru.yandex.market.mbi.bot.tg.action.command.TelegramCommand;
import ru.yandex.market.mbi.bot.tg.action.command.UnsubscribeCommand;
import ru.yandex.market.mbi.bot.tg.model.TgBotAccount;
import ru.yandex.market.mbi.bot.tg.service.TelegramCommandFactory;
import ru.yandex.market.mbi.bot.tg.util.TelegramUtils;

import static org.mockito.Mockito.mock;

public class TelegramCommandFactoryTest {

    @Test
    public void createNullableCommandTest() {
        TgBotAccount account = mock(TgBotAccount.class);
        // test UNSUBSCRIBE
        Optional<TelegramCommand> command = TelegramCommandFactory
                .createCommand("/unsubscribe 10", account, new User(1L), new Chat(), new Message());

        Assertions.assertTrue(command.isPresent());

        command = TelegramCommandFactory
                .createCommand("/unsubscribe 10 null", account, new User(1L), new Chat(), new Message());

        Assertions.assertTrue(command.isPresent());

        UnsubscribeCommand unsubscribeCommand = (UnsubscribeCommand) command.get();
        Assertions.assertNull(unsubscribeCommand.getPartnerId());

        command = TelegramCommandFactory
                .createCommand("/unsubscribe 10 100", account, new User(1L), new Chat(), new Message());

        Assertions.assertTrue(command.isPresent());

        unsubscribeCommand = (UnsubscribeCommand) command.get();
        Assertions.assertEquals(100L, unsubscribeCommand.getPartnerId());

        // test RESUBSCRIBE
        command = TelegramCommandFactory
                .createCommand("/resubscribe 10", account, new User(1L), new Chat(), new Message());

        Assertions.assertTrue(command.isPresent());

        command = TelegramCommandFactory
                .createCommand("/resubscribe 10 null", account, new User(1L), new Chat(), new Message());

        Assertions.assertTrue(command.isPresent());

        ResubscribeCommand resubscribeCommand = (ResubscribeCommand) command.get();
        Assertions.assertNull(resubscribeCommand.getPartnerId());

        command = TelegramCommandFactory
                .createCommand("/resubscribe 10 100", account, new User(1L), new Chat(), new Message());

        Assertions.assertTrue(command.isPresent());

        resubscribeCommand = (ResubscribeCommand) command.get();
        Assertions.assertEquals(100L, resubscribeCommand.getPartnerId());

        command = TelegramCommandFactory
                .createCommand("/resubscribe 10 100", account, new User(1L), new Chat(), new Message());

        Assertions.assertTrue(command.isPresent());

        resubscribeCommand = (ResubscribeCommand) command.get();
        Assertions.assertEquals(100L, resubscribeCommand.getPartnerId());

        String commandText = TelegramUtils
                .createSubscriptionForPartnerCallback(new NotificationMetaDto(10, "10", false, 777L, "louiser"), 1L);
        Assertions.assertEquals("/manageSubscription 1 10 777 true", commandText);

        command = TelegramCommandFactory
                .createCommand(commandText, account, new User(1L), new Chat(), new Message());

        Assertions.assertTrue(command.isPresent());

        ManageSubscriptionCommand manageSubscriptionCommand = (ManageSubscriptionCommand) command.get();
        Assertions.assertEquals(777L, manageSubscriptionCommand.getPartnerId());
        // имя не передаём, ибо ограничение 64 байта, иначе 400:
        Assertions.assertNull(manageSubscriptionCommand.getPartnerName());
    }
}
