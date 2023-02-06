package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.validation.defects.params.ModelIdDefectParams;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Использование некорректных идентификаторов кампании и группы")
public class AddBidModifiersInvalidIDsTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private ClientSteps clientSteps;

    private ClientInfo client1;
    private ClientInfo client2;

    // Активная кампания и группа, заведённая клиентом client1
    private CampaignInfo activeCampaign;
    private AdGroupInfo activeAdGroup;

    // Архивная кампания и группа, заведённая клиентом client1
    private CampaignInfo archivedCampaign;
    private AdGroupInfo adGroupInArchivedCampaign;

    private static final long UNEXISTING_CAMPAIGN_ID = 123456789L;
    private static final long UNEXISTING_AD_GROUP_ID = 123456789L;

    @Before
    public void before() {
        client1 = clientSteps.createDefaultClient();

        activeAdGroup = adGroupSteps.createActiveTextAdGroup(client1);
        activeCampaign = activeAdGroup.getCampaignInfo();

        adGroupInArchivedCampaign = adGroupSteps.createAdGroup(defaultTextAdGroup(null),
                new CampaignInfo().withCampaign(activeTextCampaign(client1.getClientId(), client1.getUid())
                        .withArchived(true)).withClientInfo(client1));
        archivedCampaign = adGroupInArchivedCampaign.getCampaignInfo();

        client2 = clientSteps.createDefaultClient();
    }

    @Test
    @Description("Пытаемся добавить корректировки к группе объявлений в рамках архивной кампании")
    public void testAdGroupInArchivedCampaign() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier().withAdGroupId(adGroupInArchivedCampaign.getAdGroupId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), client1.getClientId(), client1.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")),
                        new Defect<>(CampaignDefectIds.Gen.ARCHIVED_CAMPAIGN_MODIFICATION))));
    }

    @Test
    @Description("Пытаемся добавить корректировки к группе объявлений в рамках кампании другого клиента")
    public void testAdGroupInNotOwnedCampaign() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier().withAdGroupId(activeAdGroup.getAdGroupId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), client2.getClientId(), client2.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")),
                        new Defect<>(RetargetingDefectIds.IdParametrized.ADGROUP_NOT_FOUND,
                                new ModelIdDefectParams().withId(activeAdGroup.getAdGroupId())))));
    }

    @Test
    @Description("Пытаемся добавить корректировки к несуществующей группе объявлений")
    public void testNotExistingAdGroup() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier().withAdGroupId(UNEXISTING_AD_GROUP_ID)
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), client2.getClientId(), client2.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")),
                        new Defect<>(RetargetingDefectIds.IdParametrized.ADGROUP_NOT_FOUND,
                                new ModelIdDefectParams().withId(UNEXISTING_AD_GROUP_ID)))));
    }

    @Test
    @Description("Пытаемся добавить корректировки к несуществующей кампании")
    public void testNotExistingCampaign() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier().withCampaignId(UNEXISTING_CAMPAIGN_ID)
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), client2.getClientId(), client2.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("campaignId")),
                        new Defect<>(CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND))));
    }

    @Test
    @Description("Пытаемся добавить корректировки к архивной кампании")
    public void testArchivedCampaign() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier().withCampaignId(archivedCampaign.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), client1.getClientId(), client1.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("campaignId")),
                        new Defect<>(CampaignDefectIds.Gen.ARCHIVED_CAMPAIGN_MODIFICATION))));
    }

    @Test
    @Description("Пытаемся добавить корректировки к кампании другого клиента")
    public void testNotOwnedCampaign() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier().withCampaignId(activeCampaign.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())
                ), client2.getClientId(), client2.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("campaignId")),
                        new Defect<>(CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND))));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers, ClientId clientId, Long clientUid) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, clientId, clientUid);
    }
}
