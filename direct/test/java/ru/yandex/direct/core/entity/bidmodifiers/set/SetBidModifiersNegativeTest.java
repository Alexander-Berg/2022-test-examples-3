package ru.yandex.direct.core.entity.bidmodifiers.set;

import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds.GeneralDefects.ADJUSTMENT_NOT_FOUND;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds.GeneralDefects.DUPLICATE_SINGLE_ADJUSTMENT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.ARCHIVED_CAMPAIGN_MODIFICATION;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierDemographics;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.getModelChangesForUpdate;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@Description("Проверка негативных сценариев модификации корректировок ставок")
@RunWith(SpringJUnit4ClassRunner.class)
public class SetBidModifiersNegativeTest {
    private static final Integer NEW_PERCENT = DEFAULT_PERCENT / 2;
    private static final long NONEXISTENT_ID = 123456L;

    @Autowired
    private Steps steps;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    private CampaignInfo campaignInfo;
    private Long externalId;
    private Long id;

    @Before
    public void before() throws Exception {
        campaignInfo = steps.campaignSteps().createActiveTextCampaign();

        //Добавляем корректировку на компанию
        MassResult<List<Long>> result = bidModifierService.add(
                singletonList(createDefaultBidModifierMobile(campaignInfo.getCampaignId())),
                campaignInfo.getClientId(), campaignInfo.getUid());

        externalId = result.getResult().get(0).getResult().get(0);
        id = BidModifierService.getRealId(externalId);
    }

    @Test
    @Description("Несуществующая корректировка ставок")
    public void nonexistentIdTest() {
        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                NONEXISTENT_ID, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(singletonList(modelChanges), campaignInfo.getClientId(),
                campaignInfo.getUid());

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), ADJUSTMENT_NOT_FOUND)));
    }

    @Test
    @Description("Уже удаленная корректировка ставок")
    public void deletedIdTest() {
        bidModifierService.delete(singletonList(externalId), campaignInfo.getClientId(), campaignInfo.getUid());

        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                id, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(singletonList(modelChanges), campaignInfo.getClientId(),
                campaignInfo.getUid());

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), ADJUSTMENT_NOT_FOUND)));
    }

    @Test
    @Description("Чужая корректировка ставок")
    public void anotherCampaignTest() {
        CampaignInfo anotherCampaignInfo = steps.campaignSteps().createActiveTextCampaign();

        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                id, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(singletonList(modelChanges), anotherCampaignInfo.getClientId(),
                anotherCampaignInfo.getUid());

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), ADJUSTMENT_NOT_FOUND)));
    }

    @Test
    @Description("Корректировка ставки из архивной кампании")
    public void archivedCampaignTest() {
        testCampaignRepository.archiveCampaign(campaignInfo.getShard(), campaignInfo.getCampaignId());

        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                id, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(singletonList(modelChanges), campaignInfo.getClientId(),
                campaignInfo.getUid());

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), ARCHIVED_CAMPAIGN_MODIFICATION)));
    }

    @Test
    @Description("Корректировка ставки из кампании без права на запись")
    public void nonWritableCampaignTest() {
        var superReaderClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER);


        ModelChanges<BidModifierAdjustment> modelChanges = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                id, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(singletonList(modelChanges), campaignInfo.getClientId(),
                superReaderClientInfo.getUid());

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), CAMPAIGN_NO_WRITE_RIGHTS)));
    }

    @Test
    @Description("Изменение двух корректировок ставок, идентификатор одной из которых - некорректен")
    public void deleteMultipleBidModifiersOneInvalidTest() {
        ModelChanges<BidModifierAdjustment> modelChanges1 = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                id, NEW_PERCENT);

        ModelChanges<BidModifierAdjustment> modelChanges2 = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                NONEXISTENT_ID, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(asList(modelChanges1, modelChanges2),
                campaignInfo.getClientId(), campaignInfo.getUid());

        assertTrue(result.getResult().get(0).isSuccessful());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1)), ADJUSTMENT_NOT_FOUND)));

        List<BidModifier> items = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                asSet(campaignInfo.getCampaignId()), ALL_TYPES, ALL_LEVELS,
                campaignInfo.getUid());

        assertEquals("По найденной корректировке процент изменен", NEW_PERCENT,
                ((BidModifierMobile) items.get(0)).getMobileAdjustment().getPercent());
    }

    @Test
    @Description("Два одинаковых идентификатора корректировки ставок в запросе")
    public void sameIdsInRequestTest() {
        // Добавляем вторую корректировку
        MassResult<List<Long>> addResult = bidModifierService.add(
                singletonList(createDefaultBidModifierDemographics(campaignInfo.getCampaignId())),
                campaignInfo.getClientId(), campaignInfo.getUid());

        Long bmId2 = BidModifierService.getRealId(addResult.getResult().get(0).getResult().get(0));

        // Изменяем корректировки
        ModelChanges<BidModifierAdjustment> modelChanges1 = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                id, NEW_PERCENT);

        ModelChanges<BidModifierAdjustment> modelChanges2 = getModelChangesForUpdate(
                BidModifierDemographicsAdjustment.class, bmId2, NEW_PERCENT);

        MassResult<Long> result = bidModifierService.set(asList(modelChanges1, modelChanges2, modelChanges2),
                campaignInfo.getClientId(), campaignInfo.getUid());

        assertTrue(result.getResult().get(0).isSuccessful());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1)), DUPLICATE_SINGLE_ADJUSTMENT)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(2)), DUPLICATE_SINGLE_ADJUSTMENT)));

        List<BidModifier> items = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                asSet(campaignInfo.getCampaignId()), ALL_TYPES, ALL_LEVELS,
                campaignInfo.getUid())
                .stream()
                .sorted(Comparator.comparing(BidModifier::getId))
                .collect(toList());

        assertEquals("По корректировке без дублей процент изменен", NEW_PERCENT,
                ((BidModifierMobile) items.get(0)).getMobileAdjustment().getPercent());
        assertEquals("По корректировке с дублями процент не изменился", DEFAULT_PERCENT,
                ((BidModifierDemographics) items.get(1)).getDemographicsAdjustments().get(0).getPercent());
    }

    @Test
    @Description("Разные значения для одной корректировки ставок в запросе")
    public void differentValuesForBidModifierInRequestTest() {
        ModelChanges<BidModifierAdjustment> modelChanges1 = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                id, NEW_PERCENT);

        ModelChanges<BidModifierAdjustment> modelChanges2 = getModelChangesForUpdate(BidModifierMobileAdjustment.class,
                id, NEW_PERCENT + 1);

        MassResult<Long> result = bidModifierService.set(asList(modelChanges1, modelChanges2),
                campaignInfo.getClientId(), campaignInfo.getUid());

        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), DUPLICATE_SINGLE_ADJUSTMENT)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1)), DUPLICATE_SINGLE_ADJUSTMENT)));

        List<BidModifier> items = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                asSet(campaignInfo.getCampaignId()), ALL_TYPES, ALL_LEVELS,
                campaignInfo.getUid());

        assertEquals("По корректировке процент не изменился", DEFAULT_PERCENT,
                ((BidModifierMobile) items.get(0)).getMobileAdjustment().getPercent());
    }
}
