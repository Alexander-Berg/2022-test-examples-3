package ru.yandex.chemodan.uploader.social;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.uploader.preview.PreviewImageManager;
import ru.yandex.misc.io.InputStreamSource;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.TestBase;
import ru.yandex.misc.web.servletContainer.SingleWarJetty;

/**
 * @author akirakozov
 */
public class SocialProxyStagesTest extends TestBase {
    private static final Logger logger = LoggerFactory.getLogger(SocialProxyStagesTest.class);

    @Ignore
    @Test
    public void uploadFileToSocialNet() {
        InputStreamSource file = ClassLoaderUtils.streamSourceForResource(PreviewImageManager.class, "vini.jpg");
        SocialProxyStages stages = new SocialProxyStages(null, Timeout.seconds(10));
        try {
            stages.uploadPhotoToSocialNet(
                    "https://graph.facebook.com/406015222877912/photos?access_token=CAADEZBFep6zkBAD0AZCC3tSyZBbyVixITIudvybhkZC971XEmRizZAevHZCxPUCA3JuchcSNhN0ZA5FUIYbeZA2X3MyTgEx0bIsUIZBADnqqwtJ4ZCT1JnSyUKfw0J56ZByCeuRndZCawZAUZBdqoBPtgWlEsULVbZCviqBBh3Rox1L1ZA3F9l6utDmIMfR8Le9fUP7g1vcZD",
                    Cf.<String, String>map(), "source", file);
        } catch (Exception e) {
            logger.warn(e, e);
            Assert.fail();
        }
    }

    @Test(expected=SocialClientErrException.class)
    public void uploadFileToSocialNetWith4xx() {
        InputStreamSource file = ClassLoaderUtils.streamSourceForResource(PreviewImageManager.class, "vini.jpg");
        SocialProxyStages stages = new SocialProxyStages(null, Timeout.seconds(10));

        SingleWarJetty jetty = new SingleWarJetty();
        jetty.setLookupServletsInContext(false);
        jetty.setHttpPort(0);
        try {
            jetty.start();
            // expect 404 error
            stages.uploadPhotoToSocialNet("http://localhost:" + jetty.getActualHttpPort() + "/404.html",
                    Cf.<String, String>map(), "source", file);
        } finally {
            jetty.stop();
        }
    }

}
