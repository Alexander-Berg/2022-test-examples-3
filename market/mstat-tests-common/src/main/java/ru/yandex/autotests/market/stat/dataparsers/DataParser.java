package ru.yandex.autotests.market.stat.dataparsers;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * @author Romanov Daniil <a href="mailto:entarrion@yandex-team.ru"/>
 * @date 07.11.2014
 */
public interface DataParser<T> {

    T read(String input);

    T read(InputStream input);

    List<T> readAll(String input);

    List<T> readAll(InputStream input);

    String format(Object record);

    String formatCollection(Collection<?> records);
}
