package ru.yandex.market.common.mds.s3.client.content.provider;

import com.amazonaws.util.StringInputStream;
import org.junit.Test;

import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.checkProvider;

/**
 * Unit-тесты для {@link StreamContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class StreamContentProviderTest  {

    @Test
    public void testProvider() throws Exception {
        try (StringInputStream stream = new StringInputStream(TEST_DATA)) {
            final StreamContentProvider provider = new StreamContentProvider(stream);

            checkProvider(provider, TEST_DATA);
        }
    }

}
