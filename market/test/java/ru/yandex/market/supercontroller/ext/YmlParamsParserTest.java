package ru.yandex.market.supercontroller.ext;

import junit.framework.TestCase;
import org.junit.Test;
import ru.yandex.market.ir.http.Offer;

import java.util.List;

/**
 * todo описать предназначение.
 *
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class YmlParamsParserTest extends TestCase {
    @Test
    public void testParseYmlParams() {
        YmlParamsParser parser = new YmlParamsParser();
        List<Offer.YmlParam> ymlParams = parser.parseYmlParams("<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n" +
            "<offer_params>" +
            "<param name=\"vendor\" unit=\"\">Maxi Cosi</param>" +
            "<param name=\"country\">China</param>" +
            "<param name=\"Длина\" unit=\"метр\">6</param>" +
            "<param name=\"Ширина\" unit=\"метр\">\n5\n</param>" +
            "<param name=\"Высота\" unit=\"м\"> 10 </param>" +
            "</offer_params>");
        assertEquals(ymlParams.size(), 5);
        assertYmlParam(ymlParams.get(0), "vendor", "Maxi Cosi", "");
        assertYmlParam(ymlParams.get(1), "country", "China", "");
        assertYmlParam(ymlParams.get(2), "Длина", "6", "метр");
        assertYmlParam(ymlParams.get(3), "Ширина", "\n5\n", "метр");
        assertYmlParam(ymlParams.get(4), "Высота", " 10 ", "м");

        ymlParams = parser.parseYmlParams("<?xml version=\"1.0\" encoding=\"windows-1251\"?>\n" +
                "<offer_params>" +
                "<param name=\"vendor\" unit=\"\">LEGO</param>" +
                "<param name=\"Развивающая\" unit=\"\">Да</param>" +
                "<param name=\"Тип\" unit=\"\">конструктор</param>" +
                "</offer_params>"
        );
        assertEquals(ymlParams.size(), 3);
        assertYmlParam(ymlParams.get(0), "vendor", "LEGO", "");
        assertYmlParam(ymlParams.get(1), "Развивающая", "Да", "");
        assertYmlParam(ymlParams.get(2), "Тип", "конструктор", "");

        ymlParams = parser.parseYmlParams("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<offer_params>" +
                "<param name=\"vendor\" unit=\"\">LEGO</param>" +
                "<param name=\"Развивающая\" unit=\"\">Да</param>" +
                "<param name=\"Тип\" unit=\"\">конструктор</param>" +
                "</offer_params>"
        );
        assertEquals(ymlParams.size(), 3);
        assertYmlParam(ymlParams.get(0), "vendor", "LEGO", "");
        assertYmlParam(ymlParams.get(1), "Развивающая", "Да", "");
        assertYmlParam(ymlParams.get(2), "Тип", "конструктор", "");
    }

    private void assertYmlParam(Offer.YmlParam ymlParam, String name, String value, String unit) {
        assertEquals(ymlParam.getName(), name);
        assertEquals(ymlParam.getValue(), value);
        assertEquals(ymlParam.getUnit(), unit);
    }
}
