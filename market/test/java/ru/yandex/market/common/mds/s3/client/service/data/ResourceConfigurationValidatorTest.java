package ru.yandex.market.common.mds.s3.client.service.data;

import java.util.Collection;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.service.data.impl.ResourceConfigurationValidatorImpl;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceValidatorFactory;
import ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory;

/**
 * Unit-тесты для {@link ResourceConfigurationValidator}.
 *
 * @author Vladislav Bauer
 */
public class ResourceConfigurationValidatorTest {

    private static final String NEGATIVE_CONFIGURATION_NAME_PATTERN = "[A-Z]+";
    private static final String NEGATIVE_CONFIGURATION_FOLDER_PATTERN = "[A-Z]+";
    private static final String NEGATIVE_CONFIGURATION_EXTENSION_PATTERN = "[.].*";
    private static final String BUCKET = "bucket";

    @Test
    public void testValidateNamePositive() {
        check(new ResourceConfigurationValidatorImpl());

        check(new ResourceConfigurationValidatorImpl(
            ResourceValidatorFactory.nameWithPattern(ResourceValidatorFactory.DEFAULT_CONFIGURATION_NAME_PATTERN)
        ));
    }

    @Test(expected = MdsS3Exception.class)
    public void testValidateNameNegative() {
        check(new ResourceConfigurationValidatorImpl());

        check(new ResourceConfigurationValidatorImpl(
            ResourceValidatorFactory.nameWithPattern(NEGATIVE_CONFIGURATION_NAME_PATTERN)
        ));
    }

    @Test
    public void testValidateFolderPositive() {
        check(new ResourceConfigurationValidatorImpl());

        check(new ResourceConfigurationValidatorImpl(
            ResourceValidatorFactory.folderWithPattern(ResourceValidatorFactory.DEFAULT_CONFIGURATION_FOLDER_PATTERN)
        ));
    }

    @Test(expected = MdsS3Exception.class)
    public void testValidateFolderNegative() {
        check(new ResourceConfigurationValidatorImpl());

        check(new ResourceConfigurationValidatorImpl(
            ResourceValidatorFactory.folderWithPattern(NEGATIVE_CONFIGURATION_FOLDER_PATTERN)
        ));
    }

    @Test
    public void testValidateExtensionPositive() {
        check(new ResourceConfigurationValidatorImpl(ResourceValidatorFactory
            .extensionWithPattern(ResourceValidatorFactory.DEFAULT_CONFIGURATION_EXTENSION_PATTERN)));
    }

    @Test(expected = MdsS3Exception.class)
    public void testValidateExtensionNegative() {
        check(new ResourceConfigurationValidatorImpl(
            ResourceValidatorFactory.extensionWithPattern(NEGATIVE_CONFIGURATION_EXTENSION_PATTERN)
        ));
    }


    private void check(final ResourceConfigurationValidator validator) {
        final ResourceConfigurationProvider provider =
            ResourceConfigurationProviderFactory.create(BUCKET, true);
        final Collection<ResourceConfiguration> configurations = provider.getConfigurations();

        validator.validate(configurations);
    }

}
