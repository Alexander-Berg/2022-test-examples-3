package ru.yandex.market.abo.core.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author kukabara
 * @see https
 * ://issues.apache.org/jira/browse/LANG-859?page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel
 * <p>
 * According to specification of XML version 1.0 there are Unicode characters that are not allowed in the content
 * of the XML document http://www.w3.org/TR/xml/#charsets
 * Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF] any Unicode character,
 * excluding the surrogate blocks, FFFE, and FFFF.
 * <p>
 * Don't use:
 * org.apache.commons.lang.StringEscapeUtils.escapeXml(s)
 * org.apache.commons.lang3.StringEscapeUtils.escapeXml(s)
 */
public class EscapeTest {

    public static boolean isValidChar(char ch) {
        return ch == 0x9 || ch == 0xA || ch == 0xD || ch >= 0x20 && ch <= 0xD7FF || ch > 0xE000 && ch <= 0xFFFD ||
                ch >= 0x10000 && ch <= 0x10FFFF;
    }

    public static boolean isValidString(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!isValidChar(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void test() throws Exception {
        assertTrue(isValidChar('A'));
        assertTrue(isValidChar('Я'));
        assertTrue(isValidChar('Ґ'));
        assertTrue(isValidChar((char) 0x2738));

        assertFalse(isValidChar((char) 55296));
        assertFalse(isValidChar((char) 0xD83D));
    }

}
