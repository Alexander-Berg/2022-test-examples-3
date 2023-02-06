package ru.yandex.market.pers.grade.admin.db;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.translate.UnicodeEscaper;
import org.junit.Test;

import ru.yandex.common.framework.db.DataRow;

/**
 * Test grades with special unicode characters
 *
 * @author imelnikov
 */
public class EscapeGradeTest {
    @Test
    public void escapeThumbUp() throws Exception {
        DataRow row = new DataRow("grade");
        row.put("text", "классный 👍");
        StringBuilder st = new StringBuilder();
        row.toXml(st);
//		System.out.println(st);

        StringEscapeUtils.ESCAPE_XML.with(new UnicodeEscaper());
        String escape = StringEscapeUtils.escapeXml("👍");
        System.out.println(escape);

        System.out.println(StringEscapeUtils.unescapeXml(escape));
    }
}
