package ru.yandex.chemodan.app.docviewer;

import org.apache.http.client.methods.HttpGet;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.chemodan.app.docviewer.config.ConvertersContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.CoreContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.TestContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.WebSecurityContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.web.BackendContextConfiguration;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.copy.UriHelper;
import ru.yandex.chemodan.app.docviewer.disk.mpfs.MpfsUrlHelper;
import ru.yandex.chemodan.app.docviewer.test.AppVersionContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.log.Log4jHelper;
import ru.yandex.chemodan.zk.configuration.ZkEmbeddedConfiguration;
import ru.yandex.chemodan.zk.registries.staff.YandexStaffUserRegistryContextConfiguration;
import ru.yandex.chemodan.zk.registries.tvm.ZkTvm2ContextConfiguration;
import ru.yandex.commune.alive2.AliveAppsContextConfiguration;
import ru.yandex.commune.alive2.location.TestLocationResolverConfiguration;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.io.http.apache.v4.ApacheHttpClient4Utils;
import ru.yandex.misc.log.mlf.Level;
import ru.yandex.misc.test.TestBase;

/**
 * @author akirakozov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes={
                ConvertersContextConfiguration.class,
                CoreContextConfiguration.class,
                TestContextConfiguration.class,
                TestLocationResolverConfiguration.class,
                ZkEmbeddedConfiguration.class,
                BackendContextConfiguration.class,
                WebSecurityContextConfiguration.class,
                YandexStaffUserRegistryContextConfiguration.class,
                ChemodanInitContextConfiguration.class,
                AliveAppsContextConfiguration.class,
                AppVersionContextConfiguration.class,
                ZkTvm2ContextConfiguration.class,
                },
        loader=DocviewerAnnotationTestContextLoader.class)
public class DocviewerSpringTestBase extends TestBase {

    @Autowired
    private Copier copier;
    @Autowired
    private UriHelper uriHelper;
    @Autowired
    private MpfsUrlHelper mpfsUrlHelper;
    @Autowired
    protected Tvm2 tvm2;

    @BeforeClass
    public static void beforeClass() {
        Log4jHelper.configureTestLogger(Level.DEBUG);
    }

    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(true);
        uriHelper.setDisableOriginalUrlCheck(true);
        tvm2.refresh();
    }

    protected void removeResource(PassportUidOrZero uid, String path) {
        String removeResourceUrl = mpfsUrlHelper.getRemoveResourceUrl(uid, path);
        ApacheHttpClient4Utils.executeReadString(new HttpGet(removeResourceUrl), Timeout.seconds(30));
    }
}
