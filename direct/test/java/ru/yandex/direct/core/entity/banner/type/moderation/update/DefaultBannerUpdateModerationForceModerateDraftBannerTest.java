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
import static ru.yandex.direct.core.testing.data.TestBanners.draftTextBanner;

/**
 * Тест на дефолтное поведение статусов модерации баннера, группы и кампании в операции добавления.
 * ModerationMode.FORCE_MODERATE, когда баннер черновик
 */
@CoreTest
@RunWith(Parameterized.class)
public class DefaultBannerUpdateModerationForceModerateDraftBannerTest extends UpdateModerationTestBase {

    @Parameterized.Parameters(name = "{4}: {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Обновление: Группа и кампания - черновики, изменяем баннер " +
                                "-> баннер остается в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        ModerationMode.FORCE_MODERATE,
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
                        "Обновление: Группа и кампания отправлены на модерацию, баннер изменился " +
                                "-> баннер становится готовым к отправке, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        // параметры операции
                        ModerationMode.FORCE_MODERATE,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
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
                        "Обновление: Группа и кампания отправлены на модерацию, баннер не изменился " +
                                "-> баннер становится готовым к отправке, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        // параметры операции
                        ModerationMode.FORCE_MODERATE,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.NO,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(null), // не изменяем
                        TextBanner.class
                },
                {
                        "Обновление: Группа и кампания промодерированы, изменяем баннер " +
                                "-> баннер становится готовым к отправке, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.FORCE_MODERATE,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
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
                        "Обновление: Группа и кампания промодерированы, не изменяем баннер " +
                                "-> становится готовым к отправке, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.FORCE_MODERATE,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.NO,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(null),
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
        //баннер черновик
        return steps.bannerSteps().createBanner(draftTextBanner().withTitle(TITLE), adGroupInfo).getBannerId();
    }
}
