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
 * Тест на дефолтное поведение статусов модерации баннера, группы и кампании в операции обновления.
 * текстовый баннер черновик
 * ModerationMode.DEFAULT
 */
@CoreTest
@RunWith(Parameterized.class)
public class DefaultBannerUpdateModerationDefaultModeDraftBannerTest extends UpdateModerationTestBase {

    @Parameterized.Parameters(name = "{4}: {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Обновление: Баннер черновик, Группа и кампания - черновики, изменяем баннер " +
                                "-> баннер остается в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        ModerationMode.DEFAULT,
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
                        "Обновление: Баннер черновик, Группа и кампания промодерированы, изменяем баннер " +
                                "-> баннер в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
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
                        "Обновление: Баннер черновик, Группа и кампания промодерированы, не изменяем баннер " +
                                "-> баннер в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NEW,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(null),
                        TextBanner.class
                },
                {
                        "Обновление: Баннер черновик, Группа - черновик, а кампания принята, изменяем баннер " +
                                "-> баннер остается в том же статусе, " +
                                "группа остается в том же статусе",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Баннер черновик, Группа отправлена на модерацию, а кампания принята, изменяем баннер " +
                                "-> баннер остается в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.SENT,
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
                draftTextBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                        .withTitle(TITLE), adGroupInfo).getBannerId();
    }
}
