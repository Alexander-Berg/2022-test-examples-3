package ru.yandex.market.common.mds.s3.client.util;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link KeyUtils}.
 *
 * @author Vladislav Bauer
 */
public class KeyUtilsTest {

    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(KeyUtils.class);
    }

    @Test
    public void testConcatPrefixesVarargs() {
        final String key1 = "/key1";
        final String key2 = "/key2";

        assertThat(KeyUtils.concatPrefixes(key1, key2), equalTo("key1/key2"));
        assertThat(KeyUtils.concatPrefixes(key1, StringUtils.EMPTY), equalTo("key1"));
    }

    @Test
    public void testConcatPrefixesWithNullAndEmpty() {
        final String key = "key1";

        assertThat(KeyUtils.concatPrefixes(StringUtils.EMPTY, key), equalTo(key));
        assertThat(KeyUtils.concatPrefixes(key, StringUtils.EMPTY), equalTo(key));
    }

    @Test
    public void testConcatPrefixes() {
        final String[] parts = {"key1", "key2", "key3"};
        final int amount = 2;

        assertThat(KeyUtils.concatPrefixes(parts, amount), equalTo("key1/key2/"));
    }

    @Test
    public void testCleanKey() {
        assertThat(KeyUtils.cleanKey("/key1/"), equalTo("key1"));
        assertThat(KeyUtils.cleanKey("key1"), equalTo("key1"));
        assertThat(KeyUtils.cleanKey(KeyGenerator.DELIMITER_FOLDER), equalTo(StringUtils.EMPTY));
        assertThat(KeyUtils.cleanKey(StringUtils.EMPTY), equalTo(StringUtils.EMPTY));
    }

    @Test
    public void testAmountOfFolders() {
        assertThat(KeyUtils.amountOfFolders(null), equalTo(0));
        assertThat(KeyUtils.amountOfFolders(StringUtils.EMPTY), equalTo(0));
        assertThat(KeyUtils.amountOfFolders("/folder/"), equalTo(1));
        assertThat(KeyUtils.amountOfFolders("folder"), equalTo(1));
        assertThat(KeyUtils.amountOfFolders("/folder"), equalTo(1));
        assertThat(KeyUtils.amountOfFolders("folder/"), equalTo(1));
        assertThat(KeyUtils.amountOfFolders("/folder/folder/"), equalTo(2));
        assertThat(KeyUtils.amountOfFolders("folder/folder"), equalTo(2));
    }

}
