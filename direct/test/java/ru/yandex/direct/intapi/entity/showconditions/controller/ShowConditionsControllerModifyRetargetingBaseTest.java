package ru.yandex.direct.intapi.entity.showconditions.controller;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestCpmYndxFrontpageRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RetargetingItem;
import ru.yandex.direct.intapi.entity.showconditions.model.request.RetargetingModificationContainer;
import ru.yandex.direct.intapi.entity.showconditions.model.request.ShowConditionsRequest;
import ru.yandex.direct.intapi.entity.showconditions.model.response.ShowConditionsResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@RunWith(SpringJUnit4ClassRunner.class)
@IntApiTest
public abstract class ShowConditionsControllerModifyRetargetingBaseTest {

    private static final Double FRONTPAGE_PRICE_LOW_FOR_RUSSIA = .5;
    private static final Double FRONTPAGE_PRICE_SUFFICIENT_FOR_RUSSIA = 5.;

    protected RetargetingInfo retargetingInfo;

    protected int shard;
    protected long uid;
    protected long clientId;
    protected long adGroupId;
    protected long campaignId;
    protected long objectId;

    @Autowired
    protected ShowConditionsController showConditionsController;

    @Autowired
    protected TestCampaignRepository campaignRepository;

    @Autowired
    protected Steps steps;

    @Autowired
    private TestCpmYndxFrontpageRepository testCpmYndxFrontpageRepository;

    @Before
    public void before() {
        testCpmYndxFrontpageRepository.fillMinBidsTestValues();
        retargetingInfo = steps.retargetingSteps().createDefaultRetargeting();

        shard = retargetingInfo.getShard();
        uid = retargetingInfo.getUid();
        clientId = retargetingInfo.getClientId().asLong();
        adGroupId = retargetingInfo.getAdGroupId();
        campaignId = retargetingInfo.getCampaignId();
    }

    protected ShowConditionsRequest buildDeleteRequest(Long adGroupId, Long retargetingId) {
        ShowConditionsRequest request = new ShowConditionsRequest();

        RetargetingModificationContainer container = new RetargetingModificationContainer();
        container.getDeleted().add(retargetingId);
        request.getRetargetings().put(adGroupId, container);

        return request;
    }

    protected ShowConditionsRequest buildEditRequest(Long adGroupId, Long retargetingId, RetargetingItem item) {
        ShowConditionsRequest request = new ShowConditionsRequest();

        RetargetingModificationContainer container = new RetargetingModificationContainer();
        container.getEdited().put(retargetingId, item);
        request.getRetargetings().put(adGroupId, container);

        return request;
    }

    protected void updateAndAssertThatHasDefect(ShowConditionsRequest request, Long adGroupId) {
        ShowConditionsResponse response = (ShowConditionsResponse) showConditionsController
                .update(request, uid, clientId);

        assertThat(response.getItems().get(adGroupId).getErrors(), notNullValue());
        assertThat(response.getItems().get(adGroupId).getErrors(), not(empty()));
    }

    protected void archiveCampaign(long campaignId, int shard) {
        campaignRepository.archiveCampaign(shard, campaignId);
    }

    protected ShowConditionsResponse buildFrontpageRequestAndGetResponse(ClientInfo clientInfo,
                                                                         Double retargetingPrice) {
        CampaignInfo frontpageCampaignInfo =
                steps.campaignSteps().createActiveCpmYndxFrontpageCampaign(clientInfo);
        AdGroupInfo frontpageAdGroupInfo =
                steps.adGroupSteps().createDefaultCpmYndxFrontpageAdGroup(frontpageCampaignInfo);
        steps.adGroupSteps().setAdGroupProperty(frontpageAdGroupInfo, AdGroup.GEO,
                Collections.singletonList(RUSSIA_REGION_ID));
        testCpmYndxFrontpageRepository.setCpmYndxFrontpageCampaignsAllowedFrontpageTypes(
                frontpageAdGroupInfo.getShard(), frontpageAdGroupInfo.getCampaignId(),
                Collections.singletonList(FrontpageCampaignShowType.FRONTPAGE));
        RetargetingInfo frontpageRetargetingInfo =
                steps.retargetingSteps().createDefaultRetargeting(frontpageAdGroupInfo);
        ShowConditionsRequest request = buildEditRequest(frontpageAdGroupInfo.getAdGroupId(),
                frontpageRetargetingInfo.getRetargetingId(),
                new RetargetingItem().withPriceContext(BigDecimal.valueOf(retargetingPrice)));
        return (ShowConditionsResponse) showConditionsController
                .update(request, clientInfo.getUid(), clientInfo.getClientId().asLong());
    }

