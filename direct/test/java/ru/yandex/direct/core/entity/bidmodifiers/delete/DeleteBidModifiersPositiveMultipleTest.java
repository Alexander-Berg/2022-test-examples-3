package ru.yandex.direct.core.entity.bidmodifiers.delete;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static junit.framework.TestCase.assertTrue;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertFalse;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultGeoAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultVideoAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyGeoModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyMobileModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyRetargetingModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyVideoModifier;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка позитивных сценариев удаления нескольких корректировок ставок в одном запросе")
public class DeleteBidModifiersPositiveMultipleTest {

    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private BidModifierService bidModifierService;

    private CampaignInfo campaignInfo;

    private BidModifier mobile;
    private BidModifier demographic;
    private BidModifier twoDemographics;
    private BidModifier retargeting;
    private BidModifier regional;
    private BidModifier video;
    private Long adGroupId;
    private Long campaignId;

    @Before
    public void before() throws Exception {

        // Создаём кампанию и группу
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        campaignInfo = adGroupInfo.getCampaignInfo();
        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = campaignInfo.getCampaignId();

        //Создаем условие ретаргетинга
        RetConditionInfo retCondition = retConditionSteps.createDefaultRetCondition(campaignInfo.getClientInfo());
        long retCondId = retCondition.getRetConditionId();

        //Создаем корректировки
        mobile = createEmptyMobileModifier().withMobileAdjustment(createDefaultMobileAdjustment());
        demographic = createEmptyDemographicsModifier()
                .withDemographicsAdjustments(createDefaultDemographicsAdjustments());
        retargeting = createEmptyRetargetingModifier()
                .withRetargetingAdjustments(createDefaultRetargetingAdjustments(retCondId));
        regional = createEmptyGeoModifier().withRegionalAdjustments(createDefaultGeoAdjustments());
        video = createEmptyVideoModifier().withVideoAdjustment(createDefaultVideoAdjustment());

        twoDemographics = createEmptyDemographicsModifier().withDemographicsAdjustments(asList(
                createDefaultDemographicsAdjustment().withAge(AgeType._25_34).withPercent(110),
                createDefaultDemographicsAdjustment().withAge(AgeType._35_44).withPercent(110)
        ));
    }

    @Test
    @Description("Удаление корректировок разных типов на кампанию")
    public void deleteBidModifiersDifferentTypeCampaignTest() {
        List<BidModifier> bidModifiers = asList(
                mobile.withCampaignId(campaignId),
                demographic.withCampaignId(campaignId),
                retargeting.withCampaignId(campaignId),
                regional.withCampaignId(campaignId),
                video.withCampaignId(campaignId));

        addDeleteAndCheck(bidModifiers);
    }

    @Test
    @Description("Удаление корректировок разных типов на группу")
    public void deleteBidModifiersDifferentTypeAdGroupTest() {
        //Не учитываем геокорректировку, которая для групп не поддерживается
        List<BidModifier> bidModifiers = asList(
                mobile.withAdGroupId(adGroupId),
                demographic.withAdGroupId(adGroupId),
                retargeting.withAdGroupId(adGroupId),
                video.withAdGroupId(adGroupId));

        addDeleteAndCheck(bidModifiers);
    }

    @Test
    @Description("Удаление нескольких корректировок ставок одного типа на кампанию")
    public void deleteBidModifiersSameTypeTest() {
        List<BidModifier> bidModifiers = singletonList(twoDemographics.withCampaignId(campaignId));

        addDeleteAndCheck(bidModifiers);
    }


    private void addDeleteAndCheck(List<BidModifier> bidModifiers) {
        //Добавляем корректировки
        MassResult<List<Long>> addResult = bidModifierService.add(bidModifiers,
                campaignInfo.getClientId(), campaignInfo.getUid());

        List<Long> ids = addResult.getResult().stream()
                .flatMap(result -> result.getResult().stream())
                .collect(toList());

        //Удаляем корректировки
        MassResult<Long> result = bidModifierService.delete(ids, campaignInfo.getClientId(), campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        //Получаем корректировки по кампании для проверки
        List<BidModifier> items = bidModifierService.getByCampaignIds(
                campaignInfo.getClientId(), asSet(campaignInfo.getCampaignId()),
                ALL_TYPES, ALL_LEVELS, campaignInfo.getUid());

        assertTrue("Корректировки удалены", items.isEmpty());
    }
}
