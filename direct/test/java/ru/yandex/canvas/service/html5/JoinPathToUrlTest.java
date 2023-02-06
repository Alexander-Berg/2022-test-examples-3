package ru.yandex.canvas.service.html5;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class JoinPathToUrlTest {

    @Mock
    Html5SourcesService html5SourcesService;

    @Test
    public void checkJoin() {
        MockitoAnnotations.initMocks(this);
        when(html5SourcesService.addDirToUrl(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        String result = html5SourcesService.addDirToUrl("https://mds.ya.ru/123/kwlrjf/", "");
        assertEquals("Check1", "https://mds.ya.ru/123/kwlrjf/", result);

        result = html5SourcesService.addDirToUrl("https://mds.ya.ru/123/kwlrjf/", "/");
        assertEquals("Check2", "https://mds.ya.ru/123/kwlrjf/", result);

        result = html5SourcesService.addDirToUrl("https://mds.ya.ru/123/kwlrjf/", "/index.html");
        assertEquals("Check3", "https://mds.ya.ru/123/kwlrjf/", result);

        result = html5SourcesService.addDirToUrl("https://mds.ya.ru/123/kwlrjf/", "/html/index.html");
        assertEquals("Check4", "https://mds.ya.ru/123/kwlrjf/html/", result);
    }

}
