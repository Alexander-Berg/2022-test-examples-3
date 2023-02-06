package ru.yandex.autotests.direct.httpclient.util.mappers.ContactInfoApiToCmd.converters;

import org.dozer.CustomConverter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shmykov on 13.04.15.
 * Конвертер для преобразования workTime из апи бина ContactInfo в аналогичное поле в ContactInfo
 * Разница в том, что в апи в этом поле только один разделитель - ';', в то время как в бэкенде каждая строчка
 * рабочего времени разделяется с помощью';', а внутри строчки используется '#'
 * Пример:
 * - api - 0;3;10;00;18;00;4;4;07;00;22;00;5;6;12;00;11;00
 * - cmd bean - 0#3#10#00#18#00;4#4#07#00#22#00;5#6#12#00#11#00
 */
public class ContactInfoApiToCmdWorkTimeConverter implements CustomConverter {

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        String workTimeWithPoundSigns = ((String) sourceFieldValue).replaceAll(";", "#");
        Matcher matcher = Pattern.compile("(\\d+#\\d+#\\d+#\\d+#\\d+#\\d+#)").matcher(workTimeWithPoundSigns);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String workTimePart = matcher.group();
            matcher.appendReplacement(sb, workTimePart.substring(0, workTimePart.lastIndexOf('#')) + ";");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
