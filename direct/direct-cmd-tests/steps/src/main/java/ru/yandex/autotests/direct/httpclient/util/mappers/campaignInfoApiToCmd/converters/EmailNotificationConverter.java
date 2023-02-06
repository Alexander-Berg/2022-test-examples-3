package ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd.converters;

import org.dozer.DozerConverter;
import ru.yandex.autotests.directapi.common.api45.EmailNotificationInfo;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 03.04.15
 */
public class EmailNotificationConverter extends DozerConverter<EmailNotificationInfo, String> {

    public EmailNotificationConverter() {
        super(EmailNotificationInfo.class, String.class);
    }

    @Override
    public String convertTo(EmailNotificationInfo source, String destination) {
        return source.getEmail();
    }

    @Override
    public EmailNotificationInfo convertFrom(String source, EmailNotificationInfo destination) {
        EmailNotificationInfo emailNotificationInfo = new EmailNotificationInfo();
        emailNotificationInfo.setEmail(source);
        return emailNotificationInfo;
    }
}
