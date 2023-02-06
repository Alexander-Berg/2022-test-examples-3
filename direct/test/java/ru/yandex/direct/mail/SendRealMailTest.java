package ru.yandex.direct.mail;

import java.time.LocalDateTime;

import org.junit.Ignore;
import org.junit.Test;

public class SendRealMailTest {

    @Test
    @Ignore("Only for manual tests")
    public void testSendRealMail() {
        // Чтобы заработало, надо пробросить туннель до разработческой машины с SMTP сервером
        // Пример:
        // ssh -L 3025:localhost:25 ppcdev3

        YServiceTokenCreator tokenCreator = new YServiceTokenCreator("yadirect", "12345678", "abcdf");
        SmtpMailSender smtpMailSender = new SmtpMailSender("devnull@yandex-team.ru", "localhost", 3025, tokenCreator);

        // по умолчанию отправляет на адрес ${USER}@yandex-team.ru
        String user = System.getenv("USER");

        String subject = "Test mail sent at " + LocalDateTime.now();
        String messageBody = "<h1>Привет!</h1>Проверка <b>связи</b>";
        MailMessage message = new MailMessage(
                new EmailAddress("devnull@yandex-team.ru", "noreply"),
                new EmailAddress(String.format("%s@yandex-team.ru", user), user),
                subject, messageBody, MailMessage.EmailContentType.HTML, null);

        smtpMailSender.send(message);
    }
}
