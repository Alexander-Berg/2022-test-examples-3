package ru.yandex.mail.common.properties;

import java.net.URI;

import gumi.builders.UrlBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.qatools.properties.providers.SysPropPathReplacerProvider;


@lombok.ToString
@Resource.File("core.properties-devpack")
@Resource.Classpath("core.properties-${system.testing.scope}")
public class CoreProperties {
    private static CoreProperties instance = null;

    static {
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    private CoreProperties() {
        PropertyLoader.newInstance()
                .withPropertyProvider(new SysPropPathReplacerProvider())
                .populate(this);

        Logger.getRootLogger().setLevel(Level.toLevel(logLevel));
    }

    public static CoreProperties props() {
        if (instance == null) {
            instance = new CoreProperties();
        }
        return instance;
    }

    @Property("passport.host")
    private URI passportHost = URI.create("https://passport-test.yandex.ru/");

    @Property("testing.scope")
    private String testingScope = Scopes.DEVPACK.getName();

    @Property("is.local.debug")
    private boolean isLocalDebug = true;

    private String currentRequestId = "";

    @Property("fakebb.host")
    private String bbHost = null;

    @Property("fakebb.port")
    private int bbPort = 0;

    @Property("log.level")
    private String logLevel = "INFO";

    @Property("ssh_tests.ignore")
    private boolean ignoreSshTests = false;

    public Scopes scope() {
        return Scopes.from(testingScope);
    }

    public void setCurrentRequestId(String currentRequestId) {
        this.currentRequestId = currentRequestId;
    }

    public String getCurrentRequestId() {
        return currentRequestId;
    }

    public URI passportHost() {
        return passportHost;
    }

    public boolean isLocalDebug() {
        return isLocalDebug;
    }

    public String fakeBbUri() {
        UrlBuilder url = UrlBuilder.fromString(bbHost);
        if (80 != bbPort && url.scheme.equals("http")) {
            url = url.withPort(bbPort);
        }

        return url.toString();
    }

    public boolean ignoreSshTests() {
        return ignoreSshTests;
    }
}
