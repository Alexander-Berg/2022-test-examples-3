package ru.yandex.autotests.innerpochta.rules;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;

import static org.apache.commons.lang3.StringUtils.isEmpty;


/**
 * Created by angrybird on 30/10/14.
 * Logs test start.
 */
public class ZombieProxyEnableRule extends TestWatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZombieProxyEnableRule.class);

    private static final String PROXY_HOST = "qa-mail--autotests-proxy.sas.yp-c.yandex.net";
    private static final String PROXY_PORT = "3128";

    @Override
    protected void starting(Description description) {
        if (isLocal()) {
            /* http://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html */
            String excluded = String.join(
                "|",
                "localhost|127.*|[::1]",
                "sg.yandex-team.ru",
                "testpalm-api.yandex-team.ru",
                "mailimport.online.yandex.net",
                "upload.stat.yandex-team.ru",
                "tus.yandex-team.ru",
                "sw.yandex-team.ru",
                "*mail.yandex-team.ru",
                "*mail.yandex.ru"
            );
            LOGGER.info("Local debug, setup proxy via [{}:{}]", PROXY_HOST, PROXY_PORT);
            LOGGER.info("Excluded for proxy: {}\n", excluded);
            System.setProperty("proxySet", "true");
            System.setProperty("http.proxyHost", PROXY_HOST);
            System.setProperty("http.proxyPort", PROXY_PORT);
            System.setProperty("https.proxyHost", PROXY_HOST);
            System.setProperty("https.proxyPort", PROXY_PORT);
            System.setProperty("http.nonProxyHosts", excluded);
            System.setProperty("is.local.debug", "true");
        }
    }


    private boolean isLocal() {
        AquaProps aqua = PropertyLoader.newInstance().populate(AquaProps.class);
        return isEmpty(aqua.aeroLaunchId());
    }

    interface AquaProps {
        @Property("aqua.pack.id")
        String aeroPackId();

        @Property("aero.launch.uuid")
        String aeroLaunchId();

        @Property("aero.suite.uuid")
        String aeroSuiteId();
    }
}
