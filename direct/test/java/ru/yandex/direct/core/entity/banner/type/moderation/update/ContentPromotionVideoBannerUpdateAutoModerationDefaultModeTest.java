package ru.yandex.direct.core.entity.banner.type.moderation.update;

import java.util.Collection;

import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.common.db.PpcPropertyNames.CONTENT_PROMOTION_AUTO_MODERATION;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerVideoType;

/**
 * Тест на автомодерацию баннера продвижения услуг, и поведения статусов модерации
 * его группы и кампании в операции обновления.
 * ModerationMode.FORCE_SAVE_DRAFT;
 */
@CoreTest
@RunWith(Parameterized.class)
public class ContentPromotionVideoBannerUpdateAutoModerationDefaultModeTest extends UpdateModerationTestBase {

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Parameterized.Parameters(name = "{4}: {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Обновление: Группа и кампания - черновики -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.TITLE,
                        newValue(NEW_TITLE),
                        ContentPromotionBanner.class,
                },
                {
                        "Обновление: Группа и кампания промодерированы -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.TITLE,
                        newValue(NEW_TITLE),
                        ContentPromotionBanner.class,
                },
                {
                        "Обновление: Группа и кампания отклонены -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.TITLE,
                        newValue(NEW_TITLE),
                        ContentPromotionBanner.class,
                },
                {
                        "Обновление: Группа и кампания отправлены на модерацию -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.TITLE,
                        newValue(NEW_TITLE),
                        ContentPromotionBanner.class,
                },
                {
                        "Обновление: Группа отклонена, а кампания принята -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.TITLE,
                        newValue(NEW_TITLE),
                        ContentPromotionBanner.class,
                },
        });
    }

    @After
    public void tearDown() {
        ppcPropertiesSupport.remove(CONTENT_PROMOTION_AUTO_MODERATION.getName());
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        ppcPropertiesSupport.set(CONTENT_PROMOTION_AUTO_MODERATION.getName(), "true");
        return steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO);
    }

    @Override
    protected Long createBanner(AdGroupInfo adGroupInfo) {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        return steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerVideoType(adGroupInfo.getAdGroupId(), adGroupInfo.getCampaignId())
                        .withTitle(TITLE)
                        .withContentPromotionId(content.getId()),
                adGroupInfo).getBannerId();
    }
}
