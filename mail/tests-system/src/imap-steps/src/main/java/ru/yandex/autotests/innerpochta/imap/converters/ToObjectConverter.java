package ru.yandex.autotests.innerpochta.imap.converters;

import java.util.List;

import ch.lambdaj.function.convert.Converter;
import com.google.common.collect.Lists;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 08.03.14
 * Time: 0:25
 */
public class ToObjectConverter<T> implements Converter<T, Object[]> {
    private List<Object> objs;

    public ToObjectConverter(List<Object> objs) {
        this.objs = objs;
    }

    public static <T> ToObjectConverter wrapAndAddTo(Object... obj) {
        return new ToObjectConverter<T>(Lists.newArrayList(obj));
    }

    public static ToObjectConverter wrap() {
        return wrapAndAddTo();
    }

    @Override
    public Object[] convert(T from) {
        return new Object[]{from};
    }
}
