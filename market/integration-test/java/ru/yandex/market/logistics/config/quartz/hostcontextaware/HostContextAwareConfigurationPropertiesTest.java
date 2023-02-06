package ru.yandex.market.logistics.config.quartz.hostcontextaware;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(
    properties = {
        "host-aware-quartz-config.considered-master-data-centers=vla, sas",
        "host-aware-quartz-config.excluded-master-data-centers=iva, man",
        "host-aware-quartz-config.cache-enabled=false",
        "host-aware-quartz-config.cache-expires-after-write=2m",
        "host.fqdn=supercoolfqdn"
    }
)
class HostContextAwareConfigurationPropertiesTest extends AbstractContextualTest {

    @Autowired
    private HostContextAwareConfigurationProperties properties;

    @Test
    void getConsideredMasterDataCenters() {
        assertThat(properties.getConsideredMasterDataCenters())
            .containsExactlyInAnyOrder("vla", "sas");
    }

    @Test
    void getExcludedMasterDataCenters() {
        assertThat(properties.getExcludedMasterDataCenters())
            .containsExactlyInAnyOrder("iva", "man");
    }

    @Test
    void isCacheEnabled() {
        assertThat(properties.isCacheEnabled())
            .isFalse();
    }

    @Test
    void getCacheExpiresAfterWrite() {
        assertThat(properties.getCacheExpiresAfterWrite())
            .isEqualTo(Duration.ofMinutes(2));
    }

    @Test
    void getHostname() {
        assertThat(properties.getHostname())
            .isEqualTo("supercoolfqdn");
    }
}
