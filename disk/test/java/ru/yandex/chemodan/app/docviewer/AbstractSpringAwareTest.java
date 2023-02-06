package ru.yandex.chemodan.app.docviewer;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import ru.yandex.chemodan.app.docviewer.config.ConvertersContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.CoreContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.TestContextConfiguration;
import ru.yandex.chemodan.app.docviewer.config.web.WebContextConfiguration;
import ru.yandex.chemodan.app.docviewer.copy.Copier;
import ru.yandex.chemodan.app.docviewer.copy.UriHelper;
import ru.yandex.chemodan.log.Log4jHelper;
import ru.yandex.misc.log.mlf.Level;
import ru.yandex.misc.spring.ServiceUtils;
import ru.yandex.misc.thread.ThreadUtils;

// XXX strongly duplicates DocviewerSpringTestBase
public abstract class AbstractSpringAwareTest {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSpringAwareTest.class);

    private static AbstractApplicationContext applicationContext;

    private static Class[] applicationContextConfigurations() {
        return new Class[] {
                ConvertersContextConfiguration.class,
                CoreContextConfiguration.class,
                WebContextConfiguration.class,
                TestContextConfiguration.class,
                };
    }

    @Autowired
    private Copier copier;
    @Autowired
    private UriHelper uriHelper;

    @Before
    public void before() {
        copier.setEnableNativeUrlFetching(true);
        uriHelper.setDisableOriginalUrlCheck(true);
    }

    @AfterClass
    public synchronized static void shutdownContext() {
        if (applicationContext == null)
            return;

        try {
            ServiceUtils.stopAllServices(applicationContext);
        } catch (Throwable exc) {
            logger.error("Unable to stop all services: " + exc, exc);
        }

        if (ServiceUtils.invokeDelayShutdownListeners(applicationContext))
            ThreadUtils.sleep(5, TimeUnit.SECONDS);

        applicationContext.close();
        applicationContext = null;
    }

    @BeforeClass
    public static synchronized void startupContext() {
        Log4jHelper.configureTestLogger(Level.DEBUG);
        if (applicationContext != null)
            return;

        logger.info("Starting an application...");

        Configuration.loadTestsProperties();
        applicationContext = new AnnotationConfigApplicationContext(applicationContextConfigurations());

        ServiceUtils.startAllServices(applicationContext);
    }

    public AbstractSpringAwareTest() {
        registerBean(this, getClass().getName());
    }

    private void registerBean(Object bean, String name) {
        AutowireCapableBeanFactory beanFactory = applicationContext.getAutowireCapableBeanFactory();
        beanFactory.autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
        beanFactory.initializeBean(bean, name);
    }

}
