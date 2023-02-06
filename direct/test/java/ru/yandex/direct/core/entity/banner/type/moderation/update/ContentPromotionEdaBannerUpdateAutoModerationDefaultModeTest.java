package ru.yandex.direct.core.entity.banner.type.moderation.update;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerEdaType;


@CoreTest
@RunWith(Parameterized.class)
public class ContentPromotionEdaBannerUpdateAutoModerationDefaultModeTest extends UpdateModerationTestBase {

    @Parameterized.Parameters(name = "{4}: {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Обновление: Группа и кампания - черновики -> группа и кампания становятся готовы к отправке",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.READY,
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
                        "Обновление: Группа и кампания отклонены -> группа и кампания становятся готовы к переотправке " +
                                "(statusPostModerate тоже сбрасывается)",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.REJECTED,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.READY,
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
                        "Обновление: Группа отклонена, а кампания принята -> группа становится готова к переотправке",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.REJECTED,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.TITLE,
                        newValue(NEW_TITLE),
                        ContentPromotionBanner.class,
                },
                {
                        "Обновление: Группа - черновик, а кампания принята -> группа становится готова к переотправке",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.TITLE,
                        newValue(NEW_TITLE),
                        ContentPromotionBanner.class,
                },
        });
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        return steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.EDA);
    }

    @Override
    protected Long createBanner(AdGroupInfo adGroupInfo) {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.EDA);
        return steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerEdaType(adGroupInfo.getAdGroupId(), adGroupInfo.getCampaignId())
                        .withTitle(TITLE)
                        .withContentPromotionId(content.getId()),
                adGroupInfo).getBannerId();
    }
}
