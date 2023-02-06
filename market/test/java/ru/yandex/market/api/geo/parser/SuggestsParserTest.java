package ru.yandex.market.api.geo.parser;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.api.geo.domain.AddressSuggestV2;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 * Created by apershukov on 20.10.16.
 */
public class SuggestsParserTest {

    private SuggestsParser parser;

    @Before
    public void setUp() {
        parser = new SuggestsParser();
    }

    @Test
    public void testParseSuggestList() throws Exception {
        List<AddressSuggestV2> suggest = parser.parse(ResourceHelpers.getResource("address-suggest.js"));

        assertEquals(4, suggest.size());

        assertEquals("микрорайон Ленинские Горы, Москва, Россия", suggest.get(0).getFullAddress());
        assertEquals("микрорайон Ленинские Горы", suggest.get(0).getShortAddress());
        assertEquals("Москва, Россия", suggest.get(0).getDesc());

        assertEquals("улица Лебедева, микрорайон Ленинские Горы, Москва, Россия", suggest.get(1).getFullAddress());
        assertEquals("улица Лебедева", suggest.get(1).getShortAddress());
        assertEquals("микрорайон Ленинские Горы, Москва, Россия", suggest.get(1).getDesc());

        assertEquals("Менделеевская улица, микрорайон Ленинские Горы, Москва, Россия", suggest.get(2).getFullAddress());
        assertEquals("Менделеевская улица", suggest.get(2).getShortAddress());
        assertEquals("микрорайон Ленинские Горы, Москва, Россия", suggest.get(2).getDesc());

        assertEquals("улица Академика Хохлова, микрорайон Ленинские Горы, Москва, Россия", suggest.get(3).getFullAddress());
        assertEquals("улица Академика Хохлова", suggest.get(3).getShortAddress());
        assertEquals("микрорайон Ленинские Горы, Москва, Россия", suggest.get(3).getDesc());
    }
}
