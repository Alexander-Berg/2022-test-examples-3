package ru.yandex.direct.core.entity.banner.type.flags;

import java.util.Collection;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldMobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithFlags.FLAGS;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.age;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.babyFood;
import static ru.yandex.direct.core.testing.data.TestBanerFlags.empty;
import static ru.yandex.direct.core.testing.data.TestBanners.activeMobileAppBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithFlagsUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public OldBanner banner;

    @Parameterized.Parameter(2)
    public ModelChangesProvider modelChangesProvider;

    @Parameterized.Parameter(3)
    public BannerFlags targetFlags;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер: можно менять значение ранее установленного флага AGE",
                        textBanner(age(18)),
                        modelChanges(TextBanner.class), age(12)
                },
                {
                        "Текстовый баннер: можно менять значение ранее установленного флага BABY_FOOD",
                        textBanner(babyFood(8)),
                        modelChanges(TextBanner.class), babyFood(12)
                },

                {
                        "РМП-баннер: можно выставлять флаг AGE",
                        mobileAppBanner(empty()),
                        modelChanges(MobileAppBanner.class), age(18)
                },
                {
                        "РМП-баннер: можно выставлять флаг BABY_FOOD",
                        mobileAppBanner(empty()),
                        modelChanges(MobileAppBanner.class), babyFood(12)
                },
                {
                        "РМП-баннер: можно менять значение ранее установленного флагов AGE",
                        mobileAppBanner(age(18)),
                        modelChanges(MobileAppBanner.class), age(16)
                },
                {
                        "РМП-баннер: можно менять значение ранее установленного флагов BABY_FOOD",
                        mobileAppBanner(babyFood(12)),
                        modelChanges(MobileAppBanner.class), babyFood(3)
                },
                {
                        "РМП-баннер: можно удалять флаг AGE",
                        mobileAppBanner(age(18)),
                        modelChanges(MobileAppBanner.class), empty()
                },
                {
                        "РМП-баннер: можно удалять флаг BABY_FOOD",
                        mobileAppBanner(babyFood(5)),
                        modelChanges(MobileAppBanner.class), empty()
                },
        });
    }

    @Before
    public void setup() {
        steps.trustedRedirectSteps().addValidCounters();
    }

    @Test
    public void validFlags() {
        Long bannerId = createBanner(banner);

        BannerWithFlags createdBanner = getBanner(bannerId);
        assumeThat(createdBanner.getFlags(), equalTo(banner.getFlags().getFlags().isEmpty() ? null : banner.getFlags()));

        prepareAndApplyValid(modelChangesProvider.getModelChanges(bannerId, targetFlags));

        BannerWithFlags banner = getBanner(bannerId);
        assertThat(banner, hasProperty(FLAGS.name(), equalTo(targetFlags.getFlags().isEmpty() ? null : targetFlags)));
    }

    private Long createBanner(OldBanner banner) {
        bannerInfo = steps.bannerSteps().createBanner(banner);
        return bannerInfo.getBannerId();
    }

    private static OldTextBanner textBanner(BannerFlags flags) {
        return activeTextBanner().withFlags(flags);
    }

    private static OldMobileAppBanner mobileAppBanner(BannerFlags flags) {
        return activeMobileAppBanner().withFlags(flags);
    }

    private static <B extends BannerWithSystemFields> ModelChangesProvider modelChanges(Class<B> bannerType) {
        return (bannerId, newFlags) -> new ModelChanges<>(bannerId, bannerType)
                .processNotNull(newFlags, BannerWithFlags.FLAGS)
                .castModelUp(BannerWithSystemFields.class);
    }

    interface ModelChangesProvider {
        ModelChanges<BannerWithSystemFields> getModelChanges(Long banner, BannerFlags newFlags);
    }
}
