package ru.yandex.chemodan.app.docviewer.config;

import javax.servlet.Servlet;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.docviewer.config.web.BackendDaemonContextConfiguration;

/**
 * @author nshmakov
 */
@Configuration
@Import({
        BackendDaemonContextConfiguration.class,
        DocviewerInitContextConfiguration.class,
        BackendJettyInitializer.class
})
public class BackendActionTestContextConfiguration {

    @Bean
    public Servlet pingServletBackend() {
        return Mockito.mock(Servlet.class);
    }

    @Bean
    public Servlet pingSlbServletBackend() {
        return Mockito.mock(Servlet.class);
    }
}
