package ru.yandex.direct.core.entity.bidmodifiers.set;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRegionalAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.typesupport.BidModifierTypeSupportDispatcher;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.junit.Assert.assertFalse;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.DEFAULT_PERCENT;
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
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.regions.Region.NIZHNY_NOVGOROD_OBLAST_REGION_ID;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка позитивных сценариев удаления нескольких корректировок ставок в одном запросе")
public class SetBidModifiersPositiveMultipleTest {

    private static final Integer NEW_PERCENT = DEFAULT_PERCENT / 2;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private BidModifierTypeSupportDispatcher bidModifierTypeSupportDispatcher;

    private CampaignInfo campaignInfo;

    private BidModifier mobile;
    private BidModifier demographic;
    private BidModifier retargeting;
    private BidModifier regional;
    private BidModifier video;
    private BidModifier twoDemographics;
    private BidModifier twoRegionals;
    private Long adGroupId;
    private Long campaignId;
    private ClientId clientId;
    private Long uid;

    @Before
    public void before() throws Exception {
        // Создаём кампанию и группу
        AdGroupInfo adGroupInfo = adGroupSteps.createActiveTextAdGroup();
        campaignInfo = adGroupInfo.getCampaignInfo();
        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = campaignInfo.getCampaignId();
        clientId = campaignInfo.getClientId();
        uid = campaignInfo.getUid();

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

        twoDemographics = createEmptyDemographicsModifier().withCampaignId(campaignId)
                .withDemographicsAdjustments(asList(
                        new BidModifierDemographicsAdjustment().withAge(AgeType._25_34).withPercent(DEFAULT_PERCENT),
                        new BidModifierDemographicsAdjustment().withAge(AgeType._35_44).withPercent(DEFAULT_PERCENT)
                ));

        twoRegionals = createEmptyGeoModifier().withCampaignId(campaignId).withRegionalAdjustments(asList(
                new BidModifierRegionalAdjustment().withRegionId(MOSCOW_REGION_ID)
                        .withHidden(false).withPercent(DEFAULT_PERCENT),
                new BidModifierRegionalAdjustment().withRegionId(NIZHNY_NOVGOROD_OBLAST_REGION_ID)
                        .withHidden(false).withPercent(DEFAULT_PERCENT)
        ));
    }

    private void test(List<BidModifier> bidModifiers) {
        //Добавляем корректировки
        MassResult<List<Long>> addResult = bidModifierService.add(bidModifiers, clientId, uid);

        //Получаем идентификаторы
        List<Long> externalIds = addResult.getResult().stream()
                .flatMap(result2 -> result2.getResult().stream())
                .collect(toList());

        // Создаем модели изменений
        List<ModelChanges<BidModifierAdjustment>> modelChangesList = new ArrayList<>();
        for (Long externalId : externalIds) {
            Long realId = BidModifierService.getRealId(externalId);
            BidModifierType type = BidModifierService.getRealType(externalId);

            Class<BidModifierAdjustment> adjustmentClass =
                    bidModifierTypeSupportDispatcher.getTypeSupport(type).getAdjustmentClass();
            ModelChanges<BidModifierAdjustment> modelChanges = new ModelChanges<>(realId, adjustmentClass)
                    .castModelUp(BidModifierAdjustment.class);
            modelChanges.process(NEW_PERCENT, BidModifierAdjustment.PERCENT);
            modelChangesList.add(modelChanges);
        }

        // Изменяем корректировки
        MassResult<Long> result = bidModifierService.set(modelChangesList, campaignInfo.getClientId(),
                campaignInfo.getUid());

        assertFalse("Результат не должен содержать ошибок валидации", result.getValidationResult().hasAnyErrors());

        //Получаем корректировки по кампании для проверки
        List<BidModifier> items = bidModifierService.getByCampaignIds(campaignInfo.getClientId(),
                asSet(campaignInfo.getCampaignId()), ALL_TYPES, ALL_LEVELS,
                campaignInfo.getUid());

        // Проверяем процент
        SoftAssertions.assertSoftly(softly -> {
            for (BidModifier item : items) {
                for (BidModifierAdjustment adjustment :
                        bidModifierTypeSupportDispatcher.getTypeSupport(item.getType()).getAdjustments(item)) {
                    softly.assertThat(adjustment.getPercent())
                            .describedAs("Element %s %s", item, adjustment)
                            .isEqualTo(NEW_PERCENT);
                }
            }

        });
    }

    @Test
    @Description("Модификация корректировок разных типов на кампанию")
    public void setBidModifiersDifferentTypeCampaignTest() {
        List<BidModifier> items = asList(
                mobile.withCampaignId(campaignId),
                demographic.withCampaignId(campaignId),
                retargeting.withCampaignId(campaignId),
                regional.withCampaignId(campaignId),
                video.withCampaignId(campaignId));

        test(items);
    }

    @Test
    @Description("Модификация корректировок разных типов на группу")
    public void setBidModifiersDifferentTypeAdGroupTest() {
        //Не учитываем геокорректировку, которая для групп не поддерживается
        List<BidModifier> items = asList(
                mobile.withAdGroupId(adGroupId),
                demographic.withAdGroupId(adGroupId),
                retargeting.withAdGroupId(adGroupId),
                video.withAdGroupId(adGroupId));

        test(items);
    }

    @Test
    @Description("Модификация нескольких демографических корректировок ставок")
    public void setBidModifiersSameDemographicTypeTest() {
        List<BidModifier> items = singletonList(twoDemographics.withCampaignId(campaignId));
        test(items);
    }

    @Test
    @Description("Модификация нескольких корректировок ставок по региону")
    public void setBidModifiersSameRegionalTypeTest() {
        List<BidModifier> items = singletonList(twoRegionals.withCampaignId(campaignId));
        test(items);
    }
}