    @Test
    public void update_FrontpageAdGroup_NoDefects() {
        ShowConditionsResponse response =
                buildFrontpageRequestAndGetResponse(retargetingInfo.getClientInfo(), 100.);
        assertThat("Price context update must have no errors", response.isSuccessful(), is(true));
    }

    @Test
    public void update_FrontpageAdGroup_UkraineClient_InsufficientPrice_ErrorTest() {
        ClientInfo ukraineClient = steps.clientSteps().createClient(defaultClient()
                .withCountryRegionId(UKRAINE_REGION_ID)
                .withWorkCurrency(CurrencyCode.CHF));
        ShowConditionsResponse response =
                buildFrontpageRequestAndGetResponse(ukraineClient, FRONTPAGE_PRICE_LOW_FOR_RUSSIA);
        assertThat("Price context must have errors", response.isSuccessful(), is(false));
    }

    @Test
    public void update_FrontpageAdGroup_RussiaClient_InsufficientPrice_ErrorTest() {
        ClientInfo russianClient = steps.clientSteps().createClient(defaultClient()
                .withCountryRegionId(RUSSIA_REGION_ID)
                .withWorkCurrency(CurrencyCode.CHF));
        ShowConditionsResponse response =
                buildFrontpageRequestAndGetResponse(russianClient, FRONTPAGE_PRICE_LOW_FOR_RUSSIA);
        assertThat("Price context must have errors", response.isSuccessful(), is(false));
    }


    @Test
    public void update_FrontpageAdGroup_UkraineClient_SufficientPrice_SuccessTest() {
        ClientInfo ukraineClient = steps.clientSteps().createClient(defaultClient()
                .withCountryRegionId(UKRAINE_REGION_ID)
                .withWorkCurrency(CurrencyCode.CHF));
        ShowConditionsResponse response =
                buildFrontpageRequestAndGetResponse(ukraineClient, FRONTPAGE_PRICE_SUFFICIENT_FOR_RUSSIA);
        assertThat("Price context must have no errors", response.isSuccessful(), is(true));
    }

    @Test
    public void update_FrontpageAdGroup_RussiaClient_SufficientPrice_SuccessTest() {
        ClientInfo russianClient = steps.clientSteps().createClient(defaultClient()
                .withCountryRegionId(RUSSIA_REGION_ID)
                .withWorkCurrency(CurrencyCode.CHF));
        ShowConditionsResponse response =
                buildFrontpageRequestAndGetResponse(russianClient, FRONTPAGE_PRICE_SUFFICIENT_FOR_RUSSIA);
        assertThat("Price context must have no errors", response.isSuccessful(), is(true));
    }

    @Test
    public void update_PriceContextLessThanMin_SearchPriceIsNotGreaterThanMin() {
        ShowConditionsRequest request = buildEditRequest(adGroupId, objectId,
                new RetargetingItem().withPriceContext(BigDecimal.valueOf(-1)));

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_PriceContextGreaterThanMax_SearchPriceIsNotSmallerThanMax() {
        ShowConditionsRequest request = buildEditRequest(adGroupId, objectId,
                new RetargetingItem().withPriceContext(BigDecimal.valueOf(100000)));

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_DeleteInArchivedCampaign_CampaignStatusArchivedOnDelete() {
        archiveCampaign(campaignId, shard);
        ShowConditionsRequest request = buildDeleteRequest(adGroupId, objectId);

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_SuspendInArchivedCampaign_CampaignStatusArchivedOnSuspend() {
        archiveCampaign(campaignId, shard);
        ShowConditionsRequest request = buildEditRequest(adGroupId, objectId,
                new RetargetingItem().withIsSuspended(1));

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_ResumeInArchivedCampaign_CampaignStatusArchivedOnResume() {
        archiveCampaign(campaignId, shard);
        ShowConditionsRequest request = buildEditRequest(adGroupId, objectId,
                new RetargetingItem().withIsSuspended(0));

        updateAndAssertThatHasDefect(request, adGroupId);
    }

    @Test
    public void update_SuspendOnNonVisibleCampaign_NotFound() {
        uid = steps.clientSteps().createDefaultClient().getUid();

        ShowConditionsRequest request = buildEditRequest(adGroupId, objectId,
                new RetargetingItem().withIsSuspended(1));

        updateAndAssertThatHasDefect(request, adGroupId);
    }
}
