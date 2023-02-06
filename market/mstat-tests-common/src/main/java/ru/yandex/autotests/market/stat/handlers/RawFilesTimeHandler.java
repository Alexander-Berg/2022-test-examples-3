package ru.yandex.autotests.market.stat.handlers;

import org.beanio.types.TypeConversionException;
import org.beanio.types.TypeHandler;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.date.DatePatterns;

/**
 * Created by jkt on 10.04.14.
 * <p/>
 * BeanIO handler for parsing formatting UnixTimestamp from tsv to LocalDateTime
 */
public class RawFilesTimeHandler implements TypeHandler {

    @Override
    public Object parse(String text) throws TypeConversionException {
        return DatePatterns.parseByFirstMatchingPattern(text);
    }

    @Override
    public String format(Object value) {
        if (value instanceof LocalDateTime) {
            return DatePatterns.RAW_FILES_DATE.format((LocalDateTime) value);
        }
        throw new IllegalArgumentException("Argument must be of type " + LocalDateTime.class);
    }

    @Override
    public Class<?> getType() {
        return LocalDateTime.class;
    }
}
