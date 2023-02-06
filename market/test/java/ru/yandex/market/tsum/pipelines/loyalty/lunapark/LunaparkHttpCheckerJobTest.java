package ru.yandex.market.tsum.pipelines.loyalty.lunapark;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.tsum.clients.lunapark.LunaparkApi;
import ru.yandex.market.tsum.clients.lunapark.models.HttpResponse;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobActionsContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.definition.context.impl.SupportType;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.checkout.jobs.LunaparkIdResource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.pipelines.loyalty.lunapark.LunaparkHttpCheckerJob.RESPONSE_NOT_FOUND;
import static ru.yandex.market.tsum.pipelines.loyalty.lunapark.LunaparkHttpCheckerJob.TOTAL_CASE_NAME;

/**
 * @author artemmz
 * @date 21/01/2021.
 */

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JobTesterConfig.class)
public class LunaparkHttpCheckerJobTest {
    private static final Random RND = new Random();
    private static final double TOTAL_4XX_PC = 2;
    private static final double TOTAL_5XX_PC = 1.5;
    private static final int LUNAPARK_ID = 111;
    private static final HttpResponse FINE_HTTP_STAT = new HttpResponse(200, 100_500, 100);

    private LunaparkHttpCheckerJob httpCheckerJob;

    @Autowired
    private JobTester jobTester;

    @Mock
    private LunaparkApi lunaparkApi;

    @Mock
    private HttpCodeSla sla1;

    @Mock
    private HttpCodeSla sla2;

    @Mock
    private JobContext context;

    @Mock
    private JobActionsContext actions;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(lunaparkApi.checkResultReady(LUNAPARK_ID)).thenReturn(true);

        Stream.of(sla1, sla2).forEach(sla -> {
            when(sla.getCaseName()).thenReturn("sla case " + RND.nextInt());
            when(sla.getPercent4xx()).thenReturn(0d);
            when(sla.getPercent5xx()).thenReturn(0d);
        });

        when(context.actions()).thenReturn(actions);
        httpCheckerJob = setupChecker(true);
    }

    @Test
    public void execute_fine_load() throws Exception {
        when(lunaparkApi.getHttp(anyLong(), any())).thenReturn(List.of(FINE_HTTP_STAT));

        httpCheckerJob.execute(context);
        verify(actions, never()).failJob(anyString(), any(SupportType.class));
    }

    @Test
    public void execute_missing_tag() throws Exception {
        when(lunaparkApi.getHttp(anyLong(), any())).thenReturn(Collections.emptyList());
        httpCheckerJob.execute(context);
        verify(actions).failJob(contains(RESPONSE_NOT_FOUND), any(SupportType.class));
    }

    @Test
    public void execute_ignore_missing_tag() throws Exception {
        httpCheckerJob = setupChecker(false);
        when(lunaparkApi.getHttp(anyLong(), any())).thenReturn(Collections.emptyList());
        httpCheckerJob.execute(context);
        verify(actions, never()).failJob(anyString(), any(SupportType.class));
    }

    @Test
    public void execute_bad_tag_stat() throws Exception {
        when(lunaparkApi.getHttp(LUNAPARK_ID, null)).thenReturn(List.of(FINE_HTTP_STAT));
        when(lunaparkApi.getHttp(LUNAPARK_ID, sla1.getCaseName())).thenReturn(List.of(FINE_HTTP_STAT));
        when(lunaparkApi.getHttp(LUNAPARK_ID, sla2.getCaseName())).thenReturn(List.of(new HttpResponse(500, 1000,
            1.3)));

        httpCheckerJob.execute(context);
        verify(actions).failJob(contains(sla2.getCaseName()), any(SupportType.class));
        verify(actions, never()).failJob(contains(sla1.getCaseName()), any(SupportType.class));
        verify(actions, never()).failJob(contains(TOTAL_CASE_NAME), any(SupportType.class));
    }

    @Test
    public void execute_bad_total_stat() throws Exception {
        when(lunaparkApi.getHttp(LUNAPARK_ID, null)).thenReturn(List.of(
            new HttpResponse(500, 111, TOTAL_5XX_PC / 2),
            new HttpResponse(503, 111, TOTAL_5XX_PC / 2 + 2)
        ));

        List<HttpCodeSla> caseSlas = List.of(this.sla1, sla2);
        caseSlas.forEach(sla ->
            when(lunaparkApi.getHttp(LUNAPARK_ID, sla.getCaseName())).thenReturn(List.of(FINE_HTTP_STAT)));

        httpCheckerJob.execute(context);
        verify(actions).failJob(contains(TOTAL_CASE_NAME), any(SupportType.class));
        caseSlas.forEach(sla -> verify(actions, never()).failJob(contains(sla.getCaseName()), any(SupportType.class)));
    }

    private LunaparkHttpCheckerJob setupChecker(boolean failIfSlaForNameNotFound) {
        return jobTester.jobInstanceBuilder(LunaparkHttpCheckerJob.class)
            .replaceBean(lunaparkApi)
            .withResource(new LunaparkIdResource(LUNAPARK_ID))
            .withResource(new LunaparkHttpCheckerConfig(
                List.of(sla1, sla2), TOTAL_4XX_PC, TOTAL_5XX_PC, failIfSlaForNameNotFound)
            ).create();
    }
}
