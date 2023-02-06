package ru.yandex.direct.core.entity.feature.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ru.yandex.direct.core.entity.abt.service.UaasInfoService;
import ru.yandex.direct.core.entity.client.service.AgencyClientRelationService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.feature.container.FeatureRequest;
import ru.yandex.direct.core.entity.feature.container.FeatureRequestFactory;
import ru.yandex.direct.core.entity.feature.repository.ClientFeaturesRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.feature.FeatureName;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.common.util.HttpUtil.DETECTED_LOCALE_HEADER_NAME;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeatureServiceForMethodsWithParamIdsTest {

    private static final ClientId CLIENT_ID_1 = ClientId.fromLong(123L);
    private static final ClientId CLIENT_ID_2 = ClientId.fromLong(456L);
    private static final ClientId CLIENT_ID_3 = ClientId.fromLong(789L);

    private static final FeatureName FEATURE_1 = FeatureName.values()[0];
    private static final FeatureName FEATURE_2 = FeatureName.values()[1];

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

    private FeatureService featureService;

    @Before
    public void initTestData() {
        var uaasInfoService = mock(UaasInfoService.class);
        when(uaasInfoService.getInfo(any(List.class))).thenReturn(List.of());
        featureService = spy(new FeatureService(featureCache, clientFeaturesRepository, clientService, shardHelper,
                agencyClientRelationService, uaasInfoService, EnvironmentType.DEVELOPMENT, featureRequestFactory));

        var httpServletRequest = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
        when(httpServletRequest.getHeader(eq("X-Real-IP"))).thenReturn("12.12.12.12");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("yandexuid", "1234534546557")});
        when(httpServletRequest.getHeader(eq("Host"))).thenReturn("test.direct.yandex.ru");
        when(httpServletRequest.getHeader(eq("UserAgent"))).thenReturn("Mozilla/5.0 ...");
        when(httpServletRequest.getAttribute(eq(DETECTED_LOCALE_HEADER_NAME))).thenReturn(Locale.forLanguageTag("ru"));

        FeatureRequest featureRequestForFirstClient = new FeatureRequest()
                .withClientId(CLIENT_ID_1)
                .withIp("12.12.12.12")
                .withYandexuid("1234534546557")
                .withHost("test.direct.yandex.ru")
                .withUserAgent("Mozilla/5.0 ...")
                .withInterfaceLang("ru");
        doReturn(singleton(FEATURE_1.getName()))
                .when(featureService)
                .getEnabledForClientId(featureRequestForFirstClient);

        FeatureRequest featureRequestForSecondClient = new FeatureRequest()
                .withClientId(CLIENT_ID_2)
                .withIp("12.12.12.12")
                .withYandexuid("1234534546557")
                .withHost("test.direct.yandex.ru")
                .withUserAgent("Mozilla/5.0 ...")
                .withInterfaceLang("ru");

        doReturn(singleton(FEATURE_2.getName()))
                .when(featureService)
                .getEnabledForClientId(featureRequestForSecondClient);

        doReturn(singleton(FEATURE_1.getName()))
                .when(featureService)
                .getPublicForClientId(eq(featureRequestForFirstClient));
    }

    @Test
    public void getEnabled() {
        Assert.assertEquals(singleton(FEATURE_1.getName()), featureService.getEnabledForClientId(CLIENT_ID_1));
    }

    @Test
    @Ignore
    public void massGetEnabled() {
        Map<ClientId, Set<String>> expected = Map.of(CLIENT_ID_1, Set.of(FEATURE_1.getName()),
                CLIENT_ID_2, Set.of(FEATURE_2.getName()),
                CLIENT_ID_3, Set.of(FEATURE_2.getName()));
        assertThat(featureService.getEnabled(Set.of(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3)))
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void getPublic() {
        Assert.assertEquals(singleton(FEATURE_1.getName()), featureService.getPublicForClientId(CLIENT_ID_1));
    }

    @Test
    @Ignore
    public void isEnabled_True() {
        assertTrue(featureService.isEnabledForClientId(CLIENT_ID_1, FEATURE_1));
    }
    @Test
    @Ignore
    public void isEnabled_False() {
        assertFalse(featureService.isEnabledForClientId(CLIENT_ID_2, FEATURE_1));
    }

    @Test
    public void nullInterfaceLangTest() {
        var httpServletRequest = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
        when(httpServletRequest.getHeader(eq("X-Real-IP"))).thenReturn("12.12.12.12");
        when(httpServletRequest.getCookies()).thenReturn(new Cookie[]{new Cookie("yandexuid", "1234534546557")});
        when(httpServletRequest.getHeader(eq("Host"))).thenReturn("test.direct.yandex.ru");
        when(httpServletRequest.getHeader(eq("UserAgent"))).thenReturn("Mozilla/5.0 ...");
        when(httpServletRequest.getAttribute(eq(DETECTED_LOCALE_HEADER_NAME))).thenReturn(null);

        featureService.getEnabledForClientId(CLIENT_ID_1);

        verify(featureService)
                .getEnabledForClientId(new FeatureRequest()
                        .withClientId(CLIENT_ID_1)
                        .withIp("12.12.12.12")
                        .withYandexuid("1234534546557")
                        .withHost("test.direct.yandex.ru")
                        .withUserAgent("Mozilla/5.0 ...")
                        .withInterfaceLang(null));
    }
}
