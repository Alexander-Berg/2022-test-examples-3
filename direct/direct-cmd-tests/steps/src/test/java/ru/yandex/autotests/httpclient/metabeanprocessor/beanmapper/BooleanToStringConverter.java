package ru.yandex.autotests.httpclient.metabeanprocessor.beanmapper;

import org.dozer.DozerConverter;

/**
 * Created by shmykov on 09.02.15.
 */
public class BooleanToStringConverter extends DozerConverter<Boolean, String> {

    public BooleanToStringConverter() {
        super(Boolean.class, String.class);
    }

    @Override
    public String convertTo(Boolean source, String destination) {
        if (source == null) {
            return null;
        }
        if (source == true) {
            return "1";
        } else {
            return "0";
        }
    }

    @Override
    public Boolean convertFrom(String source, Boolean destination) {
        return null;
    }
}
