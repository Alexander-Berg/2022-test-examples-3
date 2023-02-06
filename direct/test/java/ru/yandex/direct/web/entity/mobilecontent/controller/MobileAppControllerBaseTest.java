package ru.yandex.direct.web.entity.mobilecontent.controller;

import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileApp;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.entity.mobilecontent.model.WebUpdateMobileAppRequest;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

public class MobileAppControllerBaseTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    protected Steps steps;

    @Autowired
    protected MobileAppController mobileAppController;

    @Autowired
    protected DirectWebAuthenticationSource authenticationSource;

    protected ClientInfo clientInfo;
    protected int shard;
    protected ClientId clientId;
    protected long retCondId;
    protected User user;

    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        setAuthData();

        addFeature(FeatureName.IN_APP_MOBILE_TARGETING);
        addFeature(FeatureName.IN_APP_MOBILE_TARGETING_CUSTOM_EVENTS_FOR_EXTERNAL_TRACKERS);
    }

    private void addFeature(FeatureName featureName) {
        steps.featureSteps().addFeature(featureName);
        Long featureId = steps.featureSteps().getFeatures().stream()
                .filter(f -> f.getFeatureTextId().equals(featureName.getName()))
                .map(Feature::getId)
                .findFirst()
                .get();

        ClientFeature featureIdToClientId =
                new ClientFeature()
                        .withClientId(clientInfo.getClientId())
                        .withId(featureId)
                        .withState(FeatureState.ENABLED);
        steps.featureSteps().addClientFeature(featureIdToClientId);
    }

    protected void setAuthData() {
        DirectWebAuthenticationSourceMock authSource =
                (DirectWebAuthenticationSourceMock) authenticationSource;
        authSource.withOperator(new User()
                .withUid(clientInfo.getUid())
                .withRole(RbacRole.MANAGER));
        authSource.withSubjectUser(new User()
                .withClientId(clientInfo.getClientId())
                .withUid(clientInfo.getUid()));

        user = clientInfo.getChiefUserInfo().getUser();
        SecurityContextHolder.getContext()
                .setAuthentication(new DirectAuthentication(user, user));
    }

    protected static WebMobileApp defaultMobileApp() {
        return new WebMobileApp()
                .withName("app")
                .withDomain("ya.ru")
                .withTrackers(emptyList())
                .withDisplayedAttributes(emptySet());
    }

    protected static WebUpdateMobileAppRequest defaultUpdateRequest() {
        return new WebUpdateMobileAppRequest()
                .withName("app")
                .withDomain("ya.ru")
                .withTrackers(emptyList())
                .withDisplayedAttributes(emptySet());
    }
}
