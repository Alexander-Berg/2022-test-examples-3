package ui_tests.src.test.java.tools;

import unit.Config;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;


public class Email {

    private String encodeToASCII(String text) {
        Pattern pattern = Pattern.compile("[а-я,А-Я]");
        if (pattern.matcher(text).find()) {
            List<String> texts = Arrays.asList(text.split("@"));
            StringBuilder newText = new StringBuilder();

            for (String text2 : texts) {
                if (pattern.matcher(text2).find()) {
                    newText.append(IDN.toASCII(text2) + "@");
                } else {
                    newText.append(text2 + "@");
                }
            }
            return newText.deleteCharAt(newText.length() - 1).toString();
        } else return text;
    }

    public void sendAnEmail(Classes.Email email) {
        Properties properties = new Properties();
        // Хост или IP-адрес почтового сервера
        properties.put("mail.smtp.host", "outbound-relay.yandex.net");
        // Требуется ли аутентификация для отправки сообщения
        properties.put("mail.smtp.auth", "true");
        // Порт для установки соединения
        properties.put("mail.smtp.socketFactory.port", "25");
        // Фабрика сокетов, так как при отправке сообщения Yandex требует SSL-соединения
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.mime.address.strict", "false");
        Session session = Session.getDefaultInstance(properties,
                // Аутентификатор - объект, который передает логин и пароль
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(Config.getOutgoingMailUserEmail(), Config.getOutgoingMailPass());
                    }
                });

        // Создаем новое почтовое сообщение
        Message message = new MimeMessage(session);

        try {
            if (email.getFromAlias() == null) {
                // От кого
                message.setFrom(new InternetAddress(Config.getOutgoingMailUserEmail()));
            } else {
                message.setFrom(new InternetAddress(encodeToASCII(email.getFromAlias())));
            }
            // Кому
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(encodeToASCII(email.getTo())));
            // Тема письма
            message.setSubject(email.getSubject());

            if (email.getReplyTo().size() > 0) {
                InternetAddress[] address = new InternetAddress[email.getReplyTo().size()];
                int i = 0;
                for (Map.Entry map : email.getReplyTo().entrySet()) {
                    if (map.getValue().toString().equals("")) {
                        address[i++] = new InternetAddress(encodeToASCII(map.getKey().toString()));
                    } else {
                        address[i++] = new InternetAddress(encodeToASCII(map.getKey().toString()), map.getValue().toString(), "utf-8");
                    }
                    message.setReplyTo(address);
                }
            }
            if (!email.getHeaders().isEmpty()) {
                for (Map.Entry map : email.getHeaders().entrySet()) {
                    message.setHeader(map.getKey().toString(), map.getValue().toString());
                }
            }

            // Текст письма
            MimeMultipart multipart = new MimeMultipart();
            MimeBodyPart part1 = new MimeBodyPart();


            if ((email.getTextForHTMLFormat() != null)) {
                part1.addHeader("Content-Type", "text/html; charset=\"UTF-8\"");
                part1.setDataHandler(new DataHandler(email.getTextForHTMLFormat(), "text/html; charset=\"utf-8\""));
            } else {
                part1.addHeader("Content-Type", "text/plain; charset=\"UTF-8\"");
                part1.setDataHandler(new DataHandler(email.getText(), "text/plain; charset=\"utf-8\""));
            }
            if (email.getFile() != null) {
                for (File file : email.getFile()) {
                    MimeBodyPart part2 = new MimeBodyPart();
                    try {
                        part2.setFileName(MimeUtility.encodeWord(file.getName()));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    part2.setDataHandler(new DataHandler(new FileDataSource(file)));
                    multipart.addBodyPart(part2);
                }
            }
            multipart.addBodyPart(part1);
            message.setContent(multipart);

            // Поехали!!!
            Transport.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new Error(e);
        }
    }


    public void readEmail(String subjectEmail) {
        Properties properties = new Properties();
        properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        Session session = Session.getDefaultInstance(properties);
        Store store = null;
        try {
            // Для чтения почтовых сообщений используем протокол IMAP.
            // Почему? Так Yandex сказал: https://yandex.ru/support/mail/mail-clients.html
            // См. раздел "Входящая почта"
            store = session.getStore("imap");
            // Подключаемся к почтовому ящику
            store.connect("imap.yandex.ru", 993, Config.getOutgoingMailUserEmail(), Config.getOutgoingMailPass());
            // Это папка, которую будем читать
            Folder inbox = null;
            try {
                // Читаем папку "Входящие сообщения"
                inbox = store.getFolder("INBOX");
                // Будем только читать сообщение, не меняя их
                inbox.open(Folder.READ_ONLY);

                // Получаем количество сообщения в папке
                int count = inbox.getMessageCount();
                // Вытаскиваем все сообщения с первого по последний
                Message[] messages = inbox.getMessages(1, count);
                // Циклом пробегаемся по всем сообщениям
                for (Message message : messages) {
                    // От кого
                    String from = ((InternetAddress) message.getFrom()[0]).getAddress();
                    System.out.println("FROM: " + from);
                    // Тема письма
                    System.out.println("SUBJECT: " + message.getSubject());
                }
            } finally {
                if (inbox != null) {
                    // Не забываем закрыть собой папку сообщений.
                    inbox.close(false);
                }
            }

        } catch (MessagingException e) {
            e.printStackTrace();
        } finally {
            if (store != null) {
                // И сам почтовый ящик тоже закрываем
                try {
                    store.close();
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
