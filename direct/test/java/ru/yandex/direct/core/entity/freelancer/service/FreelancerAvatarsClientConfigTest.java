package ru.yandex.direct.core.entity.freelancer.service;

import java.net.URI;
import java.net.URISyntaxException;

import org.asynchttpclient.AsyncHttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.avatars.client.AvatarsClient;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.EssentialConfiguration;
import ru.yandex.direct.core.entity.freelancer.model.ClientAvatarsHost;
import ru.yandex.direct.env.EnvironmentType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = EssentialConfiguration.class)
public class FreelancerAvatarsClientConfigTest {
    private static final String FAKE_EXTERNAL_ID = "fake-namespace/1/fake-key";
    private static final String AVATAR_SIZE_CONFIG_NAME = "orig";
    private static final String PROD_READ_SERVER_HOST = "avatars.mds.yandex.net";
    private static final String TEST_READ_SERVER_HOST = "avatars.mdst.yandex.net";

    private AvatarsConfigNameConverter avatarsConfigNameConverter = new AvatarsConfigNameConverter();

    @Test
    public void avatarsClientConfig_correctDbHost_forProductionConfiguration() {
        AvatarsClientPool avatarsClientPool = getFreelancersAvatarsClientPool(EnvironmentType.PRODUCTION);
        String defaultConfigName = avatarsClientPool.getDefaultConfigName();
        ClientAvatarsHost dbEnumHost = avatarsConfigNameConverter.getHost(defaultConfigName);
        assertThat(dbEnumHost).isEqualTo(ClientAvatarsHost.AVATARS_MDS_YANDEX_NET);
    }

    @Test
    public void avatarsClientConfig_correctDbHost_forPrestableConfiguration() {
        AvatarsClientPool avatarsClientPool = getFreelancersAvatarsClientPool(EnvironmentType.PRESTABLE);
        String defaultConfigName = avatarsClientPool.getDefaultConfigName();
        ClientAvatarsHost dbEnumHost = avatarsConfigNameConverter.getHost(defaultConfigName);
        assertThat(dbEnumHost).isEqualTo(ClientAvatarsHost.AVATARS_MDS_YANDEX_NET);
    }

    @Test
    public void avatarsClientConfig_correctDbHost_forTestingConfiguration() {
        AvatarsClientPool avatarsClientPool = getFreelancersAvatarsClientPool(EnvironmentType.TESTING);
        String defaultConfigName = avatarsClientPool.getDefaultConfigName();
        ClientAvatarsHost dbEnumHost = avatarsConfigNameConverter.getHost(defaultConfigName);
        assertThat(dbEnumHost).isEqualTo(ClientAvatarsHost.AVATARS_MDST_YANDEX_NET);
    }

    @Test
    public void avatarsClientConfig_correctHost_forProductionConfiguration() throws URISyntaxException {
        AvatarsClientPool avatarsClientPool = getFreelancersAvatarsClientPool(EnvironmentType.PRODUCTION);
        String defaultReadServerHost = getDefaultReadServerHost(avatarsClientPool);
        assertThat(defaultReadServerHost).isEqualTo(PROD_READ_SERVER_HOST);
    }

    @Test
    public void avatarsClientConfig_correctHost_forPrestableConfiguration() throws URISyntaxException {
        AvatarsClientPool avatarsClientPool = getFreelancersAvatarsClientPool(EnvironmentType.PRESTABLE);
        String defaultReadServerHost = getDefaultReadServerHost(avatarsClientPool);
        assertThat(defaultReadServerHost).isEqualTo(PROD_READ_SERVER_HOST);
    }

    @Test
    public void avatarsClientConfig_correctHost_forTestingConfiguration() throws URISyntaxException {
        AvatarsClientPool avatarsClientPool = getFreelancersAvatarsClientPool(EnvironmentType.TESTING);
        String defaultReadServerHost = getDefaultReadServerHost(avatarsClientPool);
        assertThat(defaultReadServerHost).isEqualTo(TEST_READ_SERVER_HOST);
    }

    @Test
    public void avatarsClientConfig_prodHostPresent_forTestingConfiguration() throws URISyntaxException {
        AvatarsClientPool avatarsClientPool = getFreelancersAvatarsClientPool(EnvironmentType.TESTING);
        String prodConfigName = avatarsConfigNameConverter.getConfigName(ClientAvatarsHost.AVATARS_MDS_YANDEX_NET);
        AvatarsClient prodClient = avatarsClientPool.getClient(prodConfigName);
        String prodReadServerHost = getReadServerHost(prodClient);
        assertThat(prodReadServerHost).isEqualTo(PROD_READ_SERVER_HOST);
    }

    private AvatarsClientPool getFreelancersAvatarsClientPool(EnvironmentType environmentType) {
        DirectConfig config = EssentialConfiguration.config(environmentType, null);
        AvatarsClientPoolFactory avatarsClientPoolFactory =
                new AvatarsClientPoolFactory(config.getBranch("freelancer_avatars_client"),
                        mock(AsyncHttpClient.class), null, null);
        return avatarsClientPoolFactory.getAvatarsClientPool();
    }

    private String getDefaultReadServerHost(AvatarsClientPool avatarsClientPool) throws URISyntaxException {
        AvatarsClient defaultClient = avatarsClientPool.getDefaultClient();
        return getReadServerHost(defaultClient);
    }

    private String getReadServerHost(AvatarsClient defaultClient) throws URISyntaxException {
        String url = defaultClient.getReadUrl(FAKE_EXTERNAL_ID, AVATAR_SIZE_CONFIG_NAME);
        URI uri = new URI(url);
        return uri.getHost();
    }
}
