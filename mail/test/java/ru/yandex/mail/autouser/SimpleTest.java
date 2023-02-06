package ru.yandex.mail.autouser;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.mail.autouser.commands.CommandConverter;
import ru.yandex.mail.autouser.commands.RepeatCommand;
import ru.yandex.mail.autouser.commands.SendCommandCommand;
import ru.yandex.mail.autouser.commands.SendTextCommand;


public class SimpleTest {
    @Test
    public void simple() {
        SendTextCommand text = new SendTextCommand("g-s-v@yandex-team.ru", "Hello!", "Hellobody");
        RepeatCommand repeat = new RepeatCommand(120000, text);
        SendCommandCommand sendCommandCommand = new SendCommandCommand("imap.agent@yandex.ru", repeat);
        Assert.assertNotNull(CommandConverter.toSafeString(sendCommandCommand));

        RepeatCommand repeat2 = new RepeatCommand(180000, text);
        Assert.assertNotNull(CommandConverter.toSafeString(repeat2));
    }
}
