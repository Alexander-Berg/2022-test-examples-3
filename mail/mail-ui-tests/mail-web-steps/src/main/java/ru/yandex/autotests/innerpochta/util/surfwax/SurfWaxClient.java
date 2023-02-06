package ru.yandex.autotests.innerpochta.util.surfwax;

import org.apache.log4j.Logger;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;

public class SurfWaxClient {

    public static final String AERO_LAUNCH_ID = "aero.launch.uuid";

    public static final String AERO_SUITE_NAME = "aero.suite.name";

    private static final String USE_LOCALHOST_FILE = "use_localhost";

    private final Logger logger = Logger.getLogger(getClass());

    private String userName = "aqua";

    private String password = "407a4ec42ce16bf67926696ecb91b847";

    public SurfWaxClient withUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public SurfWaxClient withPassword(String password) {
        this.password = password;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public RemoteWebDriver find(DesiredCapabilities caps) throws SurfWaxClientException {
        overrideCapabilities(caps);
        Host host = getHost();

        try {
            logger.info(String.format(
                "Trying to obtain %s on %s:%d",
                caps.getBrowserName(), host.getName(), host.getPort()
            ));
            return instantiate(host, caps);
        } catch (SurfWaxClientException e) {
            throw e;
        } catch (Exception e) {
            throw new SurfWaxClientException(
                String.format("Failed to obtain browser on host: %s", webdriverURL(host)), e
            );
        }
    }

    private Host getHost() {
        return useLocalhost() ?
            new Host("localhost", 4444) :
            new Host("sw.yandex-team.ru", 80);
    }

    private boolean useLocalhost() {
        return getClass().getClassLoader().getResource(USE_LOCALHOST_FILE) != null;
    }

    protected void overrideCapabilities(DesiredCapabilities caps) {
        caps.setCapability(AERO_LAUNCH_ID, System.getProperty(AERO_LAUNCH_ID));
        caps.setCapability(AERO_SUITE_NAME, System.getProperty(AERO_SUITE_NAME));
        if (caps.getCapability(CapabilityType.PROXY) == null
            && caps.getPlatform() == Platform.WINDOWS) {
            caps.setCapability(CapabilityType.PROXY, new Proxy() {{
                setProxyType(ProxyType.DIRECT);
            }});
        }
    }

    protected RemoteWebDriver instantiate(Host host, DesiredCapabilities caps) throws Exception {
        return new RemoteWebDriver(webdriverURL(host), caps);
    }

    protected URL webdriverURL(Host host) {
        String path = useLocalhost() ? "wd/hub" : "v0";

        try {
            return new URL(String.format(
                "http://%s@%s:%s/%s",
                getUserName(),
                host.getName(),
                host.getPort(),
                path
            ));
        } catch (MalformedURLException e) {
            throw new SurfWaxClientException(
                String.format("Invalid hostname: %s, or port number: %d!", host.getName(), host.getPort()),
                e
            );
        }
    }
}
