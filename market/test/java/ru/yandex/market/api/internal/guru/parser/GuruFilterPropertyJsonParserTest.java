package ru.yandex.market.api.internal.guru.parser;

import org.junit.Test;
import ru.yandex.market.api.category.FilterProperty;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class GuruFilterPropertyJsonParserTest {

    @Test
    public void shouldParse() throws Exception {
        Map<String, FilterProperty> parsed =
            new GuruFilterPropertyJsonParser().parse(ResourceHelpers.getResource("filters-properties.json"));

        assertEquals(
            "Наличие возможности зарядки от USB-порта компьютера.<br/> Наличие такой функции позволяет зарядить аккумулятор телефона от USB-порта любого компьютера, что позволит оставаться на связи даже в тот момент, когда под рукой нет зарядного устройства.",
            parsed.get("6051521").getDescription()
        );
        assertEquals(
            "Принято различать несколько типов разъемов для зарядки и синхронизации мобильных телефонов и смартфонов: <i>micro-USB, USB-C, Apple 30-pin</i> и <i>Apple Lightning</i>. Некоторые телефоны имеют собственный тип разъема - <i>проприетарный</i>. <br/> <i><b>micro-USB</b></i> – самый распространенный тип разъема. Он используется во всех современных смартфонах на базе Android и Windows Phone. Часто даже простые кнопочные телефоны используют micro-USB для зарядки и синхронизации. <br/> <i><b>USB-C</b></i> – новый тип разъема, пришедший на смену micro-USB. Разъем USB-C является симметричным, что позволяет подключать кабель любой стороной, в отличие от micro-USB. Физический размер сопоставим с micro-USB. <br/> <i><b>Apple 30-pin</b></i> – уже устаревший тип подключения. Компания Apple использовала его в iPhone 4s и более ранних версиях этого смартфона, а также в некоторых других продуктах компании (iPod и iPad). К минусам можно отнести довольно большой размер самого разъема. <br/> <i><b>Apple Lightning</b></i> – на данный момент используется в смартфонах компании Apple iPhone 5 и более поздних версиях. Разъем компактный и по размерам сопоставим с USB-C и micro-USB. <br/>",
            parsed.get("12761881").getDescription()
        );

        // Букер возвращает {} для фильтров без описания (исправить в тикете MARKETKGB-707)
        assertNull(parsed.get("404040"));
    }
}
