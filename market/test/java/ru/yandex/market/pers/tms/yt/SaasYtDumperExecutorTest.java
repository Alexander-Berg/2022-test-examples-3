package ru.yandex.market.pers.tms.yt;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.service.GradeQueueService;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.saas.IndexGenerationIdFactory;
import ru.yandex.market.pers.tms.yt.dumper.dumper.IndexerMetricLogProxy;
import ru.yandex.market.pers.tms.yt.saas.SaasIndexDumperService;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static ru.yandex.market.pers.tms.yt.saas.SaasIndexDumperService.SAAS_YT_DIFF_ENABLED;
import static ru.yandex.market.pers.tms.yt.saas.SaasIndexDumperService.SAAS_YT_PULL_LAST_SNAPSHOT;

public class SaasYtDumperExecutorTest extends MockedPersTmsTest {

    @Autowired
    private IndexerMetricLogProxy indexerMetricLogProxy;
    @Autowired
    private SaasIndexDumperService saasIndexDumperService;
    @Autowired
    private GradeQueueService gradeQueueService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private IndexGenerationIdFactory indexGenerationIdFactory;
    @Autowired
    private GradeCreator gradeCreator;

    private void prepareIndexerLogProxy() {
        Set<Date> dateSet = new HashSet<>();
        Set<String> metricsSet = new HashSet<>();
        Mockito.doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            boolean uniqueDate = dateSet.add((Date) arguments[0]);
            if (uniqueDate && dateSet.size() != 1) {
                Assert.fail("Date must be the same during index");
            }
            boolean uniqueMetric = metricsSet.add((String) arguments[1]);
            if (!uniqueMetric) {
                Assert.fail("Metric name must be unique. " + arguments[1] + " duplicates");
            }
            return null;
        }).when(indexerMetricLogProxy).logMetric(any(), any(), anyLong());
    }

    @Test
    public void testMetricsOnDiff() throws Exception {
        prepareIndexerLogProxy();

        gradeQueueService.put(createGrade());
        configurationService.mergeValue(SAAS_YT_DIFF_ENABLED, String.valueOf(true));
        configurationService.mergeValue(SAAS_YT_PULL_LAST_SNAPSHOT, System.currentTimeMillis());
        Mockito.when(indexGenerationIdFactory.buildNewGenerationId(anyLong())).thenCallRealMethod();

        saasIndexDumperService.dumpGrades();
    }

    @Test
    public void testMetricsOnSnapshot() throws Exception {
        prepareIndexerLogProxy();

        configurationService.mergeValue(SAAS_YT_DIFF_ENABLED, String.valueOf(false));
        Mockito.when(indexGenerationIdFactory.buildNewGenerationId(anyLong())).thenCallRealMethod();

        saasIndexDumperService.dumpGrades();
    }

    private long createGrade() {
        return gradeCreator.createGrade(GradeCreator.constructShopGradeRnd());
    }
}
