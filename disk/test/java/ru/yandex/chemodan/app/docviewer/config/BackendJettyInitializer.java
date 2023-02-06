package ru.yandex.chemodan.app.docviewer.config;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import ru.yandex.misc.web.servletContainer.SingleWarJetty;

/**
 * Created to resolve some strange Spring behavior. If @PostConstruct is presents in the @Configuration,
 * then all @Configuration beans is in 'construction' state untill @PostConstruct is invoked.
 * This may lead to some circular dependencies issues.
 * @author nshmakov
 */
@Configuration
public class BackendJettyInitializer {

    @Autowired
    @Qualifier("backendJetty")
    private SingleWarJetty jetty;

    @PostConstruct
    public void init() {
        jetty.start();
    }
}
