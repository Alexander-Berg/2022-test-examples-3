package ru.yandex.autotests.direct.httpclient.util.mappers.cmdToAnotherCmdMappers.converters;

import org.apache.commons.lang.StringUtils;
import org.dozer.CustomConverter;
import ru.yandex.autotests.direct.httpclient.data.campaigns.campaignInfo.DeviceTargeting;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 18.05.15
 */
public class DeviceTargetingConverter implements CustomConverter {

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (sourceFieldValue == null || sourceFieldValue.equals("")) {
            return null;
        }
        DeviceTargeting result = new DeviceTargeting();
        String[] devices = StringUtils.split((String) sourceFieldValue, ",");
        for (String device : devices) {
            switch (device) {
                case "iphone":
                    result.setIphone(1);
                    break;
                case "ipad":
                    result.setIpad(1);
                    break;
                case "android_phone":
                    result.setAndroidPhone(1);
                    break;
                case "android_tablet":
                    result.setAndroidTablet(1);
                    break;
                case "other_devices":
                    result.setOtherDevices(1);
                    break;
            }
        }
        return result;
    }
}
