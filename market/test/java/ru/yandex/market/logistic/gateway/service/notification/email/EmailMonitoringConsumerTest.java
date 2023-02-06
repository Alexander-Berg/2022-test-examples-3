package ru.yandex.market.logistic.gateway.service.notification.email;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import ru.yandex.market.logistic.gateway.model.entity.Email;
import ru.yandex.market.logistic.gateway.repository.EmailRepository;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailMonitoringConsumerTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private JavaMailSender javaMailSender;

    private EmailMonitoringConsumer consumer;

    @Before
    public void setUp() {
        consumer = new EmailMonitoringConsumer(
            new EmailSender(javaMailSender),
            emailRepository
        );

        when(emailRepository.poll()).thenReturn(Arrays.asList(new Email(), new Email()));
    }

    @Test
    public void consume() {
        consumer.consume();

        verify(javaMailSender, times(2)).send(any(SimpleMailMessage.class));
    }
}
