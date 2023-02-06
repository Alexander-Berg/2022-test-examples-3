package ru.yandex.direct.core.entity.retargeting.service.validation2;


import java.util.List;

import com.google.common.collect.ImmutableList;

import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealPlacement;
import ru.yandex.direct.core.entity.placements.model.Placement;
import ru.yandex.direct.core.entity.placements.repository.PlacementsRepository;
import ru.yandex.direct.core.entity.retargeting.model.ConditionType;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.BIG_PLACEMENT_PAGE_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PRIVATE_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.PUBLIC_GOAL_ID;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.dcmPixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * Класс, предназначенный для создания данных для тестов,
 * проверяющих корректность валидации пикселей на баннерах
 * при обновлении ретаргетингов, от которых права на эти пиксели зависят
 */
public class CpmAdGroupCreator {

    /**
     * Создание сделки с размещениями вне сети яндекса
     */
    public static List<DealInfo> createDealWithNonYandexPlacements(Steps steps,
                                                                   PlacementsRepository placementsRepository,
                                                                   ClientInfo agencyInfo) {
        Placement placement = new Placement().withIsYandexPage(0L).withPageId(BIG_PLACEMENT_PAGE_ID + 1).withDomain("");
        placementsRepository.insertPlacements(singletonList(placement));
        DealPlacement dealPlacement = new DealPlacement().withPageId(placement.getPageId());
        Deal deal = TestDeals.defaultPrivateDeal(agencyInfo.getClientId());
        deal.withPlacements(singletonList(dealPlacement));
        List<Deal> deals = singletonList(deal);
        return steps.dealSteps().addDeals(deals, agencyInfo);
    }

    /**
     * Создание сделки с размещениями внутри сети яндекса
     */
    public static List<DealInfo> createDealWithYandexPlacements(Steps steps,
                                                                PlacementsRepository placementsRepository,
                                                                ClientInfo clientInfo) {
        Placement placement = new Placement().withIsYandexPage(1L).withPageId(BIG_PLACEMENT_PAGE_ID).withDomain("");
        placementsRepository.insertPlacements(singletonList(placement));
        DealPlacement dealPlacement = new DealPlacement().withPageId(placement.getPageId());
        Deal deal = TestDeals.defaultPrivateDeal(clientInfo.getClientId());
        deal.withPlacements(singletonList(dealPlacement));
        List<Deal> deals = singletonList(deal);
        return steps.dealSteps().addDeals(deals, clientInfo);
    }

    /**
     * Создание группы объявлений, привязанной к кампании-сделке, с чужим инвентарём
     * Создаётся также cpm-баннер с пикселем dcm, привязанный к данной группе
     */
    public static AdGroupInfo createCpmAdGroupWithForeignInventory(List<DealInfo> dealInfos,
                                                                   Steps steps,
                                                                   ClientInfo clientInfo) {
        CreativeInfo creativeInfo =
                steps.creativeSteps().createCreative(defaultCanvas(clientInfo.getClientId(), null), clientInfo);
        Long creativeId = creativeInfo.getCreativeId();

        CampaignInfo cpmDealCampaignInfo =
                steps.campaignSteps().createActiveCpmDealsCampaign(clientInfo);
        AdGroupInfo cpmAdGroupWithForeignInventoryInfo =
                steps.adGroupSteps().createActiveCpmBannerAdGroup(cpmDealCampaignInfo);
        mapList(dealInfos, DealInfo::getDealId).forEach(
                dealId -> steps.dealSteps().linkDealWithCampaign(dealId, cpmDealCampaignInfo.getCampaignId()));
        OldCpmBanner cpmBanner = activeCpmBanner(cpmAdGroupWithForeignInventoryInfo.getCampaignId(),
                cpmAdGroupWithForeignInventoryInfo.getAdGroupId(), creativeId)
                .withPixels(ImmutableList.of(dcmPixelUrl()));
        steps.bannerSteps().createActiveCpmBanner(cpmBanner, cpmAdGroupWithForeignInventoryInfo);

        return cpmAdGroupWithForeignInventoryInfo;
    }

    /**
     * Создаёт приватное условие ретаргетинга и возвращает его идентификатор
     */
    public static Long getPrivateRetConditionId(Steps steps, ClientInfo clientInfo) {
        return getPrivateRetConditionInfo(steps, clientInfo).getRetConditionId();
    }

    /**
     * Создаёт приватное условие ретаргетинга и возвращает RetConditionInfo
     */
    public static RetConditionInfo getPrivateRetConditionInfo(Steps steps, ClientInfo clientInfo) {
        Goal privateGoal = new Goal();
        privateGoal.withId(PRIVATE_GOAL_ID).withType(GoalType.SOCIAL_DEMO);
        return steps.retConditionSteps()
                .createDefaultRetCondition(ImmutableList.of(privateGoal), clientInfo, ConditionType.interests,
                        RuleType.OR);
    }

    /**
     * Создаёт публичное(пол/возраст) условие ретаргетинга и возвращает его идентификатор
     */
    public static Long getPublicRetConditionId(Steps steps, ClientInfo clientInfo) {
        return getPublicRetConditionInfo(steps, clientInfo).getRetConditionId();
    }

    /**
     * Создаёт публичное(пол/возраст) условие ретаргетинга и возвращает RetConditionInfo
     */
    public static RetConditionInfo getPublicRetConditionInfo(Steps steps, ClientInfo clientInfo) {
        Goal publicGoal = new Goal();
        publicGoal.withId(PUBLIC_GOAL_ID).withType(GoalType.SOCIAL_DEMO);
        return steps.retConditionSteps()
                .createDefaultRetCondition(ImmutableList.of(publicGoal), clientInfo, ConditionType.interests,
                        RuleType.OR);
    }
}
