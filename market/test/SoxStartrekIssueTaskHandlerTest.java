package ru.yandex.market.jmf.module.def.test;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.jmf.configuration.ConfigurationService;
import ru.yandex.market.jmf.module.def.impl.sox.SoxStartrekIssueTaskContext;
import ru.yandex.market.jmf.module.def.impl.sox.SoxStartrekIssueTaskHandler;
import ru.yandex.market.jmf.startrek.support.StartrekService;
import ru.yandex.startrek.client.model.Issue;

@SpringJUnitConfig(SoxStartrekIssueConfiguration.class)
public class SoxStartrekIssueTaskHandlerTest {
    private static final String STARTREK_QUEUE = "TESTQUEUE";

    @Inject
    private ConfigurationService configurationService;
    @Inject
    private SoxStartrekIssueTaskHandler soxStartrekIssueTaskHandler;

    @Inject
    private StartrekService startrekServiceMock;

    @BeforeEach
    void setUp() {
        configurationService.setValue("soxStartrekQueue", STARTREK_QUEUE);

        Issue issueMock = new Issue(null, null, "TEST-1", null, 1, new EmptyMap<>(), null);
        Mockito.when(startrekServiceMock.createIssue(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(issueMock);
    }

    @Test
    @Transactional
    void testCreateIssueTaskHandler() {
        String testAuthor = "author";
        String testSummary = "Test summary";
        String testDescription = "Test description";
        SoxStartrekIssueTaskContext context = new SoxStartrekIssueTaskContext(testAuthor, testSummary, testDescription);
        soxStartrekIssueTaskHandler.invoke(context);

        Mockito.verify(startrekServiceMock, Mockito.times(1))
                .createIssue(
                        Mockito.eq(STARTREK_QUEUE),
                        Mockito.eq(testAuthor),
                        Mockito.eq(testSummary),
                        Mockito.eq(testDescription));
    }
}
