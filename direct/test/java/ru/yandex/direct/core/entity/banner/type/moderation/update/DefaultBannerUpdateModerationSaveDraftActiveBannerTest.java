package ru.yandex.direct.core.entity.banner.type.moderation.update;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

/**
 * Тест на дефолтное поведение статусов модерации баннера, группы и кампании в операции обновления.
 * ModerationMode = force_save_draft.
 */
@CoreTest
@RunWith(Parameterized.class)
public class DefaultBannerUpdateModerationSaveDraftActiveBannerTest extends UpdateModerationTestBase {

    @Parameterized.Parameters(name = "{4}: {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Обновление: Группа и кампания - черновики -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        ModerationMode.FORCE_SAVE_DRAFT,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Группа и кампания промодерированы -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.FORCE_SAVE_DRAFT,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Группа и кампания отклонены -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        ModerationMode.FORCE_SAVE_DRAFT,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Группа и кампания отправлены на модерацию -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        // параметры операции
                        ModerationMode.FORCE_SAVE_DRAFT,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Группа отклонена, а кампания принята -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.FORCE_SAVE_DRAFT,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
        });
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        return steps.adGroupSteps().createActiveTextAdGroup();
    }

    @Override
    protected Long createBanner(AdGroupInfo adGroupInfo) {
        return steps.bannerSteps().createBanner(
                activeTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withTitle(TITLE),
                adGroupInfo).getBannerId();
    }
}
