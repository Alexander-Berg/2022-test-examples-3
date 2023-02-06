package ru.yandex.direct.core.entity.banner.type.moderation.add;

import java.util.Collection;

import one.util.streamex.StreamEx;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoEdaBanner;

/**
 * Тест на автомодерацию баннера продвижения Еды, и поведения статусов модерации
 * его группы и кампании в операции добавления.
 * saveDraft = true;
 */
@CoreTest
@RunWith(Parameterized.class)
public class ContentPromotionEdaBannerAddAutoModerationSaveDraftTest extends AddModerationTestBase {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        // Обычно тесты не должны зависеть друг от друга.
        // Но поведение баннера продвижения Еды не зависит от флага saveDraft,
        // поэтому мы переиспользуем одни и те же тест-кейсы для обоих значений флага.
        return StreamEx.of(ContentPromotionEdaBannerAddAutoModerationNoSaveDraftTest.parameters())
                .peek(params -> params[4] = SAVE_DRAFT_NO)
                .toList();
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        return steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.EDA);
    }

    @Override
    protected BannerWithAdGroupId getBannerForAddition() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.EDA);
        return clientContentPromoEdaBanner(content.getId());
    }
}
