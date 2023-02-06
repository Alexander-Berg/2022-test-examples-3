package ru.yandex.direct.core.entity.feature.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.abt.container.AllowedFeatures;
import ru.yandex.direct.core.entity.abt.service.UaasInfoService;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.entity.feature.container.FeatureRequestFactory;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.rbac.RbacRole;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureServiceWithoutUaasAPISandboxTest {
    @Autowired
    private FeatureCache featureCache;
    @Autowired
    private ClientFeaturesRepository clientFeaturesRepository;
    @Autowired
    private ClientService clientService;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private AgencyClientRelationService agencyClientRelationService;
    @Autowired
    private FeatureRequestFactory featureRequestFactory;

    @Autowired
    private Steps steps;

    private FeatureService featureService;
    private UaasInfoService uaasInfoService;

    private static final String IP = "12.12.12.12";
    private static final String YANDEXUID = "1233453453";
    private static final String HOST = "host.ru";
    private static final String USER_AGENT = "User-agent";

    private FeatureRequest featureRequest;

    @Before
    public void setUp() {
        uaasInfoService = mock(UaasInfoService.class);
        featureService = new FeatureService(featureCache, clientFeaturesRepository, clientService, shardHelper,
                agencyClientRelationService, uaasInfoService, EnvironmentType.SANDBOX_DEVELOPMENT, featureRequestFactory);
        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient(RbacRole.MANAGER));
        featureRequest = new FeatureRequest()
                .withClientId(clientInfo.getClientId())
                .withUserAgent(USER_AGENT)
                .withHost(HOST)
                .withIp(IP)
                .withYandexuid(YANDEXUID)
                .withInterfaceLang("RU");
    }

    @Test
    public void checkUaasDisabledForAPISandbox() {
        featureService.getFeaturesWithExpBoxes(featureRequest);
        verify(uaasInfoService, never()).getInfo(anyList(), any(AllowedFeatures.class));
    }
}
