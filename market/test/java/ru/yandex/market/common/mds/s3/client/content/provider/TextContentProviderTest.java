package ru.yandex.market.common.mds.s3.client.content.provider;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link TextContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class TextContentProviderTest {

    @Test
    public void testProvider() {
        final TextContentProvider provider = new TextContentProvider(TEST_DATA);
        checkProvider(provider);
    }


    public static void checkProvider(@Nonnull final TextContentProvider provider) {
        final String text = provider.getText();

        TestUtils.checkProvider(provider, TEST_DATA);
        assertThat(text, equalTo(TEST_DATA));
    }

}
