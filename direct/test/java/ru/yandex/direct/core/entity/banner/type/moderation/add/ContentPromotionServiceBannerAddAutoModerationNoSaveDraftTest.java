package ru.yandex.direct.core.entity.banner.type.moderation.add;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoServiceBanner;

/**
 * Тест на автомодерацию баннера продвижения услуг, и поведения статусов модерации
 * его группы и кампании в операции добавления.
 * saveDraft = false;
 */
@CoreTest
@RunWith(Parameterized.class)
public class ContentPromotionServiceBannerAddAutoModerationNoSaveDraftTest extends AddModerationTestBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Группа и кампания - черновики -> группа и кампания становятся готовы к отправке",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.READY,
                },
                {
                        "Группа и кампания промодерированы -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                },
                {
                        "Группа и кампания отклонены -> группа и кампания становятся готовы к переотправке " +
                                "(statusPostModerate тоже сбрасывается)",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.REJECTED,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.READY,
                },
                {
                        "Группа и кампания отправлены на модерацию -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                },
                {
                        "Группа отклонена, а кампания принята -> группа становится готова к переотправке",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.REJECTED,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                },
                {
                        "Группа - черновик, а кампания принята -> группа становится готова к переотправке",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                },
        });
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        return steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.SERVICE);
    }

    @Override
    protected BannerWithAdGroupId getBannerForAddition() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.SERVICE);
        return clientContentPromoServiceBanner(content.getId());
    }
}
