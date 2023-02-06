package ru.yandex.direct.dialogs.client;

import java.io.IOException;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.dialogs.client.model.Skill;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
public abstract class DialogsClientTestBase {
    private final static Logger logger = LoggerFactory.getLogger(DialogsClientTestBase.class);
    static final String TICKET_BODY = "ticketBody";

    private MockWebServer mockWebServer;

    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;

    DialogsClient dialogsClient;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.setDispatcher(dispatcher());
        mockWebServer.start();

        TvmIntegration tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.isEnabled()).thenReturn(true);
        when(tvmIntegration.getTicket(TvmService.DUMMY)).thenReturn(TICKET_BODY);

        dialogsClient = new DialogsClient(url(), TvmService.DUMMY, parallelFetcherFactory, tvmIntegration);
    }

    protected abstract Dispatcher dispatcher();

    protected String url() {
        return "http://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort();
    }

    Skill createTestSkill(String skillId) {
        Skill skill = new Skill();
        skill.setSkillId(skillId);
        skill.setBotGuid("667f91f2-e610-4b89-93e0-0cf3b041e367");
        skill.setName("test-name");
        skill.setOnAir(true);

        return skill;
    }

    @After
    public void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (Exception e) {
            logger.warn("cannot shut down mockWebServer", e);
        }
    }
}
