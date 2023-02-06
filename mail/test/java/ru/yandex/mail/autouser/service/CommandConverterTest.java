package ru.yandex.mail.autouser.service;

import org.junit.Test;

import ru.yandex.mail.autouser.commands.CommandConverter;
import ru.yandex.mail.autouser.commands.SendCommandCommand;
import ru.yandex.mail.autouser.commands.SendTextCommand;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Sergey Galyamichev
 */
public class CommandConverterTest {

    @Test
    public void testConversion() {
        try {
            SendTextCommand subCommand = new SendTextCommand("address1", "subject", "text");
            SendCommandCommand command = new SendCommandCommand("address", subCommand);
            String packet = CommandConverter.toString(command);
            SendCommandCommand result = CommandConverter.fromString(packet, SendCommandCommand.class);
            assertEquals(command.getId(), result.getId());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}
