package ru.yandex.direct.core.entity.mobileapp.repository;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppRepositoryGetLatestMobileAppIdsByMobileContentIdsTest {
    private static final String STORE_URL = "https://itunes.apple.com/ru/app/meduza/id921508170?mt=8";
    private static final String TRACKER_URL1 =
            "https://app.appsflyer.com/112132123?pid=yandexdirect_int&clickid={logid}";
    private static final String IMPRESSION_URL1 =
            "https://impression.appsflyer.com/112132123?pid=yandexdirect_int&clickid={logid}";
    private static final String TRACKER_URL2 = "https://adjust.com/q1w2e3?ios_ifa={ios_ifa}&logid={logid}";
    private static final String IMPRESSION_URL2 = "https://view.adjust.com/impression/q1w2e3?ios_ifa={ios_ifa}&logid={logid}";

    @Autowired
    private MobileAppRepository repository;

    @Autowired
    private Steps steps;

    private Integer shard;
    private ClientId clientId;
    private ClientInfo clientInfo;

    @Before
    public void setUp() throws Exception {
        steps.trustedRedirectSteps().addValidCounters();
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
    }

    @Test
    public void emptyClient() {
        assertThat(repository.getLatestMobileAppIdsByMobileContentIds(shard, clientId, emptyList())).isEmpty();
    }

    @Test
    public void clientWithoutMobileApps() {
        MobileContentInfo mobileContentInfo = steps.mobileContentSteps().createDefaultMobileContent(clientInfo);
        Map<Long, Long> result = repository.getLatestMobileAppIdsByMobileContentIds(shard, clientId,
                singletonList(mobileContentInfo.getMobileContentId()));
        assertThat(result).isEmpty();
    }

    @Test
    public void oneMobileApp() {
        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);
        Long mobileContentId = mobileAppInfo.getMobileContentId();
        Map<Long, Long> result = repository.getLatestMobileAppIdsByMobileContentIds(shard, clientId,
                singletonList(mobileContentId));
        assertThat(result).containsOnly(Pair.of(mobileContentId, mobileAppInfo.getMobileAppId()));
    }

    @Test
    public void oneMobileContent_twoMobileApps() {
        MobileContentInfo mobileContentInfo = steps.mobileContentSteps().createMobileContent(clientInfo, STORE_URL);
        Long mobileContentId = mobileContentInfo.getMobileContentId();
        MobileAppInfo mobileApp1 = steps.mobileAppSteps()
                .createMobileApp(clientInfo, mobileContentInfo, STORE_URL, TRACKER_URL1, IMPRESSION_URL1);
        MobileAppInfo mobileApp2 = steps.mobileAppSteps()
                .createMobileApp(clientInfo, mobileContentInfo, STORE_URL, TRACKER_URL2, IMPRESSION_URL2);

        long maxMobileAppId = Long.max(mobileApp1.getMobileAppId(), mobileApp2.getMobileAppId());

        Map<Long, Long> result = repository.getLatestMobileAppIdsByMobileContentIds(shard, clientId,
                singletonList(mobileContentId));
        assertThat(result).containsOnly(Pair.of(mobileContentId, maxMobileAppId));
    }

    @Test
    public void twoMobileContents_oneMobileApp() {
        MobileContentInfo mobileContentInfo = steps.mobileContentSteps().createDefaultMobileContent(clientInfo);
        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);

        Map<Long, Long> result = repository.getLatestMobileAppIdsByMobileContentIds(shard, clientId,
                asList(mobileContentInfo.getMobileContentId(), mobileAppInfo.getMobileContentId()));
        assertThat(result).containsOnly(Pair.of(mobileAppInfo.getMobileContentId(), mobileAppInfo.getMobileAppId()));
    }
}
