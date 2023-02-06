package ru.yandex.market.abo.cpa.returned;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author imelnikov
 */
public class MarshrutExcelParserTest {

    @Test
    public void parseCsv() {
        InputStream io = MarshrutExcelParser.class.getResourceAsStream("/returned/marshrut-return.xls");

        List<ReturnedItem> list = MarshrutExcelParser.parse(io);
        assertFalse(list.isEmpty());
    }
}
