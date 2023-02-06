package ru.yandex.market.notification.common.model;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;
import ru.yandex.market.notification.test.util.DataSerializerUtils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link TextNotificationContent}.
 *
 * @author Vladislav Bauer
 */
public class TextNotificationContentTest extends AbstractModelTest {

    private static final String LAST_LINE = "I had to stop for the night";
    private static final String TEXT =
        "On a dark desert highway, cool wind in my hair\r\n"
        + "Warm smell of colitas, rising up through the air\r\n"
        + "Up ahead in the distance, I saw a shimmering light\r\n"
        + "My head grew heavy and my sight grew dim\r\n"
        + LAST_LINE;


    @Test
    public void testBasicMethods() {
        final String text1 = "one";
        final String text2 = "two";

        final TextNotificationContent data = TextNotificationContent.create(text1);
        final TextNotificationContent sameData = TextNotificationContent.create(text1);
        final TextNotificationContent otherData = TextNotificationContent.create(text2);

        checkBasicMethods(data, sameData, otherData);
    }

    @Test
    public void testSerialization() {
        final TextNotificationContent data = TextNotificationContent.create(TEXT);
        final String serialized = DataSerializerUtils.serializeToString(data);

        assertThat(serialized, containsString(LAST_LINE));
    }

    @Test
    public void testDeserialization() throws Exception {
        final TextNotificationContent content =
            DataSerializerUtils.deserializeFromResource(TextNotificationContent.class);

        assertThat(content, notNullValue());
        assertThat(content.getText(), equalTo(TEXT));
    }

}
