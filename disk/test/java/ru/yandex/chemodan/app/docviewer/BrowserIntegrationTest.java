package ru.yandex.chemodan.app.docviewer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.chemodan.app.docviewer.config.DocviewerActiveProfiles;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.inside.passport.PassportUidOrZero;

/**
 * @author akirakozov
 */
@ActiveProfiles(DocviewerActiveProfiles.UNISTORAGE)
public class BrowserIntegrationTest extends DocviewerSpringTestBase {

    // TODO: upload file to MDS using Mds client instead of hardcoded value
    public static final String YA_BROWSER_TEST_XLS_URL = "ya-browser://5070/testxls.xls";

    @Autowired
    private TestManager testManager;
    @Autowired
    private Copier copier;

    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(false);
    }

    @Test
    @Ignore
    //depends on external resources
    public void convertFileFromBrowser() {
        testManager.makeAvailable(PassportUidOrZero.zero(), YA_BROWSER_TEST_XLS_URL, TargetType.HTML_WITH_IMAGES);
    }

}
