package ru.yandex.autotests.innerpochta.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.junit.rules.ExternalResource;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import ru.yandex.autotests.webcommon.util.prop.WebDriverProperties;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

import static ru.yandex.autotests.innerpochta.util.ProxyServerConstants.SESSION_TIMEOUT;
import static ru.yandex.autotests.innerpochta.util.ProxyServerConstants.SESSION_TIMEOUT_VALUE;
import static ru.yandex.autotests.innerpochta.util.Utils.randomPort;


/**
 * Created by mabelpines on 19.05.15.
 */
public class ProxyServerRule extends ExternalResource {
    private static DesiredCapabilities capabilities;
    private static int port = randomPort();
    private static String requestPostBody;
    private HttpProxyServer server;
    private HttpFiltersSourceAdapter filter;

    private ProxyServerRule(HttpFiltersSourceAdapter filter) { this.filter = filter; }

    public static ProxyServerRule proxyServerRule(HttpFiltersSourceAdapter filter) {
        return new ProxyServerRule(filter);
    }

    public static void setRequestPostBody(String requestPostBody) {
        ProxyServerRule.requestPostBody = requestPostBody;
    }

    public static JsonObject parseParams(String modelName) throws JsonParseException {
        JsonObject params = new JsonObject();
        if (requestPostBody == null) {
            throw new RuntimeException("Proxy didn't catch request, retrying test");
        }
        JsonArray models = new JsonParser().parse(requestPostBody).getAsJsonObject().getAsJsonArray("models");
        for (int i = 0; i < models.size(); i++) {
            String tempModelName = models.get(i).getAsJsonObject().get("name").getAsString();
            if (modelName.equals(tempModelName)) {
                params = models.get(i).getAsJsonObject().get("params").getAsJsonObject();
                return params;
            }
        }
        throw new JsonParseException("The required model «" + modelName + "» is missing in the request");
    }

    @Override
    protected void before() throws Throwable {
        server = DefaultHttpProxyServer.bootstrap()
            .withPort(port)
            .withAddress(new InetSocketAddress(getIpv6(), port))
            .withManInTheMiddle(new SelfSignedMitmManager())
            .withAllowLocalOnly(false)
            .withListenOnAllAddresses(true)
            .withFiltersSource(filter)
            .start();
    }

    @Override
    protected void after() {
        server.stop();
    }

    public DesiredCapabilities getCapabilities() {
        setCapabilities();
        capabilities.setCapability(CapabilityType.PROXY, setProxy());
        return capabilities;
    }

    protected void setCapabilities() {
        capabilities = new DesiredCapabilities(
            WebDriverProperties.props().driverType(),
            WebDriverProperties.props().version(),
            WebDriverProperties.props().platform()
        );
        capabilities.setCapability(SESSION_TIMEOUT, SESSION_TIMEOUT_VALUE);
    }

    private Proxy setProxy() {
        Proxy proxy = new Proxy();
        proxy.setProxyType(Proxy.ProxyType.MANUAL);
        String proxyStr = server.getListenAddress().toString().substring(1);
        proxy.setHttpProxy(proxyStr);
        proxy.setSslProxy(proxyStr);
        return proxy;
    }

    private String getIpv6() throws UnknownHostException, SocketException {
        final String LOCAL_ADDRESS = "0:0:0:0:0:0:0:1";
        final String EXTERNAL_NETWORK_INTERFACE = "eth";
        final String EXTERNAL_NETWORK_INTERFACE_MAC = "en0";
        final String IPV6_CORRECT_START = "2a02";
        StringBuilder addrString = new StringBuilder();
        Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
        for (; n.hasMoreElements(); ) {
            NetworkInterface e = n.nextElement();

            Enumeration<InetAddress> a = e.getInetAddresses();
            for (; a.hasMoreElements(); ) {
                String addr = a.nextElement().getHostAddress();
                addrString.append(addr).append(" ");
                if (addr.contains(":") & !addr.contains("%")) {
                    if (!addr.equals(LOCAL_ADDRESS)) {
                        return addr;
                    }
                }
                if ((addr.contains(EXTERNAL_NETWORK_INTERFACE) || addr.contains(EXTERNAL_NETWORK_INTERFACE_MAC))
                    & addr.contains(IPV6_CORRECT_START)) {
                    return addr.substring(0, addr.indexOf("%"));
                }
            }
        }
        throw new UnknownHostException("ipv6 not found, found only: " + addrString);
    }
}
