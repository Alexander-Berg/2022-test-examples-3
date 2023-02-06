package ru.yandex.market.common.mds.s3.client.service.factory;

import java.util.function.Predicate;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link ResourceValidatorFactory}.
 *
 * @author Vladislav Bauer
 */
public class ResourceValidatorFactoryTest {

    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(ResourceValidatorFactory.class);
    }

    @Test
    public void testNameWithPattern() {
        final Predicate<ResourceConfiguration> predicate =
            ResourceValidatorFactory.nameWithPattern(ResourceValidatorFactory.DEFAULT_CONFIGURATION_NAME_PATTERN);

        assertThat(predicate.test(configuration("vault-13")), equalTo(true));
        assertThat(predicate.test(configuration("gobbledegook")), equalTo(true));
        assertThat(predicate.test(configuration("bulls-and-cows")), equalTo(true));
        assertThat(predicate.test(configuration("BULLS-AND-COWS")), equalTo(false));
    }

    @Test
    public void testExtensionWithPattern() {
        final Predicate<ResourceConfiguration> predicate = ResourceValidatorFactory
            .extensionWithPattern(ResourceValidatorFactory.DEFAULT_CONFIGURATION_EXTENSION_PATTERN);

        assertThat(predicate.test(configurationExt("txt")), equalTo(true));
        assertThat(predicate.test(configurationExt("txt.gz")), equalTo(true));
        assertThat(predicate.test(configurationExt(null)), equalTo(true));
        assertThat(predicate.test(configurationExt(".txt")), equalTo(false));
        assertThat(predicate.test(configurationExt(".")), equalTo(false));
    }

    @Test
    public void testFolderWithPattern() {
        final Predicate<ResourceConfiguration> predicate = ResourceValidatorFactory
            .folderWithPattern(ResourceValidatorFactory.DEFAULT_CONFIGURATION_FOLDER_PATTERN);

        assertThat(predicate.test(configurationFolder("123")), equalTo(true));
        assertThat(predicate.test(configurationFolder("folder/key")), equalTo(true));
        assertThat(predicate.test(configurationFolder("folder_/key")), equalTo(false));
        assertThat(predicate.test(configurationFolder(null)), equalTo(true));
        assertThat(predicate.test(configurationFolder("FOLDER")), equalTo(false));
        assertThat(predicate.test(configurationFolder(".")), equalTo(false));
    }

    private ResourceConfiguration configurationExt(final String extension) {
        final ResourceConfiguration configuration = Mockito.mock(ResourceConfiguration.class);
        final ResourceFileDescriptor fileDescriptor =
            ResourceFileDescriptor.create(RandUtils.randomText(), extension);

        when(configuration.getFileDescriptor()).thenReturn(fileDescriptor);
        return configuration;
    }

    private ResourceConfiguration configurationFolder(final String folder) {
        final ResourceConfiguration configuration = Mockito.mock(ResourceConfiguration.class);
        when(configuration.getFileDescriptor()).thenReturn(ResourceFileDescriptor.create("a", null, folder));
        return configuration;
    }

    private ResourceConfiguration configuration(final String name) {
        final ResourceConfiguration configuration = Mockito.mock(ResourceConfiguration.class);
        when(configuration.getFileDescriptor()).thenReturn(ResourceFileDescriptor.create(name));
        return configuration;
    }

}
