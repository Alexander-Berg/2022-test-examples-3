package ru.yandex.direct.core.entity.banner.type.moderation.add;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;

import static ru.yandex.direct.common.db.PpcPropertyNames.CONTENT_PROMOTION_AUTO_MODERATION;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;

/**
 * Тест на стандартную модерацию контент-промоушен-баннера (когда автомодерация выключена),
 * и поведения статусов модерации его группы и кампании в операции добавления.
 * saveDraft = true;
 */
@CoreTest
@RunWith(Parameterized.class)
public class ContentPromotionVideoBannerAddDefaultModerationSaveDraftTest extends AddModerationTestBase {

    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        // Обычно тесты не должны зависеть друг от друга.
        // Но когда автомодерация контент-промоушена выключена,
        // у него должно быть стандартное поведение, поэтому
        // мы переиспользуем тест-кейсы на стандартное поведение.
        return DefaultBannerAddModerationSaveDraftTest.parameters();
    }

    @Override
    protected AdGroupInfo createAdGroup() {
        ppcPropertiesSupport.set(CONTENT_PROMOTION_AUTO_MODERATION.getName(), "false");
        return steps.adGroupSteps().createDefaultContentPromotionAdGroup(ContentPromotionAdgroupType.VIDEO);
    }

    @Override
    protected BannerWithAdGroupId getBannerForAddition() {
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        return clientContentPromoBanner(content.getId());
    }
}
