package ru.yandex.chemodan.app.docviewer.web;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.chemodan.app.docviewer.config.web.WebContextConfiguration;
import ru.yandex.chemodan.app.docviewer.convert.TargetType;
import ru.yandex.misc.property.PropertiesHolder;
import ru.yandex.misc.property.load.strategy.SecretKeys;
import ru.yandex.misc.web.servletContainer.SingleWarJetty;

/**
 *
 * @author akirakozov
 * @author nshmakov
 */
@ContextConfiguration(classes = {
        WebContextConfiguration.class,
        DocviewerWebSpringTestBase.SecretKeysConfiguration.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DocviewerWebSpringTestBase extends DocviewerSpringTestBase {

    @Autowired
    @Qualifier("backendJetty")
    private SingleWarJetty backendJetty;

    @Autowired
    @Qualifier("serviceJetty")
    private SingleWarJetty serviceJetty;

    @Before
    public void beforeWeb() {
        tvm2.refresh();
        backendJetty.start();
        serviceJetty.start();
    }

    protected TargetType getConvertTargetMobileIncluded(boolean mobile) {
        return mobile ? TargetType.HTML_WITH_IMAGES_FOR_MOBILE : TargetType.HTML_WITH_IMAGES;
    }

    protected String getMobileParameter(boolean mobile) {
        return mobile ? "&mobile=true" : "";
    }

    @Configuration
    public static class SecretKeysConfiguration {

        @Bean
        public SecretKeys secretKeys() {
            return PropertiesHolder.secretKeys();
        }
    }
}
