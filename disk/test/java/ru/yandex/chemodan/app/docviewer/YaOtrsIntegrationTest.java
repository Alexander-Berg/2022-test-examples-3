package ru.yandex.chemodan.app.docviewer;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.chemodan.app.docviewer.config.DocviewerActiveProfiles;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.web.DocviewerWebSpringTestBase;

/**
 *
 * @author akirakozov
 *
 */
@ActiveProfiles(DocviewerActiveProfiles.UNISTORAGE)
public class YaOtrsIntegrationTest extends DocviewerWebSpringTestBase {

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
    public void convertOtrs() {
        String url = "otrs://MDS:5317/3b251b8e-349f-4ac6-9942-e1402307d828/";
        testManager.makeAvailable(TestUser.YA_TEAM_AKIRAKOZOV.uid, url, TargetType.HTML_WITH_IMAGES);
    }

}
