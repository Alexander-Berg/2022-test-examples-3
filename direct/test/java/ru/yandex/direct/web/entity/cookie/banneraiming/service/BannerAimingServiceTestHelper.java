package ru.yandex.direct.web.entity.cookie.banneraiming.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.liveresource.yav.YavLiveResourceProvider;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.liveresource.provider.LiveResourceFactoryBean;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.yav.client.YavClient;
import ru.yandex.direct.yav.client.YavClientStub;

class BannerAimingServiceTestHelper {

    private static final long DEFAULT_TIMESTAMP = 1533889005L;
    private static final long DEFAULT_OPERATOR_UID = 11L;
    private static final RbacRole DEFAULT_OPERATOR_ROLE = RbacRole.AGENCY;
    private static final Clock DEFAULT_CLOCK = Clock.fixed(Instant.ofEpochSecond(DEFAULT_TIMESTAMP), ZoneOffset.UTC);

    private static final String DEFAULT_SECRET_KEY_VALUE =
            "[{\"t\":1533889007, \"d\":\"ZyTB2F+0zdb19i5+h6gASJwCKG8=\", \"f\":1533889003}, " +
                    "{\"t\":1533889003, \"d\":\"FupNBYznQEt7twbTVnhQ/Wu8WJQ=\", \"f\":1533889002}, {\"t\":1533889007," +
                    " \"d\":\"nAHY+CpOo+p1KNMQNhtz3VxzrLE=\", \"f\":1533889012}]";
    private static final String DEFAULT_KEY_NAME = "default";
    private static final String DEFAULT_SECRET_KEY_RESOURCE =
            "yav://sec-01f33ak4h4dec51e399nq43jq6#" + DEFAULT_KEY_NAME;

    private static final DirectWebAuthenticationSourceMock defaultDirectWebAuthenticationSource =
            getDirectWebAuthenticationSourceMock(DEFAULT_OPERATOR_UID, DEFAULT_OPERATOR_ROLE);

    @Autowired
    static BannerCommonRepository bannerCommonRepository;

    @Autowired
    static BannerRelationsRepository bannerRelationsRepository;

    @Autowired
    static ShardHelper shardHelper;

    private static final BannerAimingService defaultBannerAimingService = initDefaultBannerAimingService();

    private static BannerAimingService initDefaultBannerAimingService() {
        return getDefaultBannerAimingServiceWithSecretKeyValue(DEFAULT_SECRET_KEY_VALUE);
    }

    static BannerAimingService getDefaultBannerAimingServiceWithSecretKeyValue(String secretKeyValue) {
        YavClient yavClient = new YavClientStub(Map.of(
                DEFAULT_KEY_NAME, secretKeyValue
        ));

        LiveResourceFactoryBean liveResourceFactoryBean = new LiveResourceFactoryBean(List.of(
                new YavLiveResourceProvider(yavClient)
        ));

        return new BannerAimingService(defaultDirectWebAuthenticationSource,
                DEFAULT_SECRET_KEY_RESOURCE,
                bannerCommonRepository, bannerRelationsRepository, shardHelper, DEFAULT_CLOCK,
                liveResourceFactoryBean);
    }

    static BannerAimingService getDefaultBannerAimingService() {
        return defaultBannerAimingService;
    }

    static DirectWebAuthenticationSourceMock getDirectWebAuthenticationSourceMock(long operatorUid,
                                                                                  RbacRole operatorRole) {
        DirectWebAuthenticationSourceMock directWebAuthenticationSource = new DirectWebAuthenticationSourceMock();
        User operator = TestUsers.defaultUser().withUid(operatorUid).withRole(operatorRole);
        directWebAuthenticationSource.withOperator(operator);
        return directWebAuthenticationSource;
    }

    static Clock getDefaultClock() {
        return DEFAULT_CLOCK;
    }

    static long getDefaultTimestamp() {
        return DEFAULT_TIMESTAMP;
    }
}
