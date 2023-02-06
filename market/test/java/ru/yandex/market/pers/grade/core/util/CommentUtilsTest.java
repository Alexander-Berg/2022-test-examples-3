package ru.yandex.market.pers.grade.core.util;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.comments.model.Comment;
import ru.yandex.market.comments.model.legacy.Comment2;

import java.util.Map;

/**
 * @author dinyat
 *         03/04/2017
 */
public class CommentUtilsTest {

    @Test
    public void testGetParamsWithParams() throws Exception {
        String body = "<title></title>\n" +
            "<body>комментарий</body>\n" +
            "<params>\n" +
            "\t<param>\n" +
            "\t\t<name>projectId</name>\n" +
            "\t\t<value>9</value>\n" +
            "\t</param>\n" +
            "\t<param>\n" +
            "\t\t<name>anotherParam</name>\n" +
            "\t\t<value>anotherValue</value>\n" +
            "\t</param>\n" +
            "</params>\n";
        Comment comment = new Comment(new Comment2(null, null, null, 0, 0L, body, false, null));

        Map<String, String> params = CommentUtils.getParams(comment);

        Assert.assertEquals(2, params.size());
        Assert.assertEquals("9", params.get("projectId"));
        Assert.assertEquals("anotherValue", params.get("anotherParam"));
    }

    @Test
    public void testGetParamsWithoutParams() throws Exception {
        String body = "<title></title>\n" +
            "<body>комментарий</body>\n" +
            "<params>\n" +
            "</params>\n";

        Comment comment = new Comment(new Comment2(null, null, null, 0, 0L, body, false, null));

        Map<String, String> params = CommentUtils.getParams(comment);

        Assert.assertNull(params.get("projectId"));
    }

}