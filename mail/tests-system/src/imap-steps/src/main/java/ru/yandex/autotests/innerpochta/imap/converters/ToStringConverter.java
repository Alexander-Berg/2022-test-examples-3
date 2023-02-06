package ru.yandex.autotests.innerpochta.imap.converters;

import java.util.List;

import ch.lambdaj.function.convert.Converter;
import com.google.common.collect.Lists;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 13.04.14
 * Time: 17:38
 */
public class ToStringConverter<T> implements Converter<T, String> {
    private List<Object> objs;

    public ToStringConverter(List<Object> objs) {
        this.objs = objs;
    }

    public static <T> ToStringConverter wrapAndAddTo(Object... obj) {
        return new ToStringConverter<T>(Lists.newArrayList(obj));
    }

    public static ToStringConverter wrap() {
        return wrapAndAddTo();
    }

    @Override
    public String convert(T from) {
        return from.toString();
    }
}
