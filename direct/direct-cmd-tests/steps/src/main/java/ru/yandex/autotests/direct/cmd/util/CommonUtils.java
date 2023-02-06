package ru.yandex.autotests.direct.cmd.util;

import java.beans.PropertyDescriptor;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;

import ru.yandex.autotests.direct.cmd.data.CSRFToken;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.httpclientlite.core.Response;

public class CommonUtils {

    public static CSRFToken getCSRFToken(Response response) {
        Pattern pattern = Pattern.compile("csrf_token([= :'\"&]|(%3D)|(quot;))+.+?[ \"'&]");
        java.util.regex.Matcher matcher = pattern.matcher(response.getResponseContent().asString());
        if (matcher.find()) {
            return new CSRFToken(matcher.group().replace("csrf_token", "").replaceAll("([= :'\"&]|(%3D)|(quot;))?", ""));
        }
        return CSRFToken.EMPTY;
    }

    public static Object convertEmptyToNull(Object bean) {
        try {
            for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(bean.getClass())) {
                if (descriptor.getPropertyType().equals(String.class)) {
                    String value = (String) descriptor.getReadMethod().invoke(bean);
                    if (value != null && value.isEmpty()) {
                        descriptor.getWriteMethod().invoke(bean, new Object[]{null});
                    }
                }
            }
        } catch (Exception e) {
            throw new DirectCmdStepsException("Не удалось обнулить String поля", e);
        }
        return bean;
    }
}