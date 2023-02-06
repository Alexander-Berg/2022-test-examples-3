package ru.yandex.direct.core.entity.banner.type.moderation.update;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

/**
 * Тест на дефолтное поведение статусов модерации баннера, группы и кампании в операции обновления.
 * текстовый отклоненный баннер
 * ModerationMode.DEFAULT
 */
@CoreTest
@RunWith(Parameterized.class)
public class DefaultBannerUpdateModerationDefaultModeDeclinedWithRejectedBannerTest extends UpdateModerationTestBase {

    @Parameterized.Parameters(name = "{4}: {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Обновление: Отклоненный баннер, Группа и кампания - черновики, изменяем баннер " +
                                "-> баннер остается в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.NO,
                        BannerStatusPostModerate.REJECTED,
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
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Отклоненный баннер, Группа и кампания отправлены на модерацию, баннер не изменился " +
                                "-> баннер остается в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.NO,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(null), // не изменяем
                        TextBanner.class
                },
                {
                        "Обновление: Отклоненный баннер, Группа и кампания промодерированы, изменяем баннер " +
                                "-> баннер становится готовым к отправке, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Отклоненный баннер, Группа и кампания промодерированы, не изменяем баннер " +
                                "-> баннер в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.NO,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(null),
                        TextBanner.class
                },
                {
                        "Обновление: Отклоненный баннер, Группа и кампания отклонены, изменяем баннер " +
                                "-> баннер становится готовым к отправке, " +
                                "группа и кампания становится готовыми к переотправке",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.READY,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Отклоненный баннер, Группа - черновик, а кампания принята, изменяем баннер " +
                                "-> баннер становится готовым к отправке, " +
                                "группа становится готовой к отправке",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Отклоненный баннер, Группа отправлена на модерацию, а кампания принята, изменяем баннер " +
                                "-> баннер становится готовым к отправке, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Отклоненный баннер, Группа отклонена, а кампания принята, изменяем баннер " +
                                "-> баннер становится готовым к отправке, " +
                                "группа становится готовой к отправке (statusPostModerate тоже сбрасывается)",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.REJECTED,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        TextBanner.TITLE,
                        newValue(NEW_TITLE),
                        TextBanner.class
                },
                {
                        "Обновление: Отклоненный баннер, Группа - черновик, а кампания отклонена, изменяем баннер" +
                                " -> баннер становится готовым к отправке, " +
                                "группа и кампания становится готовыми к переотправке",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        // ожидаемые значения
                        BannerStatusModerate.READY,
                        BannerStatusPostModerate.REJECTED,
                        StatusModerate.READY,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.READY,
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
                        .withTitle(TITLE)
                        .withStatusModerate(OldBannerStatusModerate.NO)
                        .withStatusPostModerate(OldBannerStatusPostModerate.REJECTED),
                adGroupInfo).getBannerId();
    }
}
