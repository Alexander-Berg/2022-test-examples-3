package ru.yandex.mail.autouser.service;

import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import ru.yandex.mail.autouser.commands.CommandExecutor;
import ru.yandex.mail.autouser.commands.EmailScheduler;
import ru.yandex.mail.autouser.mail.ImapService;
import ru.yandex.mail.autouser.mail.SmtpService;

import static org.mockito.Mockito.mock;

/**
 * @author Sergey Galyamichev
 */
@Profile("test")
@Configuration
@Import(EmailScheduler.class)
public class ImapConfiguration {
    private final ScheduledExecutorService scheduler;

    public ImapConfiguration(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    @Bean
    public CommandService service() {
        CommandExecutor executor = new CommandExecutor(mock(ImapService.class), mock(SmtpService.class), scheduler);
        return new CommandServiceImpl(executor, mock(AgentList.class));
    }
}
