package ru.yandex.market.acw.utils;

import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilsTest {

    @Test
    void convertMd5HexToUUID_test() {
        String goodHexText = "11e0f3a017e34b7ab260f8f25964d680";
        String badHexText1 = "11e0f3a017e34b7ab260f8f25964d6ff1";
        String badHexText2 = "";
        String badHexText3 = null;

        assertThat(HashUtils.convertMd5HexToUUID(goodHexText)).isEqualTo(
                UUID.fromString("11e0f3a0-17e3-4b7a-b260-f8f25964d680"));
        assertThat(HashUtils.convertMd5HexToUUID(badHexText1)).isNull();
        assertThat(HashUtils.convertMd5HexToUUID(badHexText2)).isNull();
        assertThat(HashUtils.convertMd5HexToUUID(badHexText3)).isNull();
    }

    @Test
    void convertTextToMD5UUID_test() {
        String text1 = "Just some random text to try out hash building";
        String text2 = text1 + ".";

        UUID hash1 = HashUtils.convertTextToMD5UUID(text1);
        UUID hash1_2 = HashUtils.convertTextToMD5UUID(text1);
        assertThat(hash1).isEqualTo(hash1_2);

        UUID hash2 = HashUtils.convertTextToMD5UUID(text2);
        assertThat(hash2).isNotEqualTo(hash1);
    }
}
