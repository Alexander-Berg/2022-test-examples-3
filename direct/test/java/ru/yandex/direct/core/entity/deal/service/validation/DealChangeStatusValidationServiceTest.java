package ru.yandex.direct.core.entity.deal.service.validation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.EntryStream;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealAdfox;
import ru.yandex.direct.core.entity.deal.model.DealBase;
import ru.yandex.direct.core.entity.deal.model.StatusAdfox;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.entity.deal.repository.DealRepository;
import ru.yandex.direct.core.entity.deal.service.DealTransitionsService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.deal.service.validation.DealDefects.transitionIsUnavailable;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.notContainNulls;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.objectNotFound;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class DealChangeStatusValidationServiceTest {
    private DealValidationService dealValidationService;

    private CurrencyCode defaultCurrencyCode = CurrencyCode.RUB;

    private int shard = 1;
    private long dealId = 11L;

    private Map<Long, DealBase> existingDeals = existingDeals();
    private Map<Long, StatusDirect> dealStatuses = dealStatuses();

    @Before
    public void before() {
        RbacService rbacService = mock(RbacService.class);
        DealRepository dealRepository = mock(DealRepository.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        DealTransitionsService dealTransitionsService = new DealTransitionsService();
        ClientService clientService = mock(ClientService.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        DslContextProvider dslContextProvider = mock(DslContextProvider.class);

        dealValidationService = new DealValidationService(rbacService, dealRepository, campaignRepository,
                dealTransitionsService, clientService, shardHelper, dslContextProvider);
        doReturn(mock(DSLContext.class)).when(dslContextProvider).ppc(anyInt());
        doReturn(dealStatuses).when(dealRepository).getDealsStatuses(eq(shard), any());
        doReturn(dealStatuses).when(dealRepository).getDealsStatuses(any(DSLContext.class), any());

    }

    @Test
    public void validateChangeStatus_correct() {
        dealValidationService.validateChangeStatus(shard, Collections.singletonList(dealId), StatusDirect.RECEIVED);
    }

    @Test
    public void validateChangeStatus_listOfIdsIsNull() {
        ValidationResult<List<Long>, Defect> vr =
                dealValidationService.validateChangeStatus(shard, null, StatusDirect.ACTIVE);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(),
                        notNull()))));
    }

    @Test
    public void validateChangeStatus_nullDealId() {
        ValidationResult<List<Long>, Defect> vr =
                dealValidationService.validateChangeStatus(shard, singletonList(null), StatusDirect.ACTIVE);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        index(0)),
                        notNull()))));
    }

    @Test
    public void validateChangeStatus_invalidDealId() {
        long someDealId = 555L;
        ValidationResult<List<Long>, Defect> vr =
                dealValidationService.validateChangeStatus(shard, singletonList(someDealId), StatusDirect.ACTIVE);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        index(0)),
                        objectNotFound()))));
    }

    @Test
    public void validateChangeStatus_wrongStatus() {
        ValidationResult<List<Long>, Defect> vr =
                dealValidationService.validateChangeStatus(shard, singletonList(dealId), StatusDirect.ARCHIVED);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(
                        index(0)),
                        transitionIsUnavailable()))));
    }

    @Test
    public void validateUpdateDeal_error_whenNullElement() {
        ValidationResult<List<DealAdfox>, Defect> vr =
                dealValidationService.validateUpdateDealAdfox(singletonList(null));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(), notContainNulls()))));
    }

    @Test
    public void validateUpdateDealAdfox_error_whenIdIsNull() {
        DealAdfox existingDeal = existingDeals.get(dealId);
        existingDeal.setId(null);
        ValidationResult<List<DealAdfox>, Defect> vr =
                dealValidationService.validateUpdateDealAdfox(singletonList(existingDeal));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(index(0), field(DealAdfox.ID.name())),
                        notNull()))));
    }

    @Test
    public void validateUpdateDealAdfox_error_whenIdIsNotPositive() {
        DealAdfox existingDeal = existingDeals.get(dealId);
        existingDeal.setId(0L);
        ValidationResult<List<DealAdfox>, Defect> vr =
                dealValidationService.validateUpdateDealAdfox(singletonList(existingDeal));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(index(0), field(DealAdfox.ID.name())),
                        validId()))));
    }

    @Test
    public void validateUpdateDealAdfox_error_whenAdfoxStatusIsNull() {
        DealAdfox existingDeal = existingDeals.get(dealId);
        existingDeal.setAdfoxStatus(null);
        ValidationResult<List<DealAdfox>, Defect> vr =
                dealValidationService.validateUpdateDealAdfox(singletonList(existingDeal));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(index(0), field(DealAdfox.ADFOX_STATUS.name())),
                        notNull()))));
    }

    @Test
    public void validateUpdateDealAdfox_error_whenDealIsNotExists() {
        long absentDealId = 1_000_042L;
        DealAdfox nonExistingDeal = defaultDeal(absentDealId);
        ValidationResult<List<DealAdfox>, Defect> vr =
                dealValidationService.validateUpdateDealAdfox(singletonList(nonExistingDeal));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(index(0)),
                        objectNotFound()))));
    }

    @Test
    public void validateUpdateDealAdfox_error_whenMultipleParametersAreWrong() {
        DealAdfox correctDeal = existingDeals.get(dealId);
        DealAdfox nullFieldDeal = new DealAdfox();
        ValidationResult<List<DealAdfox>, Defect> vr =
                dealValidationService.validateUpdateDealAdfox(asList(correctDeal, nullFieldDeal));

        assertThat(vr.flattenErrors())
                .describedAs("Validation errors for filled good and one empty deal")
                .hasSize(2)
                .allSatisfy(defectInfo ->
                        assertThat(defectInfo.getPath().startsWith(path(index(1))))
                                .describedAs("Validation error should be only on second element")
                                .isTrue());
    }

    private Map<Long, StatusDirect> dealStatuses() {
        return EntryStream.of(existingDeals)
                .mapValues(DealBase::getDirectStatus)
                .toMap();
    }

    private Map<Long, DealBase> existingDeals() {
        Map<Long, DealBase> result = new HashMap<>();
        Deal deal = defaultDeal(dealId);
        result.put(deal.getId(), deal);

        return result;
    }

    private Deal defaultDeal(Long dealId) {
        Deal deal = new Deal();
        deal.withId(dealId)
                .withAdfoxStatus(StatusAdfox.ACTIVE)
                .withDirectStatus(StatusDirect.ACTIVE)
                .withCurrencyCode(defaultCurrencyCode);
        return deal;
    }

}
