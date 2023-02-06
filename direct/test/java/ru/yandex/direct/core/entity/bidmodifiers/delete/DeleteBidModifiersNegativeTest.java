package ru.yandex.direct.core.entity.bidmodifiers.delete;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getExternalId;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds.GeneralDefects.DUPLICATE_ADJUSTMENT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NO_WRITE_RIGHTS;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyMobileModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.DefectIds.CAMPAIGN_STATUS_ARCHIVED;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID;
import static ru.yandex.direct.validation.result.DefectIds.OBJECT_NOT_FOUND;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@Description("Проверка негативных сценариев удаления корректировок ставок")
@RunWith(SpringJUnit4ClassRunner.class)
public class DeleteBidModifiersNegativeTest {
    private static final long ZERO_ID = 0L;
    private static final long NEGATIVE_ID = -1L;
    private static final long NONEXISTENT_ID = 123456L;

    @Autowired
    private Steps steps;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    private CampaignInfo campaignInfo;
    private Long bmId1;
    private Long bmId2;

    @Before
    public void before() throws Exception {
        campaignInfo = steps.campaignSteps().createActiveTextCampaign();

        //Добавляем две корректировки на компанию
        BidModifier bidModifierDemographicItem = createEmptyDemographicsModifier()
                .withDemographicsAdjustments(createDefaultDemographicsAdjustments())
                .withCampaignId(campaignInfo.getCampaignId());
        BidModifier bidModifierMobileItem = createEmptyMobileModifier()
                .withMobileAdjustment(createDefaultMobileAdjustment())
                .withCampaignId(campaignInfo.getCampaignId());
        MassResult<List<Long>> result
                = bidModifierService.add(asList(bidModifierMobileItem, bidModifierDemographicItem),
                campaignInfo.getClientId(), campaignInfo.getUid());

        bmId1 = result.getResult().get(0).getResult().get(0);
        bmId2 = result.getResult().get(1).getResult().get(0);
    }

    @Test
    @Description("Нулевой ID корректировки")
    public void zeroIdTest() {
        MassResult<Long> result
                = bidModifierService.delete(singletonList(ZERO_ID), campaignInfo.getClientId(), campaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), MUST_BE_VALID_ID)));
    }

    @Test
    @Description("Отрицательный ID корректировки")
    public void negativeIdTest() {
        MassResult<Long> result = bidModifierService.delete(singletonList(NEGATIVE_ID),
                campaignInfo.getClientId(), campaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), MUST_BE_VALID_ID)));
    }

    @Test
    @Description("Несуществующая корректировка ставок")
    public void nonexistentIdTest() {
        MassResult<Long> result = bidModifierService.delete(singletonList(NONEXISTENT_ID),
                campaignInfo.getClientId(), campaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), OBJECT_NOT_FOUND)));
    }

    @Test
    @Description("Уже удаленная корректировка ставок")
    public void deletedIdTest() {
        bidModifierService.delete(singletonList(bmId1), campaignInfo.getClientId(), campaignInfo.getUid());
        MassResult<Long> result
                = bidModifierService.delete(singletonList(bmId1), campaignInfo.getClientId(), campaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), OBJECT_NOT_FOUND)));

    }

    @Test
    @Description("Чужая корректировка ставок")
    public void anotherCampaignTest() {
        CampaignInfo anotherCampaignInfo = steps.campaignSteps().createActiveTextCampaign();

        MassResult<Long> result = bidModifierService.delete(singletonList(bmId1),
                anotherCampaignInfo.getClientId(), anotherCampaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), OBJECT_NOT_FOUND)));
    }

    @Test
    @Description("Корректировка ставки из архивной кампании")
    public void archivedCampaignTest() {
        testCampaignRepository.archiveCampaign(campaignInfo.getShard(), campaignInfo.getCampaignId());

        MassResult<Long> result
                = bidModifierService.delete(singletonList(bmId1), campaignInfo.getClientId(), campaignInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), CAMPAIGN_STATUS_ARCHIVED)));
    }

    @Test
    @Description("Корректировка ставки из кампании без права на запись")
    public void nonWritableCampaignTest() {
        var superReaderClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPERREADER);

        MassResult<Long> result
                = bidModifierService.delete(singletonList(bmId1), campaignInfo.getClientId(),
                superReaderClientInfo.getUid());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0)), CAMPAIGN_NO_WRITE_RIGHTS)));
    }

    @Test
    @Description("Удаление двух корректировок ставок, идентификатор одной из которых - некорректен")
    public void deleteMultipleBidModifiersOneInvalidTest() {
        MassResult<Long> result = bidModifierService
                .delete(asList(bmId1, NONEXISTENT_ID), campaignInfo.getClientId(), campaignInfo.getUid());

        assertTrue(result.getResult().get(0).isSuccessful());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1)), OBJECT_NOT_FOUND)));
    }

    @Test
    @Description("Два одинаковых идентификатора корректировки ставок в запросе")
    public void sameIdsInRequestTest() {

        MassResult<Long> result = bidModifierService
                .delete(asList(bmId1, bmId2, bmId2), campaignInfo.getClientId(), campaignInfo.getUid());

        assertTrue(result.getResult().get(0).isSuccessful());
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1)), DUPLICATE_ADJUSTMENT)));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(2)), DUPLICATE_ADJUSTMENT)));

        List<BidModifier> items = bidModifierService.getByCampaignIds(
                campaignInfo.getClientId(), asSet(campaignInfo.getCampaignId()),
                ALL_TYPES, ALL_LEVELS, campaignInfo.getUid());

        Long realBmId2 = ((BidModifierDemographics) items.get(0)).getDemographicsAdjustments().get(0).getId();

        assertEquals("Корректировка спровоцировавшая ошибку не удалена", singletonList(
                getExternalId(realBmId2, BidModifierType.DEMOGRAPHY_MULTIPLIER)), singletonList(bmId2));
    }
}
