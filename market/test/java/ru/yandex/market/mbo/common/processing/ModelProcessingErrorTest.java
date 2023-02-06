package ru.yandex.market.mbo.common.processing;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author s-ermakov
 */
public class ModelProcessingErrorTest {
    @Test
    public void testEmptyText() throws Exception {
        ModelProcessingError error = new ModelProcessingError();

        String text = error.getText();

        assertEquals("", text);
    }

    @Test
    public void testSimpleMessageText() throws Exception {
        ModelProcessingError error = new ModelProcessingError("test-group", "my-message", false, true);

        String text = error.getText();

        assertEquals("my-message", text);
    }

    @Test
    public void testSimpleMessageInNonCriticalError() throws Exception {
        ModelProcessingError error = new ModelProcessingError("test-group", "my-message", false, false);

        String text = error.getText();

        assertEquals("my-message", text);
    }

    @Test
    public void testFormattedText() throws Exception {
        String template = "<nick> is <name>";
        Map<String, String> params = ImmutableMap.of("<nick>", "s-ermakov", "<name>", "Sergey Ermakov");

        ModelProcessingError error = new ModelProcessingError("test-group", template, params, false, true);
        String text = error.getText();

        assertEquals("s-ermakov is Sergey Ermakov", text);
    }

    @Test
    public void testFormattedTextInNonCriticalError() throws Exception {
        String template = "<nick> is <name>";
        Map<String, String> params = ImmutableMap.of("<nick>", "s-ermakov", "<name>", "Sergey Ermakov");

        ModelProcessingError error = new ModelProcessingError("test-group", template, params, false, false);
        String text = error.getText();

        assertEquals("s-ermakov is Sergey Ermakov", text);
    }

    @Test
    public void testFormattedTextWithEmptyParams() throws Exception {
        String template = "<nick> is <name>";
        ModelProcessingError error = new ModelProcessingError("test-group", template, null, false, true);
        String text = error.getText();

        assertEquals(template, text);
    }

    @Test
    public void testFormattedTextWithEmptyTemplate() throws Exception {
        ModelProcessingError error = new ModelProcessingError("test-group", null, null, false, true);
        String text = error.getText();

        assertEquals("", text);
    }
}
