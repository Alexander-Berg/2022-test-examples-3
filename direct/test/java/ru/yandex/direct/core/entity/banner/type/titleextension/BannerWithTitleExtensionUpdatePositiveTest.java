package ru.yandex.direct.core.entity.banner.type.titleextension;

import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTitleExtension;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields.STATUS_BS_SYNCED;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields.STATUS_MODERATE;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTitleExtension.ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTitleExtension.TITLE_EXTENSION;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithTitleExtensionUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithTitleExtension> {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String sourceTitleExtension;

    @Parameterized.Parameter(2)
    public String titleExtension;

    @Parameterized.Parameter(3)
    public StatusBsSynced expectedStatusBsSybced;

    @Parameterized.Parameter(4)
    public BannerStatusModerate expectedStatusModerate;

    private static final String VALID_TITLE_EXTENSION = "продать товар";

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "устанавливаем второй заголовок",
                        null, VALID_TITLE_EXTENSION,
                        StatusBsSynced.NO, BannerStatusModerate.READY
                },
                {
                        "меняем второй заголовок",
                        VALID_TITLE_EXTENSION, VALID_TITLE_EXTENSION + " дёшево",
                        StatusBsSynced.NO, BannerStatusModerate.READY
                },
                {
                        "сбрасываем второй заголовок",
                        VALID_TITLE_EXTENSION, null,
                        StatusBsSynced.NO, BannerStatusModerate.READY
                },
                {
                        "незначительно меняем второй заголовок",
                        VALID_TITLE_EXTENSION, VALID_TITLE_EXTENSION + " ",
                        StatusBsSynced.NO, BannerStatusModerate.YES
                },
        });
    }

    @Test
    public void updateValidTitleExtension() {
        bannerInfo = createActiveBanner(sourceTitleExtension);

        ModelChanges<TextBanner> changes = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(titleExtension, TITLE_EXTENSION);
        Long id = prepareAndApplyValid(changes);

        var actualBanner = getBanner(id);
        assertThat(actualBanner, allOf(
                hasProperty(ID.name(), equalTo(bannerInfo.getBannerId())),
                hasProperty(TITLE_EXTENSION.name(), equalTo(titleExtension)),
                hasProperty(STATUS_BS_SYNCED.name(), equalTo(expectedStatusBsSybced)),
                hasProperty(STATUS_MODERATE.name(), equalTo(expectedStatusModerate))
        ));
    }

    private TextBannerInfo createActiveBanner(String titleExtension) {
        return steps.bannerSteps().createActiveTextBanner(
                activeTextBanner().withTitleExtension(titleExtension)
        );
    }
}
