package ru.yandex.market.core.framework.composer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class MbiXmlConverterTest {

    @DisplayName("Проверка успешной сериализации строки с битым символом")
    @Test
    void convert() {
        new MbiXmlConverter().convert(new String(new char[]{0}));
    }
}