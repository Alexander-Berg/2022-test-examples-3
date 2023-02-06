package ru.yandex.market.common.mds.s3.client.model;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link ResourceFileDescriptor}.
 *
 * @author Vladislav Bauer
 */
public class ResourceFileDescriptorTest {

    private static final String NAME = "name";
    private static final String EXT = "ext";
    private static final String FOLDER = "folder";


    @Test
    public void testBasicMethods() {
        TestUtils.checkEqualsAndHashCodeContract(ResourceFileDescriptor.class);
    }

    @Test
    public void testParsePositive() {
        check(ResourceFileDescriptor.parse("name"), NAME, null, null);
        check(ResourceFileDescriptor.parse("name.ext"), NAME, EXT, null);
        check(ResourceFileDescriptor.parse("name.ext.gz"), NAME, "ext.gz", null);
        check(ResourceFileDescriptor.parse("folder/name"), NAME, null, FOLDER);
        check(ResourceFileDescriptor.parse("folder/name.ext"), NAME, EXT, FOLDER);
        check(ResourceFileDescriptor.parse("folder/name.ext.gz"), NAME, "ext.gz", FOLDER);
        check(ResourceFileDescriptor.parse("folder/folder/name.ext.gz"), NAME, "ext.gz", "folder/folder");
    }

    @Test(expected = MdsS3Exception.class)
    public void testParseNegative() {
        fail(String.valueOf(ResourceFileDescriptor.parse(StringUtils.EMPTY)));
    }

    @Test
    public void testCreatePositive() {
        check(ResourceFileDescriptor.create(NAME, EXT), NAME, EXT, null);
        check(ResourceFileDescriptor.create(NAME, null), NAME, null, null);
        check(ResourceFileDescriptor.create(NAME, EXT, FOLDER), NAME, EXT, FOLDER);
        check(ResourceFileDescriptor.create(NAME, null, null), NAME, null, null);
    }

    @Test(expected = MdsS3Exception.class)
    public void testCreateNegative() {
        fail(String.valueOf(ResourceFileDescriptor.create(StringUtils.EMPTY, EXT)));
    }


    private void check(
        final ResourceFileDescriptor fileDescriptor,
        final String name,
        final String extension,
        final String folder
    ) {
        assertThat(fileDescriptor.getName(), equalTo(name));
        assertThat(fileDescriptor.getExtension().orElse(null), equalTo(extension));
        assertThat(fileDescriptor.getFolder().orElse(null), equalTo(folder));
    }

}
