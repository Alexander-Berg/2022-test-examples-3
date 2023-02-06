package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithDisabledVideoPlacements;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.entity.campaign.service.validation.DisableDomainValidationService;
import ru.yandex.direct.core.entity.client.repository.ClientLimitsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.client.Constants.DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithDisabledVideoPlacementsValidatorTest {

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    private DisableDomainValidationService disableDomainValidationService;

    @Autowired
    private ClientLimitsRepository clientLimitsRepository;

    @Autowired
    private ShardHelper shardHelper;

    @Mock
    private FeatureService featureService;


    @SuppressWarnings("unused")
    private static Object[] parametrizedTestData() {
        return new Object[][]{
                {"null", null, (int) DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT, false, null, null},
                {"empty list", emptyList(), (int) DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT, false, null, null},
                {"placements amount as limit", List.of("vk.ru", "rambler.ru"), 2, false, null, null},
                {"too many placements", List.of("vk.ru", "rambler.ru"), 1, false,
                        path(field(CampaignWithDisabledVideoPlacements.DISABLED_VIDEO_PLACEMENTS.name())),
                        CollectionDefects.maxCollectionSize(1)},
                {"mail.ru without any domain allowed", List.of("mail.ru"), (int) DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT,
                        false, null, null},
                {"mail.ru with any domain allowed", List.of("mail.ru"), (int) DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT,
                        true, null, null},
                {"duplicated domain", List.of("vk.ru", "vk.ru"), (int) DEFAULT_VIDEO_BLACKLIST_SIZE_LIMIT, false,
                        path(field(CampaignWithDisabledVideoPlacements.DISABLED_VIDEO_PLACEMENTS.name())),
                        CampaignDefects.duplicatedStrings(List.of("vk.ru"))},
        };
    }

    private CampaignWithDisabledVideoPlacementsValidator validator;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);
        disableDomainValidationService = new DisableDomainValidationService(clientLimitsRepository, shardHelper,
                featureService);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("description = {0}")
    public void checkCampaignWithDisabledVideoPlacementsValidator(@SuppressWarnings("unused") String testDescription,
                                                                  @Nullable List<String> placements,
                                                                  Integer videoBlacklistSizeLimit,
                                                                  Boolean disableAnyDomainsAllowed,
                                                                  @Nullable Path expectedPath,
                                                                  @Nullable Defect expectedDefect) {

        Mockito.when(featureService.isEnabledForClientId(Mockito.any(ClientId.class),
                Mockito.eq(FeatureName.DISABLE_ANY_DOMAINS_ALLOWED))).thenReturn(disableAnyDomainsAllowed);

        CampaignWithDisabledVideoPlacements campaign = new CpmBannerCampaign()
                .withDisabledVideoPlacements(placements);

        validator = new CampaignWithDisabledVideoPlacementsValidator(videoBlacklistSizeLimit,
                ClientId.fromLong(1L), disableDomainValidationService);

        checkValidator(campaign, expectedPath, expectedDefect);
    }

    private void checkValidator(CampaignWithDisabledVideoPlacements campaign, @Nullable Path expectedPath,
                                @Nullable Defect expectedDefect) {
        ValidationResult<CampaignWithDisabledVideoPlacements, Defect> result = validator.apply(campaign);

        if (expectedDefect == null) {
            assertThat(result).
                    is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            assertThat(result).
                    is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }

}
