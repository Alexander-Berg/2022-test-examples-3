package ru.yandex.direct.core.entity.banner.type.creative.moderation;

import java.util.Collection;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class TextBannerWithCreativeModerationUpdateTest
        extends BannerWithCreativeModerationUpdatePositiveTestBase {

    @Autowired
    private TestModerationRepository moderationRepository;

    @Parameterized.Parameter(8)
    public boolean needRemoveModReason;

    @Parameterized.Parameters(name = "mode {4} : {0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "обновление текстового баннера: изменился креатив -> креатив переводится в статус READY",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        TextBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        modelChangesWithNewCreative(),
                        //исходный статус связанного объекта
                        BannerCreativeStatusModerate.YES,
                        // ожидаемые значения
                        BannerCreativeStatusModerate.READY,
                        true
                },
                {
                        "обновление текстового баннера: креатив не менялся -> статус не изменился",
                        // исходные значения
                        BannerStatusModerate.YES,
                        BannerStatusPostModerate.YES,
                        TextBanner.class,
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        nothingChanged(),
                        //исходный статус связанного объекта
                        BannerCreativeStatusModerate.YES,
                        // ожидаемые значения
                        BannerCreativeStatusModerate.YES,
                        false
                },
        });
    }

    @Override
    protected AbstractBannerInfo<OldTextBanner> createBanner() {
        BannerCreativeInfo<OldTextBanner> bannerCreativeInfo =
                steps.bannerCreativeSteps().createTextBannerCreative(defaultClient);
        AbstractBannerInfo<OldTextBanner> textBannerInfo = bannerCreativeInfo.getBannerInfo();
        addModerationData(textBannerInfo.getShard(), textBannerInfo.getBannerId());
        return textBannerInfo;
    }

    @Override
    protected void additionalChecks() {
        Long modReasonId = moderationRepository.getModReasonVideoAdditionByBannerId(bannerInfo.getShard(),
                bannerInfo.getBannerId());

        if (needRemoveModReason) {
            assertThat(modReasonId, nullValue());
        } else {
            assertThat(modReasonId, notNullValue());
        }
    }


    private void addModerationData(int shard, Long bannerId) {
        moderationRepository.addModReasonVideoAddition(shard, bannerId);
        moderationRepository.addModObjectVersionVideoAddition(shard, bannerId);
    }


    private static BannerWithCreativeModelChangesFunction modelChangesWithNewCreative() {
        return (steps, clientInfo, bannerClass, bannerId) -> {
            Long creativeId = steps.creativeSteps().getNextCreativeId();
            steps.creativeSteps().addDefaultVideoAdditionCreative(clientInfo, creativeId);
            return getModelChanges(bannerClass, bannerId, creativeId);
        };
    }

    private static BannerWithCreativeModelChangesFunction nothingChanged() {
        return (steps, adGroupInfo, bannerClass, bannerId) ->
                new ModelChanges<>(bannerId, bannerClass)
                        .castModelUp(BannerWithSystemFields.class);
    }
}
