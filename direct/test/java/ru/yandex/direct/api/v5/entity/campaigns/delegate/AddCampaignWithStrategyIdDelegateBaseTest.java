package ru.yandex.direct.api.v5.entity.campaigns.delegate;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.entity.campaigns.converter.CampaignsAddRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.campaign.model.RequestSource;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions;
import ru.yandex.direct.core.entity.strategy.model.BaseStrategy;
import ru.yandex.direct.core.entity.strategy.service.StrategyOperationFactory;
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperation;
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone;
import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.testing.matchers.validation.Matchers;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultGoal;
import static ru.yandex.direct.feature.FeatureName.CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.feature.FeatureName.FIX_CRR_STRATEGY_ALLOWED;
import static ru.yandex.direct.feature.FeatureName.PACKAGE_STRATEGIES_STAGE_TWO;
import static ru.yandex.direct.feature.FeatureName.UNIVERSAL_CAMPAIGNS_BETA_DISABLED;

public abstract class AddCampaignWithStrategyIdDelegateBaseTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Autowired
    public Steps steps;

    @Autowired
    public CampaignOperationService campaignOperationService;

    @Mock
    public GeoTimezoneRepository geoTimezoneRepository;

    @Mock
    public ApiAuthenticationSource authenticationSource;

    @Autowired
    public ResultConverter resultConverter;

    @Autowired
    public PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    public FeatureService featureService;

    @Autowired
    public UserService userService;

    @Autowired
    public MetrikaClientStub metrikaClientStub;

    @Autowired
    public StrategyOperationFactory strategyOperationFactory;

    @Autowired
    public SspPlatformsRepository sspPlatformsRepository;

    public GenericApiService genericApiService;
    public AddCampaignsDelegate delegate;

    public static final String NAME = "Тестовая кампания";

    public static final GeoTimezone AMSTERDAM_TIMEZONE = new GeoTimezone()
            .withTimezone(ZoneId.of("Europe/Amsterdam"))
            .withTimezoneId(174L);

    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final Long COUNTER_ID = 1999L;

    public static final Long VALID_GOAL_ID = 199L;

    public static final String SSP_PLATFORM_1 = "Rubicon";
    public static final String SSP_PLATFORM_2 = "sspplatform.ru";

    public ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        ApiUser user = new ApiUser()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId());

        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getOperator()).thenReturn(user);
        when(auth.getChiefSubclient()).thenReturn(user);

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());

        genericApiService = new GenericApiService(
                apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));

        when(geoTimezoneRepository.getByTimeZones(eq(ImmutableList.of(AMSTERDAM_TIMEZONE.getTimezone().getId()))))
                .thenReturn(Map.of(AMSTERDAM_TIMEZONE.getTimezone().getId(), AMSTERDAM_TIMEZONE));
        when(authenticationSource.getRequestSource())
                .thenReturn(RequestSource.API_DEFAULT);

        var requestConverter = new CampaignsAddRequestConverter(userService, geoTimezoneRepository,
                authenticationSource, sspPlatformsRepository, featureService);

        delegate = new AddCampaignsDelegate(
                auth,
                campaignOperationService,
                requestConverter,
                resultConverter,
                ppcPropertiesSupport,
                featureService);

        metrikaClientStub.addUserCounter(clientInfo.getUid(), Math.toIntExact(COUNTER_ID));
        metrikaClientStub.addCounterGoal(Math.toIntExact(COUNTER_ID), VALID_GOAL_ID.intValue());
        metrikaClientStub.addGoals(clientInfo.getUid(), Set.of(
                ((Goal) ((Goal) defaultGoal(VALID_GOAL_ID))
                        .withCounterId(Math.toIntExact(COUNTER_ID)))
        ));

        steps.sspPlatformsSteps().addSspPlatforms(List.of(SSP_PLATFORM_1, SSP_PLATFORM_2));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), CRR_STRATEGY_ALLOWED, true);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FIX_CRR_STRATEGY_ALLOWED, true);

        // включаем UNIVERSAL_CAMPAIGNS_BETA_DISABLED, чтобы все цели метрики не были автоматически доступны
        // чтобы качественнее проверить валидацию целей
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), UNIVERSAL_CAMPAIGNS_BETA_DISABLED, true);

        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                PACKAGE_STRATEGIES_STAGE_TWO, true);

        steps.campaignSteps().createActiveCampaignUnderWallet(clientInfo);
    }

    public Long createStrategyAndGetId(BaseStrategy strategy) {
        StrategyAddOperation operation =
                strategyOperationFactory.createStrategyAddOperation(
                        clientInfo.getShard(),
                        clientInfo.getUid(),
                        clientInfo.getClientId(),
                        clientInfo.getUid(),
                        List.of(strategy),
                        new StrategyOperationOptions()
                );
        var result = operation.prepareAndApply();
        Assert.assertThat(result.getValidationResult(), Matchers.hasNoDefectsDefinitions());

        return result.get(0).getResult();
    }
}
