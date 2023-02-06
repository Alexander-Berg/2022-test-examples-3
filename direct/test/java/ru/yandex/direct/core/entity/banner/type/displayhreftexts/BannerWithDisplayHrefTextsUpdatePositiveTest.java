package ru.yandex.direct.core.entity.banner.type.displayhreftexts;

import java.time.LocalDateTime;
import java.util.Objects;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHrefTexts;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithDisplayHrefTextsUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {

    private static final LocalDateTime DEFAULT_LAST_CHANGE = LocalDateTime.now().minusHours(1);

    private static final String PREFIX = "Яндекс.Бизнес";
    private static final String OTHER_PREFIX = "Яндекс.Бизнес 2";

    private static final String SUFFIX = "ТПК Абсолют";
    private static final String OTHER_SUFFIX = "ТПК Абсолют 2";

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String prefixBefore;

    @Parameterized.Parameter(2)
    public String suffixBefore;

    @Parameterized.Parameter(3)
    public String prefixAfter;

    @Parameterized.Parameter(4)
    public String suffixAfter;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] parameters() {
        return new Object[][] {
                {"Добавление префикса", null, null, PREFIX, null},
                {"Добавление суффикса", null, null, null, SUFFIX},
                {"Добавление префикса и суффикса", null, null, PREFIX, SUFFIX},
                {"Изменение префикса", PREFIX, null, OTHER_PREFIX, null},
                {"Изменение суффикса", null, SUFFIX, null, OTHER_SUFFIX},
                {"Изменение префикса и суффикса", PREFIX, SUFFIX, OTHER_PREFIX, OTHER_SUFFIX},
                {"Удаление префикса", PREFIX, SUFFIX, null, SUFFIX},
                {"Удаление суффикса", PREFIX, SUFFIX, PREFIX, null},
                {"Удаление префикса и суффикса", PREFIX, SUFFIX, null, null},

                {"Удаление префикса и изменение суффикса", PREFIX, SUFFIX, null, OTHER_SUFFIX},
                {"Удаление префикса и добавление суффикса", PREFIX, null, null, SUFFIX},
                {"Добавление префикса и изменение суффикса", null, SUFFIX, PREFIX, OTHER_SUFFIX},
        };
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void update() {
        NewTextBannerInfo bannerInfo = createBanner(prefixBefore, suffixBefore);

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class);
        if (!Objects.equals(prefixBefore, prefixAfter)) {
            modelChanges.process(prefixAfter, BannerWithDisplayHrefTexts.DISPLAY_HREF_PREFIX);
        }
        if (!Objects.equals(suffixBefore, suffixAfter)) {
            modelChanges.process(suffixAfter, BannerWithDisplayHrefTexts.DISPLAY_HREF_SUFFIX);
        }

        prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(bannerInfo.getBannerId());
        SoftAssertions.assertSoftly(softly -> {
            assertThat(actualBanner.getDisplayHrefPrefix()).isEqualTo(prefixAfter);
            assertThat(actualBanner.getDisplayHrefSuffix()).isEqualTo(suffixAfter);
            assertThat(actualBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            assertThat(actualBanner.getLastChange()).isAfter(DEFAULT_LAST_CHANGE);
        });
    }

    private NewTextBannerInfo createBanner(String displayHrefPrefix, String displayHrefSuffix) {
        TextBanner banner = fullTextBanner()
                .withDisplayHrefPrefix(displayHrefPrefix)
                .withDisplayHrefSuffix(displayHrefSuffix)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withLastChange(DEFAULT_LAST_CHANGE);

        return steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(banner)
                .withClientInfo(clientInfo));
    }

}
