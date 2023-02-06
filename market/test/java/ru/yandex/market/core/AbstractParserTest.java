package ru.yandex.market.core;

import java.io.InputStream;

import org.junit.Assert;

/**
 * Базовый класс для тестирования парсеров
 *
 * @author zoom
 */
public class AbstractParserTest extends Assert {
    /**
     * Возвращает стрим ресурса, имя которого сформированого по правилам, основанным на имени тестового класса
     * <p>
     * См. код
     */
    public InputStream getContentStream(String name) {
        return this.getClass().getResourceAsStream(this.getClass().getSimpleName() + "_" + name);

    }
}
