package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithDisabledDomainsAndSsp;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.entity.campaign.service.validation.DisableDomainValidationService;
import ru.yandex.direct.core.entity.client.repository.ClientLimitsRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.client.Constants.DEFAULT_DISABLED_PLACES_COUNT_LIMIT;
import static ru.yandex.direct.feature.FeatureName.DISABLE_ANY_DOMAINS_ALLOWED;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithDisabledDomainsAndSspUpdateValidatorTest {
    private static final List<String> SSP_LIST = List.of("Smaato", "ssp.ru");

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
                {"all null", null, null, null, null, false},
                {"all empty lists", emptyList(), emptyList(), null, null, false},
                {"valid ssp", emptyList(), List.of("Smaato"), null, null, false},
                {"ssp that is domain", List.of("ssp.ru"), List.of("ssp.ru"), null, null, false},
                {"1000 domains", getBigList(1000, null), emptyList(), null, null, false},
                {"999 domains and ssp that is domain", getBigList(999, "ssp.ru"), List.of("ssp.ru"), null, null, false},
                {"1001 domains", getBigList(1001, null), emptyList(), emptyPath(),
                        CollectionDefects.maxCollectionSize(DEFAULT_DISABLED_PLACES_COUNT_LIMIT), false},
                {"invalid ssp", emptyList(), List.of("smaato"),
                        path(field(CampaignWithDisabledDomainsAndSsp.DISABLED_SSP.name()), index(0)),
                        CampaignDefects.unknownSsp("smaato"), false},
                {"mail.ru", List.of("mail.ru"), emptyList(),
                        null, null, false},
                {"duplicated domain", List.of("vk.ru", "vk.ru"), emptyList(), emptyPath(),
                        CampaignDefects.duplicatedStrings(List.of("vk.ru")), false},
                {"duplicated ssp", emptyList(), List.of("Smaato", "Smaato"), emptyPath(),
                        CampaignDefects.duplicatedStrings(List.of("Smaato")), false},
                {"duplicated ssp and domain", List.of("vk.ru", "vk.ru"), List.of("Smaato", "Smaato"), emptyPath(),
                        CampaignDefects.duplicatedStrings(List.of("Smaato", "vk.ru")), false},
                {"all null", null, null, null, null, true},
                {"all empty lists", emptyList(), emptyList(), null, null, true},
                {"valid ssp", emptyList(), List.of("Smaato"), null, null, true},
                {"ssp that is domain", List.of("ssp.ru"), List.of("ssp.ru"), null, null, true},
                {"1000 domains", getBigList(1000, null), emptyList(), null, null, true},
                {"999 domains and ssp that is domain", getBigList(999, "ssp.ru"), List.of("ssp.ru"), null, null, true},
                {"1001 domains", getBigList(1001, null), emptyList(), emptyPath(),
                        CollectionDefects.maxCollectionSize(DEFAULT_DISABLED_PLACES_COUNT_LIMIT), true},
                {"invalid ssp", emptyList(), List.of("smaato"),
                        path(field(CampaignWithDisabledDomainsAndSsp.DISABLED_SSP.name()), index(0)),
                        CampaignDefects.unknownSsp("smaato"), true},
                {"mail.ru", List.of("mail.ru"), emptyList(), null, null, true},
                {"duplicated domain", List.of("vk.ru", "vk.ru"), emptyList(), emptyPath(),
                        CampaignDefects.duplicatedStrings(List.of("vk.ru")), true},
                {"duplicated ssp", emptyList(), List.of("Smaato", "Smaato"), emptyPath(),
                        CampaignDefects.duplicatedStrings(List.of("Smaato")), true},
                {"duplicated ssp and domain", List.of("vk.ru", "vk.ru"), List.of("Smaato", "Smaato"), emptyPath(),
                        CampaignDefects.duplicatedStrings(List.of("Smaato", "vk.ru")), true},
        };
    }

    @Mock
    public SspPlatformsRepository sspPlatformsRepository;

    private CampaignWithDisabledDomainsAndSspValidator validator;

    @Before
    public void initTestData() {
        MockitoAnnotations.initMocks(this);

        doReturn(SSP_LIST)
                .when(sspPlatformsRepository).getAllSspPlatforms();

        disableDomainValidationService = new DisableDomainValidationService(clientLimitsRepository, shardHelper, featureService);

        validator = new CampaignWithDisabledDomainsAndSspValidator(sspPlatformsRepository,
                ClientId.fromLong(1L), DEFAULT_DISABLED_PLACES_COUNT_LIMIT, disableDomainValidationService);
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("description = {0} and feature disableAnyDomainsAllowed = {5}")
    public void checkCampaignWithDisabledDomainsAndSspUpdateValidator(@SuppressWarnings("unused") String testDescription,
                                                                      @Nullable List<String> domains,
                                                                      @Nullable List<String> ssp,
                                                                      @Nullable Path expectedPath,
                                                                      @Nullable Defect expectedDefect,
                                                                      boolean disableAnyDomainsAllowed) {

        Mockito.when(featureService.isEnabledForClientId(Mockito.any(ClientId.class), Mockito.eq(DISABLE_ANY_DOMAINS_ALLOWED))).thenReturn(disableAnyDomainsAllowed);

        validator = new CampaignWithDisabledDomainsAndSspValidator(sspPlatformsRepository,
                ClientId.fromLong(1L), DEFAULT_DISABLED_PLACES_COUNT_LIMIT, disableDomainValidationService);

        CampaignWithDisabledDomainsAndSsp campaign = new TextCampaign()
                .withDisabledDomains(domains)
                .withDisabledSsp(ssp);

        checkValidator(campaign, expectedPath, expectedDefect);
    }

    private void checkValidator(CampaignWithDisabledDomainsAndSsp campaign, @Nullable Path expectedPath,
                                @Nullable Defect expectedDefect) {
        ValidationResult<CampaignWithDisabledDomainsAndSsp, Defect> result = validator.apply(campaign);

        if (expectedDefect == null) {
            assertThat(result).
                    is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            assertThat(result).
                    is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }

    private static List<String> getBigList(int size, @Nullable String toAdd) {
        List<String> validDomains = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            validDomains.add(RandomStringUtils.randomAlphabetic(10) + ".ru");
        }
        if (toAdd != null) {
            validDomains.add(toAdd);
        }
        return validDomains;
    }
}
