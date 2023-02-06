package ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters;

import org.dozer.DozerConverter;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 16.04.15
 */
public class StringToDateConverter  extends DozerConverter<Date, String> {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public StringToDateConverter() {
        super(Date.class, String.class);
    }

    @Override
    public String convertTo(Date source, String destination) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTime(source);
        DateFormat format = new SimpleDateFormat(DATE_FORMAT);
        return format.format(calendar.getTime());
    }

    @Override
    public Date convertFrom(String source, Date destination) {
        DateFormat format = new SimpleDateFormat(DATE_FORMAT);
        Date date = null;
        try {
            date = format.parse(source);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}