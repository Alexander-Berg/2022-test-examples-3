package ru.yandex.direct.core.entity.banner.type.moderation.update;

import java.util.Collection;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Arrays.asList;
import static ru.yandex.direct.common.db.PpcPropertyNames.CONTENT_PROMOTION_AUTO_MODERATION;
import static ru.yandex.direct.core.testing.data.TestBanners.activeContentPromotionBannerVideoType;
import static ru.yandex.direct.core.testing.data.TestContentPromotionCommonData.defaultContentPromotion;

/**
 * Тест на модерацию полей баннера продвижения услуг, и поведения статусов модерации
 * его группы и кампании в операции обновления.
 * ModerationMode.DEFAULT;
 */
@CoreTest
@RunWith(Parameterized.class)
public class ContentPromotionVideoBannerUpdateNoAutoModerationDefaultModeTest extends UpdateModerationTestBase {

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Parameterized.Parameters(name = "{4}: {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Обновление: Активный баннер, Группа и кампания промодерированы, не изменяем баннер " +
                                "-> баннер в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
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
                        newValue(null),
                        ContentPromotionBanner.class
                },
                {
                        "Обновление: Активный баннер, Группа и кампания промодерированы, изменяем title " +
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
                        BannerStatusPostModerate.NO,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.TITLE,
                        newValue(NEW_TITLE),
                        ContentPromotionBanner.class
                },
                {
                        "Обновление: Активный баннер, Группа и кампания промодерированы, изменяем body " +
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
                        BannerStatusPostModerate.NO,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.BODY,
                        newValue("new body"),
                        ContentPromotionBanner.class
                },
                {
                        "Обновление: Активный баннер, Группа и кампания промодерированы, изменяем contentPromotionId " +
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
                        BannerStatusPostModerate.NO,
                        StatusModerate.YES,
                        StatusPostModerate.YES,
                        CampaignStatusModerate.YES,
                        //modelchange передаваемый в операцию
                        ContentPromotionBanner.CONTENT_PROMOTION_ID,
                        newContentPromotionValue(),
                        ContentPromotionBanner.class
                },
                {
                        "Обновление: Активный баннер, Группа и кампания промодерированы, изменяем language " +
                                "-> баннер остается в том же статусе, " +
                                "группа и кампания остаются в том же статусе",
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
                        ContentPromotionBanner.FLAGS,
                        newValue(new BannerFlags().with(BannerFlags.AGE, Age.AGE_0)),
                        ContentPromotionBanner.class
                },
        });
    }

    @Before
    public void setUp() {
        ppcPropertiesSupport.remove(CONTENT_PROMOTION_AUTO_MODERATION.getName());
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        return steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO);
    }

    @Override
    protected Long createBanner(AdGroupInfo adGroupInfo) {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        return steps.bannerSteps().createActiveContentPromotionBanner(
                activeContentPromotionBannerVideoType(adGroupInfo.getAdGroupId(), adGroupInfo.getCampaignId())
                        .withTitle(TITLE)
                        .withBody("body")
                        .withFlags(null)
                        .withContentPromotionId(content.getId()),
                adGroupInfo).getBannerId();
    }

    private static BiFunction<Steps, AdGroupInfo, Object> newContentPromotionValue() {
       return (steps, adGroupInf) -> steps.contentPromotionSteps()
               .createContentPromotionContent(adGroupInf.getClientId(),
                       defaultContentPromotion(adGroupInf.getClientId(), ContentPromotionContentType.VIDEO)
                               .withExternalId("newContent")).getId();
    }
}
