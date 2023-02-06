package ru.yandex.market.common.mds.s3.spring.service.impl;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory;
import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.common.mds.s3.spring.model.ResourceConfigurationStatus;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.BUCKET_NAME;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.MODULE_NAME;

/**
 * * Unit-тесты для {@link MdsS3ResourceConfigurationDetectorImpl}
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@RunWith(MockitoJUnitRunner.class)
public class MdsS3ResourceConfigurationDetectorImplTest {

    @Mock
    private ResourceConfigurationDao dao;

    @Mock
    private ResourceConfigurationProvider provider;

    @Test
    public void detectChanges() {
        final ResourceConfigurationProvider configurationProvider =
                ResourceConfigurationProviderFactory.create(BUCKET_NAME, true);
        final MdsS3ResourceConfigurationDetectorImpl instance = createDetector(null, configurationProvider);

        instance.detectChanges();

        final Collection<ResourceConfiguration> configurations = configurationProvider.getConfigurations();
        verify(dao, times(1))
                .setStatusNotFor(MODULE_NAME, ResourceConfigurationStatus.NOT_EXISTS, configurations);

        for (final ResourceConfiguration configuration : configurations) {
            verify(dao, times(1)).merge(MODULE_NAME, configuration);
        }
    }

    @Test(expected = MdsS3Exception.class)
    public void breakOnFailPositive() {
        final MdsS3ResourceConfigurationDetectorImpl instance = createDetector(null, null);
        instance.setBreakOnFail(true);
        when(provider.getConfigurations()).thenThrow(MdsS3Exception.class);

        instance.detectChanges();
    }

    @Test
    public void breakOnFailNegative() {
        final MdsS3ResourceConfigurationDetectorImpl instance = createDetector(null, null);
        instance.setBreakOnFail(false);

        instance.detectChanges();
    }

    private MdsS3ResourceConfigurationDetectorImpl createDetector(
            final ResourceConfigurationDao dao,
            final ResourceConfigurationProvider provider
    ) {
        final MdsS3ResourceConfigurationDetectorImpl result =
                new MdsS3ResourceConfigurationDetectorImpl(
                        MODULE_NAME,
                        provider != null ? provider : this.provider,
                        dao != null ? dao : this.dao
                );
        result.setBreakOnFail(true);
        return result;
    }

}
