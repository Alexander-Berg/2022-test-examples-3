package ru.yandex.mail.autouser.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.mail.autouser.commands.DeleteCommand;

import static org.junit.Assert.assertEquals;

/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ImapConfiguration.class})
public class CommandServiceTest {
    @Autowired
    private CommandService service;

    @Test
    public void runCommand() {
        DeleteCommand command = new DeleteCommand(10, 20);
        assertEquals("OK", service.run(command));
        assertEquals("OK", service.stop(command.getId()));
    }
}
