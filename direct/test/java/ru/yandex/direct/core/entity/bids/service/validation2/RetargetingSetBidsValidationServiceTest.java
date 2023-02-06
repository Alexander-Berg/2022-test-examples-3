package ru.yandex.direct.core.entity.bids.service.validation2;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bids.container.SetBidItem;
import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingSetBidsValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStrategy;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.notFoundShowConditionByParameters;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.requiredAtLeastOneOfFieldsForManualStrategy;
import static ru.yandex.direct.core.entity.bids.validation.SetBidConstraints.getBidParams;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.autobudgetPriorityNotMatchStrategy;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.notContainNulls;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingSetBidsValidationServiceTest {

    @Autowired
    private RetargetingSetBidsValidationService validationService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private Steps steps;

    private RetargetingInfo retargetingInfo;
    private long campaignId;
    private long uid;
    private int shard;

    @Before
    public void before() {
        retargetingInfo = steps.retargetingSteps().createDefaultRetargeting();
        campaignId = retargetingInfo.getCampaignId();
        uid = retargetingInfo.getUid();
        shard = retargetingInfo.getShard();
    }

    @Test
    public void compatiblePreValidation_ValidSetBidItem_ResultIsSuccessful() {
        ValidationResult<List<SetBidItem>, Defect> vr =
                validationService.compatiblePreValidation(singletonList(new SetBidItem().withId(1L)));

        assertThat(vr.hasAnyErrors(), is(false));
    }

    @Test
    public void compatiblePreValidation_NegativeId_ValidIdDefect() {
        ValidationResult<List<SetBidItem>, Defect> vr =
                validationService.compatiblePreValidation(singletonList(new SetBidItem().withId(-1L)));

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("id")), validId())));
    }

    @Test
    public void compatiblePreValidation_NullObject_CommonValidatorIsEnabled() {
        ValidationResult<List<SetBidItem>, Defect> vr =
                validationService.compatiblePreValidation(singletonList(null));

        assertThat(vr, hasDefectDefinitionWith(validationError(path(), notContainNulls())));
    }

    @Test
    public void compatibleValidate_NotVisibleCompanyForOperatorUid_NotFoundShowConditionByParameters() {
        SetBidItem setBid = new SetBidItem().withId(retargetingInfo.getRetargetingId());
        List<SetBidItem> setBids = singletonList(setBid);
        List<Retargeting> retargetings = singletonList(retargetingInfo.getRetargeting());

        var anotherClientInfo = steps.clientSteps().createDefaultClient();

        ValidationResult<List<SetBidItem>, Defect> vr =
                validationService.compatibleValidation(setBids, retargetings, anotherClientInfo.getUid(), shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)),
                notFoundShowConditionByParameters(getBidParams(setBid)))));
    }

    @Test
    public void compatibleValidation_OnArchivedCompany_BadCampaignStatusArchivedOnSetBids() {
        List<SetBidItem> setBids = singletonList(new SetBidItem().withId(retargetingInfo.getRetargetingId()));
        List<Retargeting> retargetings = singletonList(retargetingInfo.getRetargeting());

        testCampaignRepository.archiveCampaign(shard, campaignId);

        ValidationResult<List<SetBidItem>, Defect> vr =
                validationService.compatibleValidation(setBids, retargetings, uid, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)),
                BidsDefects.badStatusCampaignArchivedOnUpdateBids(Retargeting.CAMPAIGN_ID, campaignId))));
    }

    @Test
    public void compatibleValidation_NullPriceContext_RequiredPriceContextForManualStrategy() {
        List<SetBidItem> setBids = singletonList(new SetBidItem().withId(retargetingInfo.getRetargetingId()));
        List<Retargeting> retargetings = singletonList(retargetingInfo.getRetargeting());

        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.cpm_default)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.No)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();

        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.STRATEGY, CampOptionsStrategy.different_places)
                .where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();

        ValidationResult<List<SetBidItem>, Defect> vr =
                validationService.compatibleValidation(setBids, retargetings, uid, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("priceContext")),
                requiredAtLeastOneOfFieldsForManualStrategy(singletonList(SetBidItem.PRICE_CONTEXT)))));
    }


    @Test
    public void compatibleValidation_NullAutobudgetPriority_AutobudgetPriorityNotMatchStrategyDefect() {
        List<SetBidItem> setBids = singletonList(new SetBidItem().withId(retargetingInfo.getRetargetingId()));
        List<Retargeting> retargetings = singletonList(retargetingInfo.getRetargeting());

        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.autobudget)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.Yes)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();

        ValidationResult<List<SetBidItem>, Defect> vr =
                validationService.compatibleValidation(setBids, retargetings, uid, shard);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0), field("autobudgetPriority")),
                autobudgetPriorityNotMatchStrategy())));
    }
}
