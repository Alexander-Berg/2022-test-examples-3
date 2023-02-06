package ru.yandex.market.notification.simple.model.data;

import java.util.UUID;

import org.junit.Test;

import ru.yandex.market.notification.test.model.AbstractModelTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link TextWithTitleData}.
 *
 * @author Vladislav Bauer
 */
public class TextWithTitleDataTest extends AbstractModelTest {

    @Test
    public void testConstruction() {
        final String title = UUID.randomUUID().toString();
        final String text = UUID.randomUUID().toString();
        final TextWithTitleData data = new TextWithTitleData(title, text);

        assertThat(data.getTitle(), equalTo(title));
        assertThat(data.getText(), equalTo(text));
    }

    @Test
    public void testBasicMethods() {
        final TextWithTitleData data = new TextWithTitleData("", "");
        final TextWithTitleData sameData = new TextWithTitleData("", "");
        final TextWithTitleData otherData = new TextWithTitleData("title", "text");

        checkBasicMethods(data, sameData, otherData);
    }

}
