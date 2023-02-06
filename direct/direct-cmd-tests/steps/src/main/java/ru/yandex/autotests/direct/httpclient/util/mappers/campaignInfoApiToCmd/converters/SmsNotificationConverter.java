package ru.yandex.autotests.direct.httpclient.util.mappers.campaignInfoApiToCmd.converters;

import org.dozer.DozerConverter;
import ru.yandex.autotests.directapi.common.api45.SmsNotificationInfo;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 03.04.15
 */
public class SmsNotificationConverter extends DozerConverter<SmsNotificationInfo, String> {

    public SmsNotificationConverter() {
        super(SmsNotificationInfo.class, String.class);
    }

    @Override
    public String convertTo(SmsNotificationInfo source, String destination) {
        return source.getSmsTimeFrom()+ ":" + source.getSmsTimeTo();
    }

    @Override
    public SmsNotificationInfo convertFrom(String source, SmsNotificationInfo destination) {
        SmsNotificationInfo smsNotificationInfo = new SmsNotificationInfo();
        smsNotificationInfo.setSmsTimeFrom(source.substring(0, 5));
        smsNotificationInfo.setSmsTimeTo(source.substring(6, 11));
        return smsNotificationInfo;
    }
}
