package ru.yandex.market.mbi.bot.tg.service.impl;

import java.util.Queue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TextSplitterTest {

    private static final String TEXT = "Lorem Ipsum - это текст-\"рыба\", часто используемый в печати и вэб-дизайне. " +
            "Lorem Ipsum является стандартной \"рыбой\" для текстов на латинице с начала XVI века."; // length = 158

    @Test
    public void testSplitText() {
        TextSplitter splitter = new TextSplitter();

        Queue<String> messages = splitter.split(TEXT, TEXT.length());
        Assertions.assertEquals(TEXT, messages.remove());
        Assertions.assertTrue(messages.isEmpty());

        messages = splitter.split(TEXT, 4096);
        Assertions.assertEquals(TEXT, messages.remove());
        Assertions.assertTrue(messages.isEmpty());

        messages = splitter.split(TEXT, 100);
        Assertions.assertEquals("Lorem Ipsum - это текст-\"рыба\", часто используемый в печати и вэб-дизайне. Lorem Ipsum" +
                " является ", messages.remove());
        Assertions.assertEquals("стандартной \"рыбой\" для текстов на латинице с начала XVI века.", messages.remove());
        Assertions.assertTrue(messages.isEmpty());

        messages = splitter.split(TEXT, 50);
        Assertions.assertEquals("Lorem Ipsum - это текст-\"рыба\", часто используемый ", messages.remove());
        Assertions.assertEquals("в печати и вэб-дизайне. Lorem Ipsum является ", messages.remove());
        Assertions.assertEquals("стандартной \"рыбой\" для текстов на латинице с ", messages.remove());
        Assertions.assertEquals("начала XVI века.", messages.remove());
        Assertions.assertTrue(messages.isEmpty());

        messages = splitter.split(TEXT, 20);
        Assertions.assertEquals("Lorem Ipsum - это ", messages.remove());
        Assertions.assertEquals("текст-\"рыба\", часто ", messages.remove());
        Assertions.assertEquals("используемый в ", messages.remove());
        Assertions.assertEquals("печати и ", messages.remove());
        Assertions.assertEquals("вэб-дизайне. Lorem ", messages.remove());
        Assertions.assertEquals("Ipsum является ", messages.remove());
        Assertions.assertEquals("стандартной \"рыбой\" ", messages.remove());
        Assertions.assertEquals("для текстов на ", messages.remove());
        Assertions.assertEquals("латинице с начала ", messages.remove());
        Assertions.assertEquals("XVI века.", messages.remove());
        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    public void testWithDifferentSpaceChars() {
        TextSplitter splitter = new TextSplitter();
        Queue<String> messages = splitter.split(
                "This life’s not like you wanted it\r\n*\tHis eyes, I can see again, \tI need you here\n\n" +
                        "*\tIn your mind nobody’s listening\n\n", 36);

        Assertions.assertEquals("This life’s not like you wanted it\r\n", messages.remove());
        Assertions.assertEquals("*\tHis eyes, I can see again, \tI need ", messages.remove());
        Assertions.assertEquals("you here\n\n*\tIn your mind nobody’s ", messages.remove());
        Assertions.assertEquals("listening\n\n", messages.remove());

        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    public void testWithDifferentSpaceCharsLessMaxLimit() {
        String text = "This life’s not like you wanted it\r\n*\tHis eyes, I can see again, \tI need you here\n\n" +
                "*\tIn your mind nobody’s listening\n";

        TextSplitter splitter = new TextSplitter();
        Queue<String> messages = splitter.split(text, 116);

        Assertions.assertEquals(text, messages.remove());

        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    public void testWithDifferentSpaceCharsCornerCase() {
        String text = "This life’s not like you wanted it\r\n*\tHis eyes, I can see again, \tI need you here\n\n" +
                "*\tIn your mind nobody’s listening\n";

        TextSplitter splitter = new TextSplitter();
        Queue<String> messages = splitter.split(text, 115);

        Assertions.assertEquals("This life’s not like you wanted it\r\n*\tHis eyes, I can see again, \tI need you here\n\n" +
                "*\tIn your mind nobody’s ", messages.remove());
        Assertions.assertEquals("listening\n", messages.remove());

        Assertions.assertTrue(messages.isEmpty());
    }
}
