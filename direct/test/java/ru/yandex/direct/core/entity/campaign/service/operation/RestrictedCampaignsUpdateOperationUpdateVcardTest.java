package ru.yandex.direct.core.entity.campaign.service.operation;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.AlwaysEqualsDiffer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestVcards.vcardUserFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignsUpdateOperationUpdateVcardTest {
    private static final List<String> SSP_LIST = List.of("ImSSP");
    @Autowired
    CampaignOperationService campaignOperationService;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    public Steps steps;

    private CampaignInfo activeTextCampaign;

    @Before
    public void before() {
        activeTextCampaign = steps.campaignSteps().createActiveTextCampaign();
        steps.adGroupSteps().createActiveTextAdGroup(activeTextCampaign);
        steps.sspPlatformsSteps().addSspPlatforms(SSP_LIST);
    }

    @Test
    public void test() {
        ModelChanges<TextCampaign> textCampaignModelChanges = new ModelChanges<>(activeTextCampaign.getCampaignId(),
                TextCampaign.class);
        Vcard vcard = vcardUserFields(null);
        textCampaignModelChanges.process(vcard, TextCampaign.CONTACT_INFO);
        textCampaignModelChanges.process("1@1.ru", TextCampaign.EMAIL);
        textCampaignModelChanges.process(LocalDate.now().plusDays(1), TextCampaign.START_DATE);

        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation restrictedCampaignUpdateOperation =
                campaignOperationService.createRestrictedCampaignUpdateOperation(
                        List.of(textCampaignModelChanges),
                        activeTextCampaign.getUid(),
                        UidAndClientId.of(activeTextCampaign.getUid(), activeTextCampaign.getClientId()),
                        options);
        MassResult<Long> apply = restrictedCampaignUpdateOperation.apply();
        assertThat(apply.getValidationResult().hasAnyErrors()).isFalse();

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(activeTextCampaign.getShard(),
                        Collections.singletonList(activeTextCampaign.getCampaignId()));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        TextCampaign actualCampaign = textCampaigns.get(0);
        assertThat(actualCampaign.getContactInfo()).is(matchedBy(beanDiffer(vcard)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()
                        .forFields(BeanFieldPath.newPath("geoId")).useDiffer(new AlwaysEqualsDiffer()))));
    }

}
