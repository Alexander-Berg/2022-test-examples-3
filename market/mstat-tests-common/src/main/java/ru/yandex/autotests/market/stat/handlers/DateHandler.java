package ru.yandex.autotests.market.stat.handlers;

import org.beanio.types.TypeConversionException;
import org.beanio.types.TypeHandler;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.stat.date.DatePatterns;

/**
 * Created by kateleb on 28.06.16
 */
public class DateHandler implements TypeHandler {

    @Override
    public LocalDateTime parse(String text) throws TypeConversionException {
        return DatePatterns.MYSQL_DAY.parse(text);
    }

    @Override
    public String format(Object time) {
        if (time instanceof LocalDateTime) {
            return DatePatterns.MYSQL_DAY.format((LocalDateTime) time);
        }
        throw new IllegalArgumentException("Argument must be of type " + LocalDateTime.class);

    }

    @Override
    public Class<?> getType() {
        return LocalDateTime.class;
    }
}
