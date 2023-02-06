package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDisabledDomainsAndSsp;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.DisableDomainValidationService;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.client.service.ClientLimitsService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.repository.TestClientLimitsRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_DOMAIN_LENGTH;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Places.INVALID_DOMAIN_OR_SSP;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Places.UNKNOWN_SSP;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithDisabledDomainsAndSspAddValidationTypeSupportTest {
    private static final int SHARD = 1;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private SspPlatformsRepository sspPlatformsRepository;

    @Mock
    private FeatureService featureService;

    @Autowired
    private ClientLimitsService clientLimitsService;

    @Autowired
    private TestClientLimitsRepository testClientLimitsRepository;

    @Autowired
    DisableDomainValidationService disableDomainValidationService;

    @Autowired
    private Steps steps;

    private CampaignWithDisabledDomainsAndSspAddValidationTypeSupport validationTypeSupport;

    private ClientId clientId;
    private Long operatorUid;
    private CampaignValidationContainer container;
    private Long campaignId;
    private List<String> allSspPlatforms;
    private static final String CORRECTLY_DISABLED_DOMAIN = "google.com";
    private static final String INCORRECTLY_DISABLED_DOMAIN = "www.direct.yandex.ru";

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        validationTypeSupport = new CampaignWithDisabledDomainsAndSspAddValidationTypeSupport(sspPlatformsRepository,
                clientLimitsService, disableDomainValidationService);

        clientId = steps.clientSteps().createDefaultClient().getClientId();
        campaignId = RandomNumberUtils.nextPositiveLong();
        operatorUid = RandomNumberUtils.nextPositiveLong();
        container = CampaignValidationContainer.create(SHARD, operatorUid, clientId);
        allSspPlatforms = List.of(RandomStringUtils.random(73), RandomStringUtils.random(73));
        doReturn(allSspPlatforms)
                .when(sspPlatformsRepository).getAllSspPlatforms();
    }

    @Test
    public void validateEmpty_Successfully() {
        var campaign = createCampaign(List.of(), List.of());
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateNull_Successfully() {
        var campaign = createCampaign(null, null);
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_Successfully() {
        var campaign = createCampaign(List.of(CORRECTLY_DISABLED_DOMAIN),
                List.of(allSspPlatforms.get(RandomUtils.nextInt(0, allSspPlatforms.size()))));
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_invalidDomain() {
        var campaign = createCampaign(List.of(INCORRECTLY_DISABLED_DOMAIN),
                List.of(allSspPlatforms.get(RandomUtils.nextInt(0, allSspPlatforms.size()))));
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithDisabledDomainsAndSsp.DISABLED_DOMAINS), index(0)),
                INVALID_DOMAIN_OR_SSP)));
    }

    @Test
    public void validate_invalidDomainLength() {
        var campaign = createCampaign(List.of(RandomStringUtils.random(MAX_DOMAIN_LENGTH + 1)),
                List.of(allSspPlatforms.get(RandomUtils.nextInt(0, allSspPlatforms.size()))));
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithDisabledDomainsAndSsp.DISABLED_DOMAINS), index(0)),
                LENGTH_CANNOT_BE_MORE_THAN_MAX)));
    }

    @Test
    public void validate_invalidSsp() {
        var campaign = createCampaign(List.of(CORRECTLY_DISABLED_DOMAIN),
                List.of("invalid Ssp"));
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithDisabledDomainsAndSsp.DISABLED_SSP), index(0)),
                UNKNOWN_SSP)));
    }

    @Test
    public void validate_withLimits_Successfully() {
        testClientLimitsRepository.addClientLimits(SHARD,
                (ClientLimits) new ClientLimits().withClientId(clientId).withGeneralBlacklistSizeLimit(2L));

        var campaign = createCampaign(List.of(CORRECTLY_DISABLED_DOMAIN),
                List.of(allSspPlatforms.get(RandomUtils.nextInt(0, allSspPlatforms.size()))));
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_withLimits_sizeCannotBeMoreThanMax() {
        testClientLimitsRepository.addClientLimits(SHARD,
                (ClientLimits) new ClientLimits().withClientId(clientId).withGeneralBlacklistSizeLimit(1L));

        var campaign = createCampaign(List.of(CORRECTLY_DISABLED_DOMAIN),
                List.of(allSspPlatforms.get(RandomUtils.nextInt(0, allSspPlatforms.size()))));
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0)),
                SIZE_CANNOT_BE_MORE_THAN_MAX)));
    }

    private CampaignWithDisabledDomainsAndSsp createCampaign(List<String> disabledDomains, List<String> disabledSsp) {
        CommonCampaign campaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignId)
                .withClientId(clientId.asLong())
                .withName("valid_campaign_name")
                .withUid(operatorUid);
        return ((CampaignWithDisabledDomainsAndSsp) campaign)
                .withDisabledDomains(disabledDomains)
                .withDisabledSsp(disabledSsp);
    }
}
