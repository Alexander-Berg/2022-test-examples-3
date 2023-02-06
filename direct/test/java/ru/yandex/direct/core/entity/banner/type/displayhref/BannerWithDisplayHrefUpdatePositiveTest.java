package ru.yandex.direct.core.entity.banner.type.displayhref;

import java.time.LocalDateTime;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithDisplayHref;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref.DISPLAY_HREF;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields.LAST_CHANGE;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields.STATUS_BS_SYNCED;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximately;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithDisplayHrefUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithDisplayHref> {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String sourceDisplayHref;

    @Parameterized.Parameter(2)
    public String targetDisplayHref;

    @Parameterized.Parameter(3)
    public Boolean lastChangeMustBeUpdated;

    @Parameterized.Parameter(4)
    public StatusBsSynced expectedStatusBsSynced;

    private static final String VALID_DISPLAY_HREF = "/display-href";
    private static final LocalDateTime YESTERDAY = now().minusDays(1);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"устанавливаем displayHref", null, VALID_DISPLAY_HREF, true, StatusBsSynced.YES},
                {"меняем displayHref", VALID_DISPLAY_HREF, VALID_DISPLAY_HREF + "-new", true, StatusBsSynced.YES},
                {"сбрасываем displayHref", VALID_DISPLAY_HREF, null, true, StatusBsSynced.NO},
                {"displayHref не меняется", VALID_DISPLAY_HREF, VALID_DISPLAY_HREF, false, StatusBsSynced.YES},
        });
    }

    @Test
    public void validDisplayHref() {
        Long bannerId = createBanner(sourceDisplayHref);

        ModelChanges<TextBanner> changes = new ModelChanges<>(bannerId, TextBanner.class)
                .process(targetDisplayHref, DISPLAY_HREF);

        prepareAndApplyValid(changes);

        BannerWithDisplayHref banner = getBanner(bannerId);
        assertThat(banner, allOf(
                hasProperty(DISPLAY_HREF.name(), equalTo(targetDisplayHref)),
                hasProperty(STATUS_BS_SYNCED.name(), equalTo(expectedStatusBsSynced)),
                hasProperty(LAST_CHANGE.name(), lastChangeMustBeUpdated ?
                        approximatelyNow() : approximately(YESTERDAY))
        ));
    }

    private Long createBanner(String displayHref) {
        bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner().withDisplayHref(displayHref).withLastChange(YESTERDAY)
        );
        return bannerInfo.getBannerId();
    }
}
