package ru.yandex.market.common.mds.s3.client.model;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.test.TestUtils;

/**
 * Unit-тесты для {@link ResourceLocation}.
 *
 * @author Vladislav Bauer
 */
public class ResourceLocationTest {

    @Test
    public void testBasicMethods() {
        TestUtils.checkEqualsAndHashCodeContract(ResourceLocation.class);
    }

}
