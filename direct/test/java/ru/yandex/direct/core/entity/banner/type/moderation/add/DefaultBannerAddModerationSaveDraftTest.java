package ru.yandex.direct.core.entity.banner.type.moderation.add;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

/**
 * Тест на дефолтное поведение статусов модерации баннера, группы и кампании в операции добавления.
 * save_draft = true.
 */
@CoreTest
@RunWith(Parameterized.class)
public class DefaultBannerAddModerationSaveDraftTest extends AddModerationTestBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Группа и кампания - черновики -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.NEW,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NEW,
                },
                {
                        "Группа и кампания промодерированы -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                },
                {
                        "Группа и кампания отклонены -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.NO,
                },
                {
                        "Группа и кампания отправлены на модерацию -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.SENT,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.SENT,
                },
                {
                        "Группа отклонена, а кампания принята -> баннер становится черновиком, " +
                                "группа и кампания остаются в том же статусе",
                        // исходные значения
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                        // параметры операции
                        SAVE_DRAFT_YES,
                        // ожидаемые значения
                        BannerStatusModerate.NEW,
                        BannerStatusPostModerate.NO,
                        StatusModerate.NO,
                        StatusPostModerate.NO,
                        CampaignStatusModerate.YES,
                },
        });
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        return steps.adGroupSteps().createActiveTextAdGroup();
    }

    @Override
    protected BannerWithAdGroupId getBannerForAddition() {
        return clientTextBanner();
    }
}
