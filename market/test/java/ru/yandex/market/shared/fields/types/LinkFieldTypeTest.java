package ru.yandex.market.shared.fields.types;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.robot.shared.fields.types.LinkFieldType;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 07.09.11
 */
public class LinkFieldTypeTest extends Assert {

    private final LinkFieldType linkFieldType = new LinkFieldType();

    @Test
    public void testParse() throws Exception {
        assertEquals("http://example.com/image.jpg", parse("http://example.com/image.jpg", "http://example.com/test.php"));
        assertEquals("http://example.com/image.jpg", parse("image.jpg", "http://example.com/test.php"));
        assertEquals("http://example.com/image.jpg", parse("/image.jpg", "http://example.com/test.php"));
        assertEquals("http://example.com/image.jpg", parse("/image.jpg", "http://example.com/test/test.php"));
        assertEquals("http://st.example.com/image.jpg", parse("//st.example.com/image.jpg", "http://example.com/"));
        assertEquals("https://st.example.com/image.jpg", parse("//st.example.com/image.jpg", "https://example.com/"));
        assertEquals("http://example.com/test/image.jpg", parse("image.jpg", "http://example.com/test/test.php"));
        assertEquals("http://t.co/image.jpg", parse("/image.jpg", "http://t.co/test/test.php"));
        assertEquals("http://www.anindasatinal.com/modules/catalog/products/pr_01_1923_min.jpg", parse("modules/catalog/products/pr_01_1923_min.jpg", "http://www.anindasatinal.com/index.php?do=catalog/product&pid=1923"));
        assertEquals("http://www.anindasatinal.com/modules/catalog/products/pr_01_1923_min.jpg", parse("modules/catalog/products/pr_01_1923_min.jpg", "http://www.anindasatinal.com/index.php#do=catalog/product&pid=1923"));
    }

    private String parse(String imageUrl, String pageUrl) {
        return (String) linkFieldType.parse(imageUrl, pageUrl);
    }

}
