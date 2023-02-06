package ru.yandex.market.crm.operatorwindow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.operatorwindow.configuration.ModuleOwTestConfiguration;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.utils.UrlCreationService;

import static org.mockito.Mockito.mock;

@SpringJUnitConfig(classes = ModuleOwTestConfiguration.class)
@ActiveProfiles("test")
public abstract class AbstractModuleOwTest {

    @Inject
    protected MockSecurityDataService securityDataService;
    @Inject
    protected MockAuthRunnerService authRunnerService;
    @Inject
    protected UrlCreationService urlCreationServiceMock;

    private AutoCloseable mockitoMocks;

    @BeforeEach
    public void setUp0() throws IOException {
        mockitoMocks = MockitoAnnotations.openMocks(this);

        authRunnerService.reset();
        securityDataService.reset();

        mockUrlCreationService();
    }

    private void mockUrlCreationService() throws IOException {
        var inputStream = new ByteArrayInputStream("YEAH!".getBytes(StandardCharsets.UTF_8));

        var urlConnection = mock(URLConnection.class);
        Mockito.when(urlConnection.getContentType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        Mockito.when(urlConnection.getInputStream()).thenReturn(inputStream);

        var url = mock(URL.class);
        Mockito.when(url.getPath()).thenReturn("/");
        Mockito.when(url.openConnection()).thenReturn(urlConnection);
        Mockito.when(url.openStream()).thenReturn(inputStream);

        Mockito.when(urlCreationServiceMock.create(Mockito.anyString())).thenReturn(url);
    }

    @AfterEach
    public void releaseMocks() throws Exception {
        mockitoMocks.close();
        Mockito.reset(urlCreationServiceMock);
    }
}
