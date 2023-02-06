package ru.yandex.direct.web.configuration;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import ru.yandex.direct.abac.Attribute;
import ru.yandex.direct.common.net.NetAcl;
import ru.yandex.direct.core.entity.banner.type.creative.BannerCreativeRepository;
import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.entity.uac.repository.stat.AssetStatRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.entity.zora.ZoraService;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.core.testing.configuration.UacYdbTestingConfiguration;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.excel.processing.service.internalad.CryptaSegmentDictionariesService;
import ru.yandex.direct.grid.processing.service.attributes.AttributeResolverService;
import ru.yandex.direct.web.configuration.mock.auth.DirectWebAuthenticationSourceMock;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource;
import ru.yandex.direct.web.core.security.csrf.CsrfInterceptor;
import ru.yandex.direct.ytcore.entity.statistics.service.RecentStatisticsService;

@Configuration
@Import({DirectWebAppConfiguration.class, CoreTestingConfiguration.class, UacYdbTestingConfiguration.class})
public class TestingDirectWebAppConfiguration {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSteps userSteps;

    @Bean(name = DirectWebAuthenticationSource.BEAN_NAME)
    public DirectWebAuthenticationSource directWebAuthenticationSourceMock() {
        return new DirectWebAuthenticationSourceMock();
    }

    @MockBean
    public AssetStatRepository assetStatRepository;

    @MockBean
    public BannerCreativeRepository bannerCreativeRepository;

    @SpyBean
    public AttributeResolverService attributeResolverService;

    @Bean
    public AttributeResolverService attributeResolverService(
            DirectWebAuthenticationSource directWebAuthenticationSource, FeatureService featureService) {
        return new AttributeResolverService(directWebAuthenticationSource, featureService) {
            @Override
            public boolean resolve(Attribute... values) {
                return true;
            }
        };
    }

    @SpyBean
    public SspPlatformsRepository sspPlatformsRepository;

    @Bean
    public SspPlatformsRepository sspPlatformsRepository(DslContextProvider dslContextProvider) {
        return new SspPlatformsRepository(dslContextProvider) {
            @Override
            public List<String> getAllSspPlatforms() {
                return List.of();
            }
        };
    }

    @Bean
    public CsrfInterceptor csrfInterceptor() {
        return new CsrfInterceptor(null, null, null) {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                return true;
            }
        };
    }

    @Bean
    public TestAuthHelper testAuthHelper() {
        DirectWebAuthenticationSourceMock authSourceMock =
                DirectWebAuthenticationSourceMock.castToMock(directWebAuthenticationSourceMock());
        return new TestAuthHelper(authSourceMock, userService, userSteps);
    }

    @MockBean
    public RecentStatisticsService recentStatisticsService;

    @SpyBean
    public CryptaSegmentDictionariesService cryptaSegmentDictionariesService;

    @Bean
    public CryptaSegmentDictionariesService cryptaSegmentDictionariesService(CryptaSegmentRepository cryptaSegmentRepository) {
        return new CryptaSegmentDictionariesService(cryptaSegmentRepository) {
            @Override
            public List<String> getGenderValues() {
                return List.of("Мужчины", "Женщины");
            }

            @Override
            public List<String> getAgeValues() {
                return List.of("<18", "18-24", "25-34", "35-44", "45-54", "55+");
            }

            @Override
            public List<String> getIncomeValues() {
                return List.of("Низкий", "Средний", "Выше среднего", "Высокий", "Премиум");
            }
        };
    }

    @SpyBean
    public ZoraService zoraService;

    @Bean
    public MockMvc mockMvc(WebApplicationContext ctx, CharacterEncodingFilter characterEncodingFilter) {
        return MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(characterEncodingFilter)
                .build();
    }

    @SpyBean
    public NetAcl netAcl;
}
