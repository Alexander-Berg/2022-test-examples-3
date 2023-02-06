package ru.yandex.direct.core.entity.banner.type.displayhref;

import java.util.Collection;
import java.util.function.Function;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.BannerDisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithDisplayHref;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref.DISPLAY_HREF;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_DISPLAY_HREFS;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithDisplayHrefModerationUpdateTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithDisplayHref> {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public DslContextProvider dslContextProvider;

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Function<Long, ModelChanges<TextBanner>> bannerChanges;

    @Parameterized.Parameter(2)
    public String sourceDisplayHref;

    @Parameterized.Parameter(3)
    public BannerDisplayHrefStatusModerate sourceStatusModerate;

    @Parameterized.Parameter(4)
    public String targetDisplayHref;

    @Parameterized.Parameter(5)
    public BannerDisplayHrefStatusModerate expectedStatusModerate;

    private static final String VALID_DISPLAY_HREF = "/display-href";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"меняем displayHref -> отправляем на модерацию",
                        emptyChanges(),
                        VALID_DISPLAY_HREF, BannerDisplayHrefStatusModerate.YES,
                        VALID_DISPLAY_HREF + "-new", BannerDisplayHrefStatusModerate.READY},
                {"устанавливаем displayHref -> отправляем на модерацию",
                        emptyChanges(),
                        null, null,
                        VALID_DISPLAY_HREF, BannerDisplayHrefStatusModerate.READY},
                {"displayHref задан и не меняется -> статус модерации остается прежним",
                        emptyChanges(),
                        VALID_DISPLAY_HREF, BannerDisplayHrefStatusModerate.NO,
                        VALID_DISPLAY_HREF, BannerDisplayHrefStatusModerate.NO},
                {"удаляем displayHref",
                        emptyChanges(),
                        VALID_DISPLAY_HREF, BannerDisplayHrefStatusModerate.YES,
                        null, null},
                {"displayHref не задан и не меняется",
                        emptyChanges(),
                        null, null,
                        null, null},

                {"если баннер поменялся и displayHref задан -> отправляем на модерацию, даже если уже промодерирован",
                        changesWithHrefChanged(),
                        VALID_DISPLAY_HREF, BannerDisplayHrefStatusModerate.YES,
                        VALID_DISPLAY_HREF, BannerDisplayHrefStatusModerate.READY},
        });
    }

    private static Function<Long, ModelChanges<TextBanner>> emptyChanges() {
        return bannerId -> new ModelChanges<>(bannerId, TextBanner.class);
    }

    private static Function<Long, ModelChanges<TextBanner>> changesWithHrefChanged() {
        return bannerId -> new ModelChanges<>(bannerId, TextBanner.class)
                .process("http://some.site.org", TextBanner.HREF);
    }

    @Test
    public void test() {
        Long bannerId = createBanner(sourceDisplayHref, sourceStatusModerate);

        ModelChanges<TextBanner> changes = bannerChanges.apply(bannerId)
                .process(targetDisplayHref, DISPLAY_HREF);

        prepareAndApplyValid(changes);

        BannerWithDisplayHref actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getDisplayHrefStatusModerate(), equalTo(expectedStatusModerate));
    }

    private Long createBanner(String displayHref, BannerDisplayHrefStatusModerate statusModerate) {
        bannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner().withDisplayHref(displayHref)
        );
        var bannerId = bannerInfo.getBannerId();

        if (statusModerate != null) {
            setDisplayHrefStatusModerate(bannerId, statusModerate);
        }

        return bannerId;
    }

    private void setDisplayHrefStatusModerate(Long bannerId, BannerDisplayHrefStatusModerate statusModerate) {
        dslContextProvider.ppc(bannerInfo.getShard())
                .update(BANNER_DISPLAY_HREFS)
                .set(BANNER_DISPLAY_HREFS.STATUS_MODERATE,
                        BannerDisplayHrefStatusModerate.toSource(statusModerate))
                .where(BANNER_DISPLAY_HREFS.BID.eq(bannerId))
                .execute();
    }
}
