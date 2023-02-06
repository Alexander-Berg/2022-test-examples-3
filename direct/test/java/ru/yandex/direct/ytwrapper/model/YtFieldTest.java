package ru.yandex.direct.ytwrapper.model;

import org.junit.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class YtFieldTest {
    private static final long TEST_NUM_ONE = 123456L;
    private static final long TEST_NUM_TWO = 654321L;
    private static final String TEST_STR = "TEST";

    @Test
    public void testEquals() {
        YtField<Long> longYtFieldOne = new YtField<>("field", Long.class);
        YtField<Long> longYtFieldTwo = new YtField<>("field", Long.class);
        assertThat("Two fields equal", longYtFieldOne, equalTo(longYtFieldTwo));
    }

    @Test
    public void testNotEquals() {
        YtField<Long> longYtFieldOne = new YtField<>("field", Long.class);
        YtField<Long> longYtFieldTwo = new YtField<>("field2", Long.class);
        YtField<String> longYtFieldThree = new YtField<>("field", String.class);

        assertThat("Two fields with different names do not equal", longYtFieldOne, not(equalTo(longYtFieldTwo)));
        assertThat("Two fields with different types do not equal", longYtFieldOne, not(equalTo(longYtFieldThree)));
    }

    @Test
    public void testValues() {
        YtField<Long> longYtFieldOne = new YtField<>("field", Long.class);
        YTreeMapNode node = YTree.mapBuilder().buildMap();
        longYtFieldOne.insertValue(node, TEST_NUM_ONE);

        assertThat("Extracted value is correct", longYtFieldOne.extractValue(node, TEST_NUM_TWO),
                equalTo(TEST_NUM_ONE));
    }

    @Test
    public void testValuesDefault() {
        YtField<Long> longYtFieldOne = new YtField<>("field", Long.class);
        YTreeMapNode node = YTree.mapBuilder().buildMap();

        assertThat("Default value is correct", longYtFieldOne.extractValue(node, TEST_NUM_TWO), equalTo(TEST_NUM_TWO));
    }

    @Test
    public void testValuesEqualFields() {
        YtField<Long> longYtFieldOne = new YtField<>("field", Long.class);
        YTreeMapNode node = YTree.mapBuilder().buildMap();
        longYtFieldOne.insertValue(node, TEST_NUM_ONE);

        YtField<Long> longYtFieldTwo = new YtField<>("field", Long.class);
        assertThat("Equal fields return equal values", longYtFieldTwo.extractValue(node, TEST_NUM_TWO),
                equalTo(TEST_NUM_ONE));
    }

    @Test
    public void testValuesStringType() {
        YtField<String> longYtFieldOne = new YtField<>("field", String.class);
        YTreeMapNode node = YTree.mapBuilder().buildMap();
        longYtFieldOne.insertValue(node, TEST_STR);

        assertThat("Equal fields return equal values", longYtFieldOne.extractValue(node, null), equalTo(TEST_STR));
    }

    @Test(expected = ClassCastException.class)
    public void testCastException() {
        YtField<String> longYtFieldOne = new YtField<>("field", String.class);
        YTreeMapNode node = YTree.mapBuilder().buildMap();
        longYtFieldOne.insertValue(node, TEST_STR);

        YtField<Long> longYtFieldTwo = new YtField<>("field", Long.class);
        longYtFieldTwo.extractValue(node, null);
    }

    @Test()
    public void testTypeOverwrite() {
        YtField<String> longYtFieldOne = new YtField<>("field", String.class);
        YTreeMapNode node = YTree.mapBuilder().buildMap();
        longYtFieldOne.insertValue(node, TEST_STR);

        YtField<Long> longYtFieldTwo = new YtField<>("field", Long.class);
        longYtFieldTwo.insertValue(node, TEST_NUM_ONE);

        assertThat("Extracted value is correct", longYtFieldTwo.extractValue(node, TEST_NUM_TWO),
                equalTo(TEST_NUM_ONE));
    }

    @Test()
    public void testValueOverwrite() {
        YtField<Long> longYtFieldOne = new YtField<>("field", Long.class);
        YTreeMapNode node = YTree.mapBuilder().buildMap();
        longYtFieldOne.insertValue(node, TEST_NUM_ONE);
        assertThat("Extracted value is correct", longYtFieldOne.extractValue(node, null), equalTo(TEST_NUM_ONE));

        longYtFieldOne.insertValue(node, TEST_NUM_TWO);
        assertThat("Extracted value is changed and correct", longYtFieldOne.extractValue(node, null),
                equalTo(TEST_NUM_TWO));
    }
}
