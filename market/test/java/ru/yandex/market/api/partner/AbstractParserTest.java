package ru.yandex.market.api.partner;


import java.io.InputStream;

/**
 * Базовый класс для тестирования парсеров
 *
 * @author zoom
 */
public abstract class AbstractParserTest {
    /**
     * Возвращает стрим ресурса, имя которого сформированого по правилам, основанным на имени тестового класса
     * <p>
     * См. код
     */
    protected InputStream getContentStream(String name) {
        return this.getClass().getResourceAsStream(this.getClass().getSimpleName() + "_" + name);

    }

}
