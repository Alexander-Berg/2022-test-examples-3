package ru.yandex.chemodan.app.docviewer;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

import ru.yandex.chemodan.app.docviewer.web.DocviewerMain;
import ru.yandex.chemodan.boot.ChemodanPropertiesLoadStrategy;
import ru.yandex.chemodan.log.Log4jHelper;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.env.EnvironmentTypeReader;
import ru.yandex.misc.log.mlf.Level;
import ru.yandex.misc.property.load.PropertiesLoader;
import ru.yandex.misc.test.TestBase;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author akirakozov
 */
public class ContextsTest extends TestBase {

    @BeforeClass
    public static void setUp() {
        Log4jHelper.configureTestLogger(Level.DEBUG);
        System.setProperty(EnvironmentTypeReader.YANDEX_ENVIRONMENT_TYPE_PROPERTY,
                EnvironmentType.DEVELOPMENT.toString().toLowerCase());
    }

    @Test
    public void context() {
        PropertiesLoader.initialize(new ChemodanPropertiesLoadStrategy(new SimpleAppName("docviewer", "web"), true));
        DocviewerMain main = new DocviewerMain();
        ConfigurableApplicationContext context = main.loadApplicationContext();
        context.close();
    }

}
