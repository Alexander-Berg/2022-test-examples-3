package ru.yandex.direct.traceinterception.entity.traceinterception.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.traceinterception.configuration.TraceInterceptionTest;
import ru.yandex.direct.traceinterception.model.TraceInterception;
import ru.yandex.direct.traceinterception.model.TraceInterceptionAction;
import ru.yandex.direct.traceinterception.model.TraceInterceptionCondition;
import ru.yandex.direct.traceinterception.model.TraceInterceptionStatus;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.dbschema.ppcdict.tables.TraceInterceptions.TRACE_INTERCEPTIONS;

@TraceInterceptionTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TraceInterceptionsRepositoryTest {
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private TraceInterceptionsRepository traceInterceptionsRepository;

    @Before
    public void before() {
        dslContextProvider.ppcdict().deleteFrom(TRACE_INTERCEPTIONS).execute();
    }

    @Test
    public void insertAndGet() {
        TraceInterception traceInterception = new TraceInterception()
                .withId(1L)
                .withCondition(new TraceInterceptionCondition()
                        .withService("service")
                        .withMethod("method")
                        .withFunc("func")
                        .withTags("tags"))
                .withAction(new TraceInterceptionAction()
                        .withSleepDuration(10L)
                        .withExceptionMessage("exception")
                        .withSemaphorePermits(20))
                .withStatus(TraceInterceptionStatus.ON);

        traceInterceptionsRepository.add(traceInterception);

        List<TraceInterception> data = traceInterceptionsRepository.getAll();
        TraceInterception selected = data.get(0);

        assertThat("что положили то и достали", selected, beanDiffer(traceInterception));
    }
}
