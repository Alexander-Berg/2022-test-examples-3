package ru.yandex.direct.core.entity.retargeting.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.bids.container.SetBidItem;
import ru.yandex.direct.core.entity.bids.container.ShowConditionType;
import ru.yandex.direct.core.entity.bids.validation.BidsDefects;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStrategy;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsAutobudget;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStrategyName;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.notFoundShowConditionByParameters;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.oneOfFieldsShouldBeSpecified;
import static ru.yandex.direct.core.entity.bids.validation.BidsDefects.requiredAtLeastOneOfFieldsForManualStrategy;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.autobudgetPriorityNotMatchStrategy;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NewRetargetingServiceSetBidsTest extends BaseTestWithCtreatedRetargetings {

    @Autowired
    private Steps steps;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    public OldBannerRepository bannerRepository;

    private static final CompareStrategy STRATEGY =
            allFieldsExcept(newPath("adGroupId"), newPath("campaignId"), newPath("priceContext"));

    @Test
    public void setBids_OneItem_ResultIsFullySuccessful() {
        setBidsForItemsAndCheckResult(singletonList(bid1()), singletonList(true));
    }

    @Test
    public void setBids_OneItem_BidIsSetCorrectly() {
        setBidsForItemsAndCheckBids(singletonList(bid1()), singletonList(true));
        setBidsForItemsAndCheckBids(singletonList(bid1()), singletonList(true));
    }

    @Test
    public void setBids_OneItemWithNonExistentId_RetargetingNotFound() {
        setBidsForItemsAndCheckResult(singletonList(bid1().withId(100000L)), singletonList(false));
    }

    @Test
    public void setBids_BothItemsValid_ResultIsFullySuccessful() {
        setBidsForItemsAndCheckResult(asList(bid1(), bid2()), asList(true, true));
    }

    @Test
    public void setBids_BothItemsValid_BidaIsSetCorrectly() {
        setBidsForItemsAndCheckBids(asList(bid1(), bid2()), asList(true, true));
    }

    @Test
    public void setBids_OneOfItemsValid_ResultIsPartlySuccessful() {
        setBidsForItemsAndCheckResult(asList(bid1().withAutobudgetPriority(0), bid2()), asList(false, true));
    }

    @Test
    public void setBids_OneOfItemsValid_BidIsSetCorrectly() {
        setBidsForItemsAndCheckBids(asList(bid1().withAutobudgetPriority(0), bid2()), asList(false, true));
    }

    @Test
    public void setBids_NoItemsValid_ElementsResultsIsBroken() {
        setBidsForItemsAndCheckResult(asList(bid1().withAutobudgetPriority(0), bid2().withAutobudgetPriority(0)),
                asList(false, false));
    }

    @Test
    public void setBids_NoItemsValid_BidsIsNotSet() {
        setBidsForItemsAndCheckBids(asList(bid1().withAutobudgetPriority(0), bid2().withAutobudgetPriority(0)),
                asList(false, false));
    }

    @Test
    public void setBids_EmptyList_ResultIsBroken() {
        MassResult<SetBidItem> result = serviceUnderTest.setBids(new ArrayList<>(), clientId, uid);
        assertThat("результат операции отрицательный", result.isSuccessful(), is(false));
    }

    @Test
    public void setBids_ByAdGroup_TwoItemsUpdateIsSuccessful() {
        SetBidItem adGroupSetBid = bid1().withId(null)
                .withAdGroupId(retargetingInfo1.getAdGroupId());

        setBidsForItemsAndCheckResult(singletonList(adGroupSetBid), singletonList(true));
    }

    @Test
    public void setBids_DropsRetargetingStatusBsSynced() {
        Assert.state(retargetingInfo1.getRetargeting().getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус ретаргетинга statusBsSynced сброшен");

        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid1()), clientId, uid);
        assumeThat("результат операции положительный", result.isSuccessful(), is(true));
        assumeThat("поэлементные результаты соответствуют ожидаемым",
                mapList(result.getResult(), Result::isSuccessful), contains(true));

        Retargeting retargeting = retargetingRepository
                .getRetargetingsByIds(shard, singletonList(retargetingId1), maxLimited()).get(0);
        assertThat("статус statusBsSynced ретаргетинга сброшен",
                retargeting.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void setBids_DoesNotDropAdGroupStatusBsSynced() {
        Assert.state(retargetingInfo1.getAdGroupInfo().getAdGroup().getStatusBsSynced() == StatusBsSynced.YES,
                "невозможно провести тест: статус группы statusBsSynced сброшен");

        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid1()), clientId, uid);
        assumeThat(result, isSuccessful(true));

        AdGroup adGroup = adGroupRepository.getAdGroups(shard, singletonList(retargetingInfo1.getAdGroupId())).get(0);
        assertThat("статус statusBsSynced группы не сброшен", adGroup.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void setBids_InvalidBidSelector_CommonSetBidsValidationServicePreValidateConnected() {
        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(new SetBidItem()), clientId, uid);
        assumeThat(result, isSuccessful());

        List<ModelProperty<?, ?>> bidsIdsFields = asList(SetBidItem.ID, SetBidItem.AD_GROUP_ID, SetBidItem.CAMPAIGN_ID);
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path(index(0)),
                oneOfFieldsShouldBeSpecified(bidsIdsFields))));
    }

    @Test
    public void setBids_NegativedBidSelector_ValidIdDefect() {
        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid1().withId(-1L)), clientId, uid);
        assumeThat(result, isSuccessful());

        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path(index(0), field("id")),
                validId())));
    }

    @Test
    public void setBids_NotVisibleForUser_NotFoundShowConditionByParametersDefect() {
        SetBidItem bid = bid1();
        uid = steps.clientSteps().createDefaultClient().getUid();

        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid), clientId, uid);
        assumeThat(result, isSuccessful());

        BidsDefects.BidsParams param = new BidsDefects.BidsParams()
                .withField(SetBidItem.ID)
                .withId(bid.getId());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), notFoundShowConditionByParameters(param))));
    }

    @Test
    public void setBids_ChangePriceByAdGroup_PriceChangedForAllRetargetings() {
        BigDecimal newPriceContext = BigDecimal.valueOf(66.6);

        SetBidItem bid = new SetBidItem()
                .withAdGroupId(retargetingInfo1.getAdGroupId())
                .withPriceContext(newPriceContext);

        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid), clientId, uid);
        assumeThat(result, isSuccessful());

        assertThat("количество успешных изменений считается по изменениям в базе", result.getSuccessfulCount(), is(2));

        List<SetBidItem> bids = getBids(asList(retargetingId1, retargetingId2));
        assertThat(bids.get(0).getPriceContext(), comparesEqualTo(newPriceContext));
        assertThat(bids.get(1).getPriceContext(), comparesEqualTo(newPriceContext));
    }

    @Test
    public void setBids_ChangePriceByCampaign_PriceChangedForAllRetargetings() {
        BigDecimal newPriceContext = BigDecimal.valueOf(66.6);

        SetBidItem bid = new SetBidItem()
                .withCampaignId(retargetingInfo1.getCampaignId())
                .withPriceContext(newPriceContext);

        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid), clientId, uid);
        assumeThat(result, isSuccessful());

        assertThat("количество успешных изменений считается по изменениям в базе", result.getSuccessfulCount(), is(2));

        List<SetBidItem> bids = getBids(asList(retargetingId1, retargetingId2));
        assertThat(bids.get(0).getPriceContext(), comparesEqualTo(newPriceContext));
        assertThat(bids.get(1).getPriceContext(), comparesEqualTo(newPriceContext));
    }

    @Test
    public void setBids_InvalidBidsByCampaign_CountInvalidByCampaign() {
        SetBidItem bid = new SetBidItem()
                .withCampaignId(retargetingInfo1.getCampaignId())
                .withPriceContext(BigDecimal.valueOf(-1));

        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid), clientId, uid);
        assertThat(result, isSuccessful(false));
        assertThat(result.getErrorCount(), is(1));
    }

    @Test
    public void setBids_OnArchivedCompany_BadCampaignStatusArchivedOnSetBids() {
        SetBidItem bid = bid1();

        testCampaignRepository.archiveCampaign(shard, campaignId);

        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid), clientId, uid);

        assertThat(result.getValidationResult(), hasDefectDefinitionWith(validationError(path(index(0)),
                BidsDefects.badStatusCampaignArchivedOnUpdateBids(Retargeting.CAMPAIGN_ID, campaignId))));
    }

    @Test
    public void setBids_NullPriceOnManualStrategy_RequiredAtLeastOneOfFieldsForManualStrategyDefect() {
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

        SetBidItem bid = bid1().withPriceContext(null);
        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid), clientId, uid);

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("priceContext")),
                        requiredAtLeastOneOfFieldsForManualStrategy(singletonList(SetBidItem.PRICE_CONTEXT)))));
    }

    @Test
    public void setBids_NullPriorityOnAutobudgetStrategy_AutoBudgetPriorityIsNotNullForAutoStrategy() {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STRATEGY_NAME, CampaignsStrategyName.autobudget)
                .set(CAMPAIGNS.AUTOBUDGET, CampaignsAutobudget.Yes)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();

        SetBidItem bid = bid1().withAutobudgetPriority(null);
        MassResult<SetBidItem> result = serviceUnderTest.setBids(singletonList(bid), clientId, uid);

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("autobudgetPriority")),
                        autobudgetPriorityNotMatchStrategy())));
    }

    private void setBidsForItemsAndCheckResult(List<SetBidItem> setBidItems, List<Boolean> elementsResults) {
        MassResult<SetBidItem> result = serviceUnderTest.setBids(setBidItems, clientId, uid);
        assertThat(result, isSuccessful(elementsResults));
    }

    private void setBidsForItemsAndCheckBids(List<SetBidItem> setBidItems, List<Boolean> elementsResults) {
        List<SetBidItem> oldSetBidItems = getBids(mapList(setBidItems, SetBidItem::getId));

        MassResult<SetBidItem> result = serviceUnderTest.setBids(setBidItems, clientId, uid);
        assumeThat(result, isSuccessful(elementsResults));

        List<SetBidItem> actualSetBidItems = getBids(mapList(setBidItems, SetBidItem::getId));

        // да, это не хорошо, но по другому будет непонятно
        for (int i = 0; i < setBidItems.size(); i++) {
            if (elementsResults.get(i)) {
                assertThat("ставка изменилась правильно", actualSetBidItems.get(i),
                        beanDiffer(setBidItems.get(i)).useCompareStrategy(STRATEGY));
                assertThat("ставка изменилась правильно", actualSetBidItems.get(i).getPriceContext().doubleValue(),
                        equalTo(setBidItems.get(i).getPriceContext().doubleValue()));
            } else {
                assertThat("ставка осталась неизменной", actualSetBidItems.get(i),
                        beanDiffer(oldSetBidItems.get(i)).useCompareStrategy(STRATEGY));
                assertThat("ставка осталась неизменной", actualSetBidItems.get(i).getPriceContext().doubleValue(),
                        equalTo(oldSetBidItems.get(i).getPriceContext().doubleValue()));
            }
        }
    }

    private List<SetBidItem> getBids(List<Long> ids) {
        List<Retargeting> retargetings = retargetingRepository.getRetargetingsByIds(shard, ids, maxLimited());
        return mapList(retargetings,
                r -> new SetBidItem()
                        .withId(r.getId())
                        .withPriceContext(r.getPriceContext())
                        .withAutobudgetPriority(r.getAutobudgetPriority())
                        .withShowConditionType(ShowConditionType.AUDIENCE_TARGET)
        );
    }

    private SetBidItem bid1() {
        return new SetBidItem().withId(retargetingId1)
                .withPriceContext(BigDecimal.valueOf(89.3))
                .withAutobudgetPriority(1)
                .withShowConditionType(ShowConditionType.AUDIENCE_TARGET);
    }

    private SetBidItem bid2() {
        return new SetBidItem().withId(retargetingId2)
                .withPriceContext(BigDecimal.valueOf(54.7))
                .withAutobudgetPriority(5)
                .withShowConditionType(ShowConditionType.AUDIENCE_TARGET);
    }
}
