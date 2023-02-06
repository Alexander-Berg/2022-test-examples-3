package ru.yandex.direct.core.entity.banner.type.turbogallery;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboGalleryHref;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.time.LocalDateTime.now;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields.LAST_CHANGE;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields.STATUS_BS_SYNCED;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields.STATUS_MODERATE;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTurboGallery.TURBO_GALLERY_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximately;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithTurboGalleryUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTurboGalleryHref> {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String originalTurboGalleryHref;

    @Parameterized.Parameter(2)
    public ModelChangesProvider modelChangesProvider;

    @Parameterized.Parameter(3)
    public String expectedTurboGalleryHref;

    @Parameterized.Parameter(4)
    public Boolean lastChangeMustBeUpdated;

    @Parameterized.Parameter(5)
    public StatusBsSynced expectedStatusBsSynced;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return List.of(new Object[][]{
                {
                        "добавляем турбо-галерею",
                        null, turboGalleryChanges(VALID_TURBO_GALLERY_HREF),
                        VALID_TURBO_GALLERY_HREF, true, StatusBsSynced.NO
                },
                {
                        "меняем ссылку на турбо-галерею",
                        VALID_TURBO_GALLERY_HREF + "/old", turboGalleryChanges(VALID_TURBO_GALLERY_HREF),
                        VALID_TURBO_GALLERY_HREF, true, StatusBsSynced.NO
                },
                {
                        "удаляем турбо-галерею",
                        VALID_TURBO_GALLERY_HREF, turboGalleryChanges(null),
                        null, true, StatusBsSynced.NO
                },
                {
                        "ничего не изменилось",
                        VALID_TURBO_GALLERY_HREF, emptyChanges(),
                        VALID_TURBO_GALLERY_HREF, false, StatusBsSynced.YES
                }
        });
    }

    private static final String VALID_TURBO_GALLERY_HREF = "https://yandex.ru/turbo?42";
    private static final LocalDateTime YESTERDAY = now().minusDays(1);

    @Test
    public void updateValidTurboGallery() {
        bannerInfo = createBannerWithTurboGallery(originalTurboGalleryHref);
        Long bannerId = bannerInfo.getBannerId();
        var createdBanner = getBanner(bannerId);
        assumeThat(createdBanner, allOf(
                hasProperty(TURBO_GALLERY_HREF.name(), equalTo(originalTurboGalleryHref)),
                hasProperty(STATUS_BS_SYNCED.name(), equalTo(StatusBsSynced.YES)),
                hasProperty(STATUS_MODERATE.name(), equalTo(BannerStatusModerate.YES)),
                hasProperty(LAST_CHANGE.name(), approximately(YESTERDAY))
        ));

        ModelChanges<BannerWithSystemFields> changes = modelChangesProvider.getModelChanges(bannerId);
        prepareAndApplyValid(changes);

        TextBanner banner = getBanner(bannerId);
        assertThat(banner, allOf(
                hasProperty(TURBO_GALLERY_HREF.name(), equalTo(expectedTurboGalleryHref)),
                hasProperty(STATUS_BS_SYNCED.name(), equalTo(expectedStatusBsSynced)),
                hasProperty(STATUS_MODERATE.name(), equalTo(BannerStatusModerate.YES)),
                hasProperty(LAST_CHANGE.name(), lastChangeMustBeUpdated ? approximatelyNow() : approximately(YESTERDAY))
        ));
    }

    private TextBannerInfo createBannerWithTurboGallery(@Nullable String turboGalleryHref) {
        return steps.bannerSteps().createBanner(activeTextBanner().withTurboGalleryHref(turboGalleryHref).withLastChange(YESTERDAY));
    }

    private static ModelChangesProvider emptyChanges() {
        return bannerId -> new ModelChanges<>(bannerId, TextBanner.class)
                .castModelUp(BannerWithSystemFields.class);
    }

    private static ModelChangesProvider turboGalleryChanges(@Nullable String turboGalleryHref) {
        return bannerId -> new ModelChanges<>(bannerId, TextBanner.class)
                .process(turboGalleryHref, TURBO_GALLERY_HREF)
                .castModelUp(BannerWithSystemFields.class);
    }

    interface ModelChangesProvider {
        ModelChanges<BannerWithSystemFields> getModelChanges(Long banner);
    }
}
