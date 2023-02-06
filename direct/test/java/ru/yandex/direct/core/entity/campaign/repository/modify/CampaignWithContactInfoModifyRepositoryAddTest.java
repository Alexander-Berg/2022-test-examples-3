package ru.yandex.direct.core.entity.campaign.repository.modify;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignWithSystemFieldsByCampaignType;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithContactInfoModifyRepositoryAddTest {
    @Autowired
    public CampaignModifyRepository campaignModifyRepository;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    CampaignRepository campaignRepository;
    @Autowired
    SspPlatformsRepository sspPlatformsRepository;
    @Autowired
    public Steps steps;
    @Autowired
    public DslContextProvider dslContextProvider;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();


    private ClientInfo defaultClientAndUser;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        defaultClientAndUser = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void insert() {
        Vcard contactInfo = fullVcard()
                .withGeoId(null)
                .withPointType(null)
                .withLastChange(null)
                .withLastDissociation(null);
        CommonCampaign campaignOne = getCampaign().withContactInfo(contactInfo);

        List<CommonCampaign> campaigns = List.of(campaignOne);
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                RestrictedCampaignsAddOperationContainer.create(defaultClientAndUser.getShard(), defaultClientAndUser.getUid(),
                        defaultClientAndUser.getClientId(), defaultClientAndUser.getUid(),
                        defaultClientAndUser.getUid());
        List<Long> ids = campaignModifyRepository.addCampaigns(dslContextProvider.ppc(defaultClientAndUser.getShard()),
                addCampaignParametersContainer,
                campaigns);
        assertThat(ids).hasSize(1);

        List<? extends CommonCampaign> typedCampaigns = (List<CommonCampaign>)
                campaignTypedRepository.getTypedCampaigns(defaultClientAndUser.getShard(),
                        Collections.singletonList(campaignOne.getId()));
        CommonCampaign actualCampaign = typedCampaigns.get(0);
        assertThat(actualCampaign.getContactInfo()).is(matchedBy(beanDiffer(contactInfo)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())));
    }

    private CommonCampaign getCampaign() {
        CommonCampaign campaign = defaultCampaignWithSystemFieldsByCampaignType(campaignType);
        return campaign
                .withTimeZoneId(130L)
                .withUid(defaultClientAndUser.getUid())
                .withClientId(defaultClientAndUser.getClientId().asLong());
    }
}
