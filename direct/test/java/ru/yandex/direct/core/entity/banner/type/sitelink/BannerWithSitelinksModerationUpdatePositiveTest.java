package ru.yandex.direct.core.entity.banner.type.sitelink;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSitelinks;
import ru.yandex.direct.core.entity.banner.model.BannerWithSitelinksModeration;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithSitelinks;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.draftTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithSitelinksModerationUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithSitelinks> {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public TestModerationRepository testModerationRepository;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerProvider bannerProvider;

    @Parameterized.Parameter(2)
    public ModerationMode moderationMode;

    @Parameterized.Parameter(3)
    public BannerModelChangesProvider modelChangesProvider;

    @Parameterized.Parameter(4)
    public BannerStatusSitelinksModerate expectedStatusSitelinksModerate;

    @Parameterized.Parameter(5)
    public Boolean expectedClearModerationData;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "нет изменений, дефолтный режим модерации " +
                                "-> статус модерации сайтлинков не меняется",
                        // исходный баннер
                        bannerWithSitelinks(activeTextBanner()
                                .withStatusSitelinksModerate(StatusSitelinksModerate.YES)),
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        emptyModelChanges(),
                        // ожидаемые значения
                        BannerStatusSitelinksModerate.YES,
                        false
                },
                {
                        "добавили сайтлинки в new баннер " +
                                "-> статус модерации устанавливается в new",
                        // исходный баннер
                        bannerWithoutSitelinks(activeTextBanner()
                                .withStatusModerate(OldBannerStatusModerate.NEW)),
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        newSitelinkModelChanges(),
                        // ожидаемые значения
                        BannerStatusSitelinksModerate.NEW,
                        false
                },
                {
                        "добавили сайтлинки в ready баннер " +
                                "-> статус модерации устанавливается в ready",
                        // исходный баннер
                        bannerWithoutSitelinks(activeTextBanner()),
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        newSitelinkModelChanges(),
                        // ожидаемые значения
                        BannerStatusSitelinksModerate.READY,
                        false
                },
                {
                        "удалили сайтлинки " +
                                "-> статус модерации сбрасывается в new, удалили данные модерации",
                        // исходный баннер
                        bannerWithSitelinks(activeTextBanner()
                                .withStatusSitelinksModerate(StatusSitelinksModerate.YES)),
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        deleteSitelinkModelChanges(),
                        // ожидаемые значения
                        BannerStatusSitelinksModerate.NEW,
                        true
                },
                {
                        "режим сохранения в черновики " +
                                "-> сбросили статус модерации, удалили данные модерации",
                        // исходный баннер
                        bannerWithSitelinks(activeTextBanner()
                                .withStatusSitelinksModerate(StatusSitelinksModerate.YES)),
                        // параметры операции
                        ModerationMode.FORCE_SAVE_DRAFT,
                        //modelchange
                        newSitelinkModelChanges(),
                        // ожидаемые значения
                        BannerStatusSitelinksModerate.NEW,
                        true
                },
                {
                        "сайтлинки отклонили, режим принудительной отправки в модерацию " +
                                "-> переотправляем сайтлинки в модерацию",
                        // исходный баннер
                        bannerWithSitelinks(activeTextBanner()
                                .withStatusSitelinksModerate(StatusSitelinksModerate.NO)),
                        // параметры операции
                        ModerationMode.FORCE_MODERATE,
                        //modelchange
                        emptyModelChanges(),
                        // ожидаемые значения
                        BannerStatusSitelinksModerate.READY,
                        false
                },
                {
                        "баннер без сайтлинков, режим принудительной отправки в модерацию " +
                                "-> статус модерации всегда new",
                        // исходный баннер
                        bannerWithoutSitelinks(draftTextBanner()),
                        // параметры операции
                        ModerationMode.FORCE_MODERATE,
                        //modelchange
                        emptyModelChanges(),
                        // ожидаемые значения
                        BannerStatusSitelinksModerate.NEW,
                        false
                },
                {
                        "сайтлинки отклонили, есть другие изменения " +
                                "-> переотправляем сайтлинки в модерацию",
                        // исходный баннер
                        bannerWithSitelinks(activeTextBanner()
                                .withStatusSitelinksModerate(StatusSitelinksModerate.NO)),
                        // параметры операции
                        ModerationMode.DEFAULT,
                        //modelchange
                        titleModelChanges(),
                        // ожидаемые значения
                        BannerStatusSitelinksModerate.READY,
                        false
                },
        });
    }

    @Test
    public void test() {
        bannerInfo = bannerProvider.createBanner(steps);
        testModerationRepository.addModReasonSitelinkSet(bannerInfo.getShard(), bannerInfo.getBannerId());

        ModelChanges<BannerWithSystemFields> modelChanges =
                modelChangesProvider.getModelChanges(steps, bannerInfo);
        prepareAndApplyValid(modelChanges);

        BannerWithSitelinksModeration actualBanner =
                getBanner(bannerInfo.getBannerId(), BannerWithSitelinksModeration.class);
        assertThat(actualBanner.getStatusSitelinksModerate()).isEqualTo(expectedStatusSitelinksModerate);

        Long modReason = testModerationRepository.getModReasonSitelinkSetIdByBannerId(bannerInfo.getShard(),
                bannerInfo.getBannerId());
        if (expectedClearModerationData) {
            assertThat(modReason).isNull();
        } else {
            assertThat(modReason).isNotNull();
        }
    }

    @Override
    protected ModerationMode getModerationMode() {
        return moderationMode;
    }

    protected static BannerProvider bannerWithoutSitelinks(OldTextBanner banner) {
        return steps -> steps.bannerSteps().createBanner(banner);
    }

    protected static BannerProvider bannerWithSitelinks(OldTextBanner banner) {
        return steps -> {
            SitelinkSetInfo sitelinkSet = steps.sitelinkSetSteps().createDefaultSitelinkSet();
            ClientInfo client = sitelinkSet.getClientInfo();
            return steps.bannerSteps().createBanner(banner.withSitelinksSetId(sitelinkSet.getSitelinkSetId()), client);
        };
    }

    private static BannerModelChangesProvider emptyModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static BannerModelChangesProvider newSitelinkModelChanges() {
        return (steps, banner) -> {
            SitelinkSetInfo newSitelinkSet =
                    steps.sitelinkSetSteps().createDefaultSitelinkSet(banner.getClientInfo());
            return new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                    .process(newSitelinkSet.getSitelinkSetId(), BannerWithSitelinks.SITELINKS_SET_ID)
                    .castModelUp(BannerWithSystemFields.class);
        };
    }

    private static BannerModelChangesProvider deleteSitelinkModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .process(null, BannerWithSitelinks.SITELINKS_SET_ID)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static BannerModelChangesProvider titleModelChanges() {
        return (steps, banner) -> new ModelChanges<>(banner.getBannerId(), TextBanner.class)
                .process("new title " + randomAlphabetic(5), TextBanner.TITLE)
                .castModelUp(BannerWithSystemFields.class);
    }


    interface BannerProvider {
        TextBannerInfo createBanner(Steps steps);
    }

    interface BannerModelChangesProvider {
        ModelChanges<BannerWithSystemFields> getModelChanges(Steps steps,
                                                             AbstractBannerInfo<? extends OldBannerWithSitelinks> banner);
    }
}
