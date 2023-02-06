package ru.yandex.market.partner.auction;

import java.io.InputStream;

import ru.yandex.market.core.AbstractParserTest;

/**
 * @author vbudnev
 */

/**
 * Небольшое расширение для удобства пользования файла ресурса с явным именованием из разных классов тестов.
 */
public class AbstractParserTestExtended extends AbstractParserTest {

    /**
     * Возвращает стрим ресурса, с явным указанием именем файла.
     */
    public InputStream getContentStreamFromExplicitFile(String name) {
        return this.getClass().getResourceAsStream(name);
    }

}
