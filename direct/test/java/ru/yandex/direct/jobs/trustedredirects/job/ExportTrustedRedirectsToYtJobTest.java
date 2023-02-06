package ru.yandex.direct.jobs.trustedredirects.job;

import java.time.LocalDate;
import java.util.Collections;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.jobs.trustedredirects.job.ExportTrustedRedirectsToYtJob.EXPORT_CLUSTER;
import static ru.yandex.direct.jobs.trustedredirects.job.ExportTrustedRedirectsToYtJob.LAST_EXECUTION;

class ExportTrustedRedirectsToYtJobTest {
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private YtProvider ytProvider;
    @Mock
    private TrustedRedirectsService trustedRedirectsService;

    private ExportTrustedRedirectsToYtJob job;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        job = new ExportTrustedRedirectsToYtJob(ppcPropertiesSupport, ytProvider, trustedRedirectsService);
    }

    @Test
    void testShouldRunNull() {
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(null);

        doReturn(property)
                .when(ppcPropertiesSupport).get(eq(LAST_EXECUTION));
        assertThat(job.shouldRun())
                .isTrue();
    }

    @Test
    void testShouldRunOld() {
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(LocalDate.now().minusDays(4));

        doReturn(property)
                .when(ppcPropertiesSupport).get(eq(LAST_EXECUTION));
        assertThat(job.shouldRun())
                .isTrue();
    }

    @Test
    void testShouldRunToday() {
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(LocalDate.now());

        doReturn(property)
                .when(ppcPropertiesSupport).get(eq(LAST_EXECUTION));
        assertThat(job.shouldRun())
                .isFalse();
    }

    @Test
    void testExecuteErrorPropertyNotSet() {
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(null);

        doReturn(property)
                .when(ppcPropertiesSupport).get(eq(LAST_EXECUTION));
        doThrow(new RuntimeException())
                .when(ytProvider).getClusterConfig(eq(EXPORT_CLUSTER));

        assertThatThrownBy(() -> job.execute())
                .isInstanceOf(RuntimeException.class);

        verify(ppcPropertiesSupport, never()).set(anyString(), any());
    }

    @Test
    void testExecute() {
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(null);

        YtClusterConfig ytClusterConfig = mock(YtClusterConfig.class);
        doReturn("//home/direct")
                .when(ytClusterConfig).getHome();
        doReturn(ytClusterConfig)
                .when(ytProvider).getClusterConfig(any());

        doReturn(property)
                .when(ppcPropertiesSupport).get(eq(LAST_EXECUTION));
        YtDynamicOperator operator = mock(YtDynamicOperator.class);
        doReturn(operator)
                .when(ytProvider).getDynamicOperator(eq(EXPORT_CLUSTER));

        YTreeNode replica = YTree.builder().beginMap()
                .key("cluster_name").value("seneca-sas")
                .key("replica_path").value("//tmp/replica")
                .buildMap();
        doReturn(replica)
                .when(operator).runRpcCommand(any(Function.class));

        YtDynamicOperator readOperator = mock(YtDynamicOperator.class);
        doReturn(readOperator)
                .when(ytProvider).getDynamicOperator(eq(YtCluster.SENECA_SAS));

        doReturn(Collections.emptyList())
                .when(readOperator).runRpcCommand(any(Function.class));

        job.execute();

        verify(operator).runInTransaction(any());
        verify(property).set(eq(LocalDate.now()));
    }
}
