package ru.yandex.direct.core.entity.banner.type.leadformattributes;

import java.time.LocalDateTime;
import java.util.Objects;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithLeadformAttributes;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithLeadformAttributesUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {

    private static final LocalDateTime DEFAULT_LAST_CHANGE = LocalDateTime.now().minusHours(1);

    private static final String HREF = "https://yandex.ru";
    private static final String BUTTON_TEXT = "Оставить заявку";

    private static final String OTHER_HREF = "https://yandex.by";
    private static final String OTHER_BUTTON_TEXT = "Оставить отзыв";

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String hrefBefore;

    @Parameterized.Parameter(2)
    public String buttonTextBefore;

    @Parameterized.Parameter(3)
    public String hrefAfter;

    @Parameterized.Parameter(4)
    public String buttonTextAfter;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] parameters() {
        return new Object[][] {
                {"Добавление ссылки и текста кнопки", null, null, HREF, BUTTON_TEXT},
                {"Изменение ссылки", HREF, BUTTON_TEXT, OTHER_HREF, BUTTON_TEXT},
                {"Изменение текста кнопки", HREF, BUTTON_TEXT, HREF, OTHER_BUTTON_TEXT},
                {"Изменение ссылки и текста кнопки", HREF, BUTTON_TEXT, OTHER_HREF, OTHER_BUTTON_TEXT},
                {"Удаление ссылки и текста кнопки", HREF, BUTTON_TEXT, null, null}
        };
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void update() {
        NewTextBannerInfo bannerInfo = createBanner(hrefBefore, buttonTextBefore);

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class);
        if (!Objects.equals(hrefBefore, hrefAfter)) {
            modelChanges.process(hrefAfter, BannerWithLeadformAttributes.LEADFORM_HREF);
        }
        if (!Objects.equals(buttonTextBefore, buttonTextAfter)) {
            modelChanges.process(buttonTextAfter, BannerWithLeadformAttributes.LEADFORM_BUTTON_TEXT);
        }

        prepareAndApplyValid(modelChanges);

        TextBanner actualBanner = getBanner(bannerInfo.getBannerId());
        SoftAssertions.assertSoftly(softly -> {
            assertThat(actualBanner.getLeadformHref()).isEqualTo(hrefAfter);
            assertThat(actualBanner.getLeadformButtonText()).isEqualTo(buttonTextAfter);
            assertThat(actualBanner.getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
            assertThat(actualBanner.getLastChange()).isAfter(DEFAULT_LAST_CHANGE);
        });
    }

    private NewTextBannerInfo createBanner(String leadformHref, String leadformButtonText) {
        TextBanner banner = fullTextBanner()
                .withLeadformHref(leadformHref)
                .withLeadformButtonText(leadformButtonText)
                .withStatusBsSynced(StatusBsSynced.YES)
                .withLastChange(DEFAULT_LAST_CHANGE);

        return steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(banner)
                .withClientInfo(clientInfo));
    }
}
