package ru.yandex.chemodan.app.tcm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.chemodan.app.tcm.services.ConferenceServiceContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanTestBaseContextConfiguration;
import ru.yandex.chemodan.zk.configuration.ImportZkEmbeddedConfiguration;
import ru.yandex.chemodan.zk.configuration.ZkEmbedded;
import ru.yandex.commune.zk2.ZkConfiguration;
import ru.yandex.commune.zk2.ZkPath;
import ru.yandex.misc.net.HostnameUtils;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author friendlyevil
 */
@Configuration
@Import({ConferenceServiceContextConfiguration.class})
@ImportZkEmbeddedConfiguration
public class TcmTestContextConfiguration extends ChemodanTestBaseContextConfiguration {
    @Override
    protected AppName appName() {
        return new SimpleAppName("tcm-test", "tcm-test");
    }

    @Bean
    public ZkConfiguration zkConfiguration(ZkEmbedded zkEmbedded) {
        return zkEmbedded.getConfiguration();
    }

    @Bean
    public ZkEmbedded zkEmbedded() throws Exception {
        return new ZkEmbedded();
    }

    @Bean
    public ZkPath zkRoot() {
        return new ZkPath("/tcm/test/" + HostnameUtils.localHostname() + "_" + Random2.R.nextAlnum(5));
    }
}
