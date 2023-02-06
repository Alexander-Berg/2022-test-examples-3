package ru.yandex.direct.core.entity.banner.type.moderation.add;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
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
import static ru.yandex.direct.common.db.PpcPropertyNames.CONTENT_PROMOTION_AUTO_MODERATION;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;

/**
 * Тест на автомодерацию контент-промоушен-баннера, и поведения статусов модерации
 * его группы и кампании в операции добавления.
 * saveDraft = false;
 */
@CoreTest
@RunWith(Parameterized.class)
public class ContentPromotionVideoBannerAddAutoModerationNoSaveDraftTest extends AddModerationTestBase {

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Группа и кампания - черновики -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        SAVE_DRAFT_NO,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                },
                {
                        "Группа и кампания отправлены на модерацию -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        // параметры операции
                        SAVE_DRAFT_NO,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                },
                {
                        "Группа и кампания промодерированы -> группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_NO,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                },
                {
                        "Группа и кампания отклонены -> группа и кампания становятся готовыми к переотправке",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        SAVE_DRAFT_NO,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.READY,
                },
                {
                        "Группа - черновик, а кампания принята -> группа становится готовой к отправке",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_NO,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                },
                {
                        "Группа отправлена на модерацию, а кампания принята -> " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_NO,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                },
                {
                        "Группа отклонена, а кампания принята -> " +
                                "группа становится готовой к отправке (statusPostModerate тоже сбрасывается)",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.REJECTED,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_NO,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                },
                {
                        "Группа - черновик, а кампания отклонена -> " +
                                "группа и кампания становится готовыми к переотправке",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        SAVE_DRAFT_NO,
                        // ожидаемые значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.READY,
                },
        });
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        ppcPropertiesSupport.set(CONTENT_PROMOTION_AUTO_MODERATION.getName(), "true");
        return steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO);
    }

    @Override
    protected BannerWithAdGroupId getBannerForAddition() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        return clientContentPromoBanner(content.getId());
    }
}
