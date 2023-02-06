package ru.yandex.market.mbi.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit тесты для {@link StringUtilsTest}.
 *
 * @author avetokhin 17/01/17.
 */
class StringUtilsTest {

    @Test
    void splitToIntegerTestPositive() {
        assertThat(StringUtils.splitToInteger("1, 2,4 ,5,,,", ",")).containsExactly(1, 2, 4, 5);
    }

    @Test
    void splitToIntegerTestNegative() {
        assertThatExceptionOfType(NumberFormatException.class)
                .isThrownBy(() -> StringUtils.splitToInteger("1, 2,B4 ,5", ","));
    }

    @Test
    void truncateToBytesDontNeedToTruncate() {
        final int maxBytesCount = 1024;
        final String testString = "I have got a toy. It is not my toy, it is my child's";
        assertThat(StringUtils.truncateToBytes(testString, maxBytesCount))
                .as("Strings should be the same")
                .isEqualTo(testString);
    }

    @Test
    void truncateToBytesNeewToTruncate() {
        final int maxBytesCount = 100;
        final String longTestString = "Я люблю есть сосиски. Вкусные, разные, из " +
                "говядины, свинины и конины. А еще из курицы.";
        final String expectedTruncatedString = "Я люблю есть сосиски. Вкусные, разные, из говядины, свин";
        assertThat(StringUtils.truncateToBytes(longTestString, maxBytesCount))
                .as("Truncated string is not as expected")
                .isEqualTo(expectedTruncatedString);
    }

    @Test
    void testHasInvalidXmlCharactersTrue() {
        assertThat(StringUtils.hasInvalidXmlCharacters("sd\0x1eFgf")).isTrue();
        assertThat(StringUtils.hasInvalidXmlCharacters("sd\u001EFgf")).isTrue();
        assertThat(StringUtils.hasInvalidXmlCharacters("sdgf\0xB")).isTrue();
        assertThat(StringUtils.hasInvalidXmlCharacters("\0xD8AEsdgf")).isTrue();
    }

    @Test
    void testHasInvalidXmlCharactersFalse() {
        assertThat(StringUtils.hasInvalidXmlCharacters("sd   Fgf")).isFalse();
        assertThat(StringUtils.hasInvalidXmlCharacters("впвЫй.!ё\"№%:,.;()")).isFalse();
    }
}
