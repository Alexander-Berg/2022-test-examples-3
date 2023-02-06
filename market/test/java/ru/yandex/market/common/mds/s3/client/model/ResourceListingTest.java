package ru.yandex.market.common.mds.s3.client.model;

import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link ResourceListing}.
 *
 * @author Vladislav Bauer
 */
public class ResourceListingTest {

    @Test
    public void testBasicMethods() {
        TestUtils.checkEqualsAndHashCodeContract(ResourceListing.class);
    }

    @Test(expected = MdsS3Exception.class)
    public void testBucketNameNegative() {
        final ResourceListing listing = ResourceListing.create(
            StringUtils.EMPTY, Collections.emptyList(), Collections.emptyList()
        );

        fail(String.valueOf(listing));
    }

}
