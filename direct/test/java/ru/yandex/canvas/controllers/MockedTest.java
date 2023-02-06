package ru.yandex.canvas.controllers;

import java.io.IOException;
import java.util.Optional;

import com.mongodb.MongoClient;
import org.junit.Before;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import ru.yandex.canvas.controllers.video.PythonRedirect;
import ru.yandex.canvas.service.AuthRequestParams;
import ru.yandex.canvas.service.AuthService;
import ru.yandex.canvas.service.AvatarsService;
import ru.yandex.canvas.service.DirectService;
import ru.yandex.canvas.service.MongoMonitorService;
import ru.yandex.canvas.service.RTBHostExportService;
import ru.yandex.canvas.service.ScreenshooterService;
import ru.yandex.canvas.service.StillageService;
import ru.yandex.canvas.service.scraper.ScraperService;
import ru.yandex.direct.tracing.TraceHelper;

import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public abstract class MockedTest {
    protected static final long USER_ID = 1;
    protected static final long CLIENT_ID = 2;

    @MockBean
    private TraceHelper traceHelper;

    @MockBean
    private PythonRedirect pythonRedirect;

    @MockBean
    private MongoTemplate mongoTemplate;

    @MockBean
    private GridFsTemplate gridFsTemplate;

    @MockBean
    private StillageService stillageService;

    @MockBean
    private AvatarsService avatarsService;

    @MockBean
    private ScreenshooterService screenshooterService;

    @MockBean
    private DirectService directService;

    @MockBean
    private RTBHostExportService rtbHostExportService;

    @MockBean
    private MongoMonitorService mongoMonitorService;

    @MockBean
    private ScraperService scraperService;

    @MockBean
    private AuthRequestParams authRequestParams;

    @MockBean
    private AuthService authService;

    @MockBean
    private RootController rootController;

    @MockBean
    private MongoClient mongoClient;

    @MockBean
    private MongoDbFactory mongoDbFactory;

    @MockBean
    private FilesController filesController;

    public AvatarsService getAvatarsService() {
        return avatarsService;
    }

    @Before
    public void initMockedTest() throws IOException {
        when(authRequestParams.getClientId()).thenReturn(Optional.of(CLIENT_ID));
        when(authRequestParams.getUserId()).thenReturn(Optional.of(USER_ID));
    }

}
