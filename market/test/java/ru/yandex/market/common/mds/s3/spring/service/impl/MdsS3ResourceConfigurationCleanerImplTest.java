package ru.yandex.market.common.mds.s3.spring.service.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.PureHistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory;
import ru.yandex.market.common.mds.s3.spring.db.ResourceConfigurationDao;
import ru.yandex.market.common.mds.s3.spring.service.MdsS3ResourceConfigurationCleaner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.mds.s3.spring.SpringTestConstants.BUCKET_NAME;
import static ru.yandex.market.common.mds.s3.spring.model.ResourceConfigurationStatus.CHANGED;
import static ru.yandex.market.common.mds.s3.spring.model.ResourceConfigurationStatus.EXISTS;
import static ru.yandex.market.common.mds.s3.spring.model.ResourceConfigurationStatus.NOT_EXISTS;

/**
 * Unit-тесты для {@link MdsS3ResourceConfigurationCleanerImpl}
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@RunWith(Parameterized.class)
public class MdsS3ResourceConfigurationCleanerImplTest {

    @Mock
    private PureHistoryMdsS3Client pureHistoryMdsS3Client;

    @Mock
    private MdsS3Client mdsS3Client;

    @Mock
    private ResourceConfigurationDao dao;

    private ResourceConfiguration fullDelete;
    private ResourceConfiguration needClean;
    private ResourceConfiguration historyAndLast;
    private MdsS3ResourceConfigurationCleaner instance;

    private boolean withFolder;

    public MdsS3ResourceConfigurationCleanerImplTest(final boolean withFolder) {
        this.withFolder = withFolder;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {true}, {false}
        });
    }


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        instance = new MdsS3ResourceConfigurationCleanerImpl(
                pureHistoryMdsS3Client,
                mdsS3Client,
                dao
        );
        fullDelete = ResourceConfigurationProviderFactory.createWithHistoryOnly(BUCKET_NAME, withFolder);
        needClean = ResourceConfigurationProviderFactory.createWithHistoryOnly(BUCKET_NAME, withFolder);
        historyAndLast = ResourceConfigurationProviderFactory.createWithHistoryAndLast(BUCKET_NAME, withFolder);
    }

    @Test
    public void doClean() {
        mockJdbc();

        instance.doClean();

        verify(mdsS3Client, times(1))
                .deleteUsingPrefix(fullDelete.toLocation());

        verify(pureHistoryMdsS3Client).deleteOld(needClean);
    }

    @Test(expected = MdsS3Exception.class)
    public void doCleanExceptions() {
        mockJdbc();

        when(pureHistoryMdsS3Client.deleteOld(any())).thenThrow(MdsS3Exception.class);

        instance.doClean();
    }


    private void mockJdbc() {
        when(dao.getByStatus(EXISTS)).thenReturn(Arrays.asList(historyAndLast, needClean));
        when(dao.getByStatus(NOT_EXISTS)).thenReturn(Collections.singletonList(fullDelete));
        when(dao.getByStatus(CHANGED)).thenReturn(Collections.emptyList());
    }

}
