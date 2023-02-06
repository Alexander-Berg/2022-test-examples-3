package ru.yandex.market.mbo.test;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 09.02.2018
 */
public class InjectResourcesTest {

    private static final int EXPECTED_IMAGE_SIZE = 10772;
    private static final int EXPECTED_TEXT_SIZE = 970;
    private static final int TEST_TEXT_BEGIN = 337;

    private static final int MUTABLE_BYTE_INDEX = 23;
    private static final byte ORIGINAL_BYTE = (byte) 67;

    @Rule
    public InjectResources resource = new InjectResources(this);

    @InjectResource("/mbo-core/test-image-1.jpeg")
    private byte[] originalImage;

    @InjectResource("/mbo-core/test-long-text.txt")
    private String textInject;

    @Test
    public void testInjectBytes() {
        assertThat(originalImage, is(notNullValue()));
        assertThat(originalImage.length, is(EXPECTED_IMAGE_SIZE));
    }

    @Test
    public void testInjectString() {
        assertThat(textInject, is(notNullValue()));
        assertThat(textInject.length(), is(EXPECTED_TEXT_SIZE));
        String expectedSubstring = "Рефрен специфицирует разрыв функции";
        assertThat(textInject.substring(TEST_TEXT_BEGIN, TEST_TEXT_BEGIN + expectedSubstring.length()),
            is(expectedSubstring));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testNoInfluence1() {
        assertThat("has original content", originalImage[MUTABLE_BYTE_INDEX], is(ORIGINAL_BYTE));
        originalImage[MUTABLE_BYTE_INDEX] = 42; // change
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testNoInfluence2() {
        assertThat("has original content", originalImage[MUTABLE_BYTE_INDEX], is(ORIGINAL_BYTE));
        originalImage[MUTABLE_BYTE_INDEX] = 57; // change
    }
}
