package ru.yandex.direct.logviewercore.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.logviewercore.domain.LogRecordInfo;
import ru.yandex.direct.logviewercore.domain.ppclog.LogPriceRecord;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class LogViewerServiceCutColumnsTest {

    private static final String SERVICE_KEY = "service";
    private static final String SERVICE_VALUE1 = "some-service1";
    private static final String SERVICE_VALUE2 = "some-service2";
    private static final String METHOD_KEY = "method";
    private static final String METHOD_VALUE1 = "some-method1";
    private static final String METHOD_VALUE2 = "some-method2";
    private static final String UID_KEY = "uid";
    private static final long UID_VALUE1 = 12345L;
    private static final long UID_VALUE2 = 1234567L;

    private LogViewerService testingService;

    private LogRecordInfo<LogPriceRecord> info;
    private List<String> fields;

    private LogPriceRecord record1;
    private LogPriceRecord record2;

    @Before
    public void prepare() {
        testingService = new LogViewerService(
                mock(DatabaseWrapperProvider.class),
                mock(ShardHelper.class),
                Collections.emptyList(),
                mock(FeatureService.class),
                mock(FeatureManagingService.class)
        );

        info = new LogRecordInfo<>(LogPriceRecord.class);
        fields = Arrays.asList(SERVICE_KEY, METHOD_KEY, UID_KEY);

        record1 = new LogPriceRecord();
        record1.service = SERVICE_VALUE1;
        record1.method = METHOD_VALUE1;
        record1.uid = UID_VALUE1;

        record2 = new LogPriceRecord();
        record2.service = SERVICE_VALUE2;
        record2.method = METHOD_VALUE2;
        record2.uid = UID_VALUE2;
    }

    @Test
    public void cutColumns_OneResult() {
        List<LogPriceRecord> rows = Collections.singletonList(record1);
        List<List<Object>> expectedResult = Collections.singletonList(
                Arrays.asList(SERVICE_VALUE1, METHOD_VALUE1, UID_VALUE1));

        List<List<Object>> actualResult = testingService.cutColumns(info, rows, fields);

        assertThat(actualResult, beanDiffer(expectedResult));
    }

    @Test
    public void cutColumns_ManyResults() {
        List<LogPriceRecord> rows = Arrays.asList(record1, record2);
        List<List<Object>> expectedResult = Arrays.asList(
                Arrays.asList(SERVICE_VALUE1, METHOD_VALUE1, UID_VALUE1),
                Arrays.asList(SERVICE_VALUE2, METHOD_VALUE2, UID_VALUE2));

        List<List<Object>> actualResult = testingService.cutColumns(info, rows, fields);

        assertThat(actualResult, beanDiffer(expectedResult));
    }
}
