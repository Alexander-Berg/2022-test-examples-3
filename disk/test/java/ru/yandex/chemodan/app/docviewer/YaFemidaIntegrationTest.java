package ru.yandex.chemodan.app.docviewer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;

/**
 * @author akirakozov
 */
public class YaFemidaIntegrationTest extends DocviewerSpringTestBase {

    @Autowired
    private TestManager testManager;
    @Autowired
    private Copier copier;

    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(false);
    }

    // Need test oauth token for femida for testing
    @Ignore
    @Test
    public void convertFileFromFemida() {
        String url = "ya-femida://287081";
        testManager.makeAvailable(TestUser.YA_TEAM_AKIRAKOZOV.uid, url, TargetType.HTML_WITH_IMAGES);
    }

}
