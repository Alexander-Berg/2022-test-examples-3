package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.index;

public class RestrictedCampaignsUpdateOperationValidationTest {

    private Long operatorUid;
    private UidClientIdShard uidClientIdShard;
    List<TextCampaign> campaigns;
    List<ModelChanges<TextCampaign>> modelChanges;

    private RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation;
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;

    @Before
    public void before() {
        operatorUid = 11L;
        uidClientIdShard = UidClientIdShard.of(11L, 12L, 1);
        campaigns = List.of(new TextCampaign().withId(1L), new TextCampaign().withId(2L));
        Set<Long> campaignIds = listToSet(campaigns, TextCampaign::getId);

        modelChanges = new ArrayList<>();
        for (TextCampaign campaign : campaigns) {
            modelChanges.add(new ModelChanges<>(campaign.getId(), TextCampaign.class));
        }

        var strategyTypedRepository = mock(StrategyTypedRepository.class);
        var campaignModifyRepository = mock(CampaignModifyRepository.class);
        var campaignUpdateOperationSupportFacade = mock(CampaignUpdateOperationSupportFacade.class);
        var campaignAdditionalActionsService = mock(CampaignAdditionalActionsService.class);
        var ppcDslContextProvider = mock(DslContextProvider.class);
        var rbacService = mock(RbacService.class);
        var campaignTypedRepository = mock(CampaignTypedRepository.class);
        var metrikaClientFactory = mock(RequestBasedMetrikaClientFactory.class);
        var featureService = mock(FeatureService.class);
        updateRestrictedCampaignValidationService = mock(UpdateRestrictedCampaignValidationService.class);

        when(metrikaClientFactory.createMetrikaClient(any()))
                .thenReturn(mock(RequestBasedMetrikaClientAdapter.class));
        when(campaignTypedRepository.getClientCampaignIds(eq(uidClientIdShard.getShard()),
                eq(uidClientIdShard.getClientId()), anyCollection()))
                .thenReturn(campaignIds);

        doReturn(campaigns)
                .when(campaignTypedRepository)
                .getTypedCampaigns(eq(uidClientIdShard.getShard()), anyCollection());

        var options = new CampaignOptions();
        restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                modelChanges,
                operatorUid,
                uidClientIdShard,
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService, campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService, ppcDslContextProvider, rbacService, metrikaClientFactory,
                featureService, Applicability.PARTIAL, options);
    }

    @Test
    public void testAppliedChanges_bothCampaignsAreValid() {
        mockValidationResult(List.of());

        restrictedCampaignsUpdateOperation.apply();
        List<BaseCampaign> validAppliedChangesModels = mapList(captureAppliedChanges().values(), AppliedChanges::getModel);

        assertThat("размер моделей из валидных AppliedChanges должен совпадать с кол-вом кампаний",
                validAppliedChangesModels, hasSize(campaigns.size()));

        assertThat("модели из валидных AppliedChanges должны содержать все кампании",
                validAppliedChangesModels,
                containsInAnyOrder(mapList(campaigns, Matchers::sameInstance)));
    }

    @Test
    public void testAppliedChanges_firstInvalid_secondValid() {
        mockValidationResult(List.of(0));

        restrictedCampaignsUpdateOperation.apply();
        Map<Integer, AppliedChanges<BaseCampaign>> validAppliedChangesWithIndex = captureAppliedChanges();

        assertThat("мапа валидных AppliedChanges должна содержать 1 элемент",
                validAppliedChangesWithIndex.values(), hasSize(1));

        assertThat("мапа валидных AppliedChanges c индексами должна содержать запись для второй кампании",
                validAppliedChangesWithIndex,
                hasEntry(equalTo(1), hasProperty("model", sameInstance(campaigns.get(1)))));
    }

    private void mockValidationResult(List<Integer> invalidCampaignIndexes) {
        var mcValidationResult = new ValidationResult<>(modelChanges);
        var validationResult = new ValidationResult<>(campaigns);
        for (int i : invalidCampaignIndexes) {
            var mcSubResult = mcValidationResult.getOrCreateSubValidationResult(index(i), modelChanges.get(i));
            mcSubResult.addError(invalidValue());
            var subResult = validationResult.getOrCreateSubValidationResult(index(0), campaigns.get(i));
            subResult.addError(invalidValue());
        }

        doReturn(mcValidationResult)
                .when(updateRestrictedCampaignValidationService)
                .preValidate(any(), anyList(), anySet());

        doReturn(mcValidationResult)
                .when(updateRestrictedCampaignValidationService)
                .validateBeforeApply(any(), any(), anyMap());

        doReturn(validationResult)
                .when(updateRestrictedCampaignValidationService)
                .validate(any(), any(), anyMap());
    }

    private Map<Integer, AppliedChanges<BaseCampaign>> captureAppliedChanges() {
        ArgumentCaptor<Map<Integer, AppliedChanges<BaseCampaign>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(updateRestrictedCampaignValidationService).validate(any(), any(), captor.capture());
        return captor.getValue();
    }
}
