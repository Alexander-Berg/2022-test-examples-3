package ru.yandex.market.mbo.synchronizer.export;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author dmserebr
 * @date 21/03/2019
 */
public class DumpTableToXMLTest {

    @Test
    public void testContainsUnacceptableCharacters() {
        Assertions.assertThat(DumpTableToXML.containsUnacceptableCharacters(null)).isFalse();
        Assertions.assertThat(DumpTableToXML.containsUnacceptableCharacters("")).isFalse();
        Assertions.assertThat(DumpTableToXML.containsUnacceptableCharacters("test")).isFalse();
        Assertions.assertThat(DumpTableToXML.containsUnacceptableCharacters("Привет!")).isFalse();
        Assertions.assertThat(DumpTableToXML.containsUnacceptableCharacters("123 \t\n")).isFalse();
        Assertions.assertThat(DumpTableToXML.containsUnacceptableCharacters("中華")).isFalse();
        Assertions.assertThat(DumpTableToXML.containsUnacceptableCharacters("\uD83D\uDC4Cили \uD83D\uDC4E")).isTrue();
        Assertions.assertThat(DumpTableToXML.containsUnacceptableCharacters("\uD83D")).isTrue();
    }
}
