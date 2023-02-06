package ru.yandex.direct.core.entity.adgeneration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.bangenproxy.client.BanGenProxyClient;
import ru.yandex.direct.bangenproxy.client.zenmeta.ZenMetaInfoClient;
import ru.yandex.direct.bangenproxy.client.zenmeta.model.ZenMetaInfo;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ZenMetaInfoServiceTest {
    private static final String TEST_URL = "https://zen.yandex.ru/";
    private static final String TITLE = "Zen Title";
    private static final String BODY = "Zen Body Text";

    @Mock
    private ZenMetaInfoClient zenMetaInfoClient;

    @Autowired
    private BanGenProxyClient banGenProxyClient;

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    private MetrikaGoalsService metrikaGoalsService;

    private ZenMetaInfoService zenMetaInfoService;

    @Before
    public void before() {
        openMocks(this);
        zenMetaInfoService = new ZenMetaInfoService(ppcPropertiesSupport,
                banGenProxyClient, zenMetaInfoClient, metrikaGoalsService);
    }

    @Test
    public void getZenMetaInfoWithGeneratedTextByUrl_emptyContent() {
        setUpZenMetaInfoReponseAndCheck(null, "Zen Body Text");
    }

    @Test
    public void getZenMetaInfoWithGeneratedTextByUrl_htmlContent() {
        setUpZenMetaInfoReponseAndCheck("<h1>Zen Body</h1>\n" +
                "<p> Text</p>\n", "");
    }

    @Test
    public void getZenMetaInfoWithGeneratedTextByUrl_textContent() {
        setUpZenMetaInfoReponseAndCheck(BODY, "");
    }

    @After
    public void after() {
        reset(banGenProxyClient);
    }

    private void setUpZenMetaInfoReponseAndCheck(String content, String snippet) {
        doReturn(new ZenMetaInfo()
                .withTitle(TITLE)
                .withSnippet(snippet)
                .withContent(content))
                .when(zenMetaInfoClient)
                .getZenMetaInfoByUrl(eq(TEST_URL));
        zenMetaInfoService.getZenMetaInfoWithGeneratedTextByUrl(TEST_URL);
        verify(banGenProxyClient)
                .getUrlInfoForCustom(TITLE, BODY);
    }
}
