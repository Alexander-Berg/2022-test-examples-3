package ru.yandex.direct.excel.processing.service.internalad;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.copyentity.CopyConfig;
import ru.yandex.direct.core.copyentity.CopyEntityTestUtils;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupAddOrUpdateItem;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupOperationContainer;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupOperationContainer.RequestSource;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.campaign.InternalAutobudgetCampaignInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.excel.processing.configuration.ExcelProcessingTest;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.validInternalNetworkTargeting;
import static ru.yandex.direct.core.validation.defects.Defects.badStatusCampaignArchived;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ExcelProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyOperationInternalAutobudgetCampaignTest extends BaseCopyOperationInternalCampaignTest {

    private Long autobudgetCampaignId;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        ClientId clientId = clientInfo.getClientId();
        asserts.init(clientId, clientId, uid);

        InternalAutobudgetCampaign autobudgetCampaign =
                TestCampaigns.defaultInternalAutobudgetCampaignWithSystemFields(clientInfo);

        InternalAutobudgetCampaignInfo internalAutobudgetCampaignInfo =
                (InternalAutobudgetCampaignInfo) new InternalAutobudgetCampaignInfo()
                        .withTypedCampaign(autobudgetCampaign)
                        .withClientInfo(clientInfo);

        steps.trustedRedirectSteps().addValidCounters();

        var internalAutobudgetCampaign =
                steps.internalAutobudgetCampaignSteps().createCampaign(internalAutobudgetCampaignInfo);
        autobudgetCampaignId = internalAutobudgetCampaign.getId();
    }

    @Test
    public void autobudgetCampaignIsCopied() {
        CopyConfig copyConfig = CopyEntityTestUtils.campaignCopyConfig(clientInfo, autobudgetCampaignId, uid);
        xerox = factory.build(copyConfig);
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        assertCampaignIsCopied(copiedCampaignIds, autobudgetCampaignId);
    }

    @Test
    public void archivedAutobudgetCampaign_notCopied() {
        CopyConfig copyConfig = CopyEntityTestUtils.campaignCopyConfig(clientInfo, autobudgetCampaignId, uid);
        xerox = factory.build(copyConfig);
        steps.campaignSteps().archiveCampaign(clientInfo.getShard(), autobudgetCampaignId);

        var copyResult = xerox.copy();
        assertThat(copyResult.getMassResult().getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(
                        path(index(0), field(CommonCampaign.STATUS_ARCHIVED)),
                        badStatusCampaignArchived()))));
    }

    @Test
    public void autobudgetCampaign_adGroupsCopied() {
        var validInternalNetworkTargeting = validInternalNetworkTargeting();

        var addItem = (InternalAdGroup) steps.adGroupSteps().createActiveInternalAdGroup(clientInfo).getAdGroup()
                .withCampaignId(autobudgetCampaignId);

        var itemToAdd = new InternalAdGroupAddOrUpdateItem()
                .withAdGroup(addItem.withId(null))
                .withAdditionalTargetings(List.of(validInternalNetworkTargeting));

        var operationContainer =
                new InternalAdGroupOperationContainer(
                        Applicability.FULL, clientInfo.getUid(),
                        UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                        true,
                        RequestSource.EXCEL
                );

        addOrUpdateInternalAdGroupsService
                .addOrUpdateInternalAdGroups(List.of(itemToAdd), operationContainer, false);

        CopyConfig copyConfig = CopyEntityTestUtils.campaignCopyConfig(clientInfo, autobudgetCampaignId, uid);
        xerox = factory.build(copyConfig);
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        assertCampaignIsCopied(copiedCampaignIds, autobudgetCampaignId);

        Set<Long> copiedAdGroupIds = Set.copyOf(copyResult.getEntityMapping(AdGroup.class).values());
        asserts.assertAdGroupIsCopied(copiedAdGroupIds, addItem.getId());
    }

}
