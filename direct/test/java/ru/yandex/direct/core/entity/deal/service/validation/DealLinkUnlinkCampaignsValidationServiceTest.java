package ru.yandex.direct.core.entity.deal.service.validation;

import java.util.HashMap;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.deal.container.CampaignDeal;
import ru.yandex.direct.core.entity.deal.container.UpdateDealContainer;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealBase;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.entity.deal.repository.DealRepository;
import ru.yandex.direct.core.entity.deal.service.DealTransitionsService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignTypeNotSupported;
import static ru.yandex.direct.core.entity.deal.service.validation.DealDefects.dealCurrencyShouldMatchCampaign;
import static ru.yandex.direct.core.entity.deal.service.validation.DealDefects.dealIsNotActive;
import static ru.yandex.direct.core.entity.deal.service.validation.DealValidationService.AVAILABLE_CAMPAIGN_TYPES;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.requiredButEmpty;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class DealLinkUnlinkCampaignsValidationServiceTest {
    private DealValidationService dealValidationService;

    private long dealId1 = 11L;
    private long dealId2 = 22L;
    private long campaignId1 = 1L;
    private long campaignId2 = 2L;
    private CurrencyCode defaultCurrencyCode = CurrencyCode.RUB;

    private Map<Long, DealBase> existingDeals = existingDeals();
    private Map<Long, CampaignSimple> existingCampaigns = existingCampaigns();

    private int shard = 1;
    private ClientId agencyId = ClientId.fromLong(111L);
    private long agencyUid = 222;

    @Before
    public void setUp() {
        RbacService rbacService = mock(RbacService.class);
        DealRepository dealRepository = mock(DealRepository.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        DealTransitionsService dealTransitionsService = new DealTransitionsService();
        ClientService clientService = mock(ClientService.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        DslContextProvider dslContextProvider = mock(DslContextProvider.class);

        dealValidationService = new DealValidationService(rbacService, dealRepository, campaignRepository,
                dealTransitionsService, clientService, shardHelper, dslContextProvider);

        dealValidationService = spy(dealValidationService);

        doReturn(existingCampaigns)
                .when(dealValidationService).getExistingCampaigns(eq(agencyUid), any());

        doReturn(existingDeals)
                .when(dealValidationService).getExistingDeals(eq(shard), eq(agencyId));

        doReturn(new Client()
                .withId(agencyId.asLong())
                .withWorkCurrency(defaultCurrencyCode)
        ).when(clientService).getClient(agencyId);
    }


    @Test
    public void validateLinkUnlinkCampaigns_NullAddedAndRemoved() {
        UpdateDealContainer campaignsContainer = new UpdateDealContainer();

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(),
                        requiredButEmpty()))));
    }

    @Test
    public void validateLinkUnlinkCampaigns_EmptyAddedAndRemoved() {
        UpdateDealContainer campaignsContainer =
                new UpdateDealContainer().withAdded(emptyList()).withRemoved(emptyList());

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(),
                        requiredButEmpty()))));
    }

    @Test
    public void validateLinkUnlinkCampaigns_TwoDealsToCampaign() {
        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withAdded(asList(new CampaignDeal().withDealId(dealId1).withCampaignId(campaignId1),
                        new CampaignDeal().withDealId(dealId2).withCampaignId(campaignId1)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateLinkUnlinkCampaigns_CampaignNotFound() {
        long someCampaignId = 444L;
        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withAdded(singletonList(new CampaignDeal().withDealId(dealId1).withCampaignId(someCampaignId)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        field(UpdateDealContainer.ADDED.name()),
                        index(0),
                        field(CampaignDeal.CAMPAIGN_ID.name())),
                        objectNotFound()))));
    }

    @Test
    public void validateLinkUnlinkCampaigns_ArchivedCampaign() {
        long archivedCampaignId = 55L;
        existingCampaigns.put(archivedCampaignId, defaultCampaign(archivedCampaignId).withStatusArchived(true));
        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withAdded(singletonList(new CampaignDeal().withDealId(dealId1).withCampaignId(archivedCampaignId)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        field(UpdateDealContainer.ADDED.name()),
                        index(0),
                        field(CampaignDeal.CAMPAIGN_ID.name())),
                        archivedCampaignModification()))));
    }

    @Test
    public void validateLinkUnlinkCampaigns_CurrencyNotMatched() {
        long campaignId = 55L;
        existingCampaigns.put(campaignId, defaultCampaign(campaignId).withCurrency(CurrencyCode.USD));
        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withAdded(singletonList(new CampaignDeal().withDealId(dealId1).withCampaignId(campaignId)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        field(UpdateDealContainer.ADDED.name()),
                        index(0)),
                        dealCurrencyShouldMatchCampaign(CurrencyCode.USD)))));
    }


    @Test
    public void validateLinkUnlinkCampaigns_RemoveNotDraftCampaign() {
        long campaignId = 55L;
        existingCampaigns.put(campaignId, defaultCampaign(campaignId).withStatusModerate(CampaignStatusModerate.SENT));

        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withRemoved(singletonList(new CampaignDeal().withDealId(dealId1).withCampaignId(campaignId)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateLinkUnlinkCampaigns_AddNotDraftCampaign() {
        long campaignId = 55L;
        existingCampaigns.put(campaignId, defaultCampaign(campaignId).withStatusModerate(CampaignStatusModerate.SENT));

        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withAdded(singletonList(new CampaignDeal().withDealId(dealId1).withCampaignId(campaignId)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateLinkUnlinkCampaigns_InvalidCampaignType() {
        long campaignId = 55L;
        existingCampaigns.put(campaignId, defaultCampaign(campaignId).withType(notAllowedCampaignType()));

        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withAdded(singletonList(new CampaignDeal().withDealId(dealId1).withCampaignId(campaignId)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        field(UpdateDealContainer.ADDED.name()),
                        index(0),
                        field(CampaignDeal.CAMPAIGN_ID.name())),
                        campaignTypeNotSupported()))));
    }

    @Test
    public void validateLinkUnlinkCampaigns_DealNotFound() {
        long someDealId = 444L;
        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withAdded(singletonList(new CampaignDeal().withDealId(someDealId).withCampaignId(campaignId1)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        field(UpdateDealContainer.ADDED.name()),
                        index(0),
                        field(CampaignDeal.DEAL_ID.name())),
                        objectNotFound()))));
    }

    @Test
    public void validateLinkUnlinkCampaigns_DealIsNotActive() {
        long dealId = 55L;
        existingDeals.put(dealId, defaultDeal(dealId).withDirectStatus(StatusDirect.COMPLETED));

        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withAdded(singletonList(new CampaignDeal().withDealId(dealId).withCampaignId(campaignId1)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        field(UpdateDealContainer.ADDED.name()),
                        index(0),
                        field(CampaignDeal.DEAL_ID.name())),
                        dealIsNotActive()))));
    }

    /**
     * На уделённые 1н кейс, т.к. они проверяются тем же самым валидатором, что и при добавлении.
     */
    @Test
    public void validateLinkUnlinkCampaigns_DealNotFoundWhenRemove() {
        long someDealId = 444L;
        UpdateDealContainer campaignsContainer = new UpdateDealContainer()
                .withRemoved(singletonList(new CampaignDeal().withDealId(someDealId).withCampaignId(campaignId1)));

        ValidationResult<UpdateDealContainer, Defect> vr =
                dealValidationService
                        .validateUpdateDeal(shard, agencyUid, agencyId, campaignsContainer);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        field(UpdateDealContainer.REMOVED.name()),
                        index(0),
                        field(CampaignDeal.DEAL_ID.name())),
                        objectNotFound()))));
    }

    private Map<Long, CampaignSimple> existingCampaigns() {
        Map<Long, CampaignSimple> result = new HashMap<>();

        Campaign campaign = defaultCampaign(campaignId1);
        result.put(campaign.getId(), campaign);

        campaign = defaultCampaign(campaignId2);
        result.put(campaign.getId(), campaign);
        return result;
    }

    private Campaign defaultCampaign(Long campaignId) {
        return new Campaign()
                .withId(campaignId)
                .withType(CampaignType.CPM_DEALS)
                .withStatusArchived(false)
                .withCurrency(defaultCurrencyCode)
                .withStatusModerate(CampaignStatusModerate.NEW);
    }

    private Map<Long, DealBase> existingDeals() {
        Map<Long, DealBase> result = new HashMap<>();
        Deal deal1 = defaultDeal(dealId1);
        Deal deal2 = defaultDeal(dealId2);
        result.put(deal1.getId(), deal1);
        result.put(deal2.getId(), deal2);

        return result;
    }

    private Deal defaultDeal(Long dealId) {
        Deal deal = new Deal();
        deal.withId(dealId)
                .withDirectStatus(StatusDirect.ACTIVE)
                .withCurrencyCode(defaultCurrencyCode);
        return deal;
    }

    private CampaignType notAllowedCampaignType() {
        return StreamEx.of(CampaignType.values())
                .filter(t -> !AVAILABLE_CAMPAIGN_TYPES.contains(t))
                .findFirst().orElse(CampaignType.CPM_BANNER);
    }
}
