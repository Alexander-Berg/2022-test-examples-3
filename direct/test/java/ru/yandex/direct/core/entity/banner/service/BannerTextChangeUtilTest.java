package ru.yandex.direct.core.entity.banner.service;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.Model;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.model.ModelWithId;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class BannerTextChangeUtilTest {

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public AppliedChanges<? extends Model> appliedChanges;

    @Parameterized.Parameter(2)
    public boolean expectedTextChange;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // старый баннер
                {
                        "при изменении TITLE у TextBanner - текст сильно изменился",
                        applyChange(new OldTextBanner(), OldTextBanner.class, OldTextBanner.TITLE, "new"),
                        true
                },
                {
                        "при изменении TITLE_EXTENSION у TextBanner - текст сильно изменился",
                        applyChange(new OldTextBanner(), OldTextBanner.class, OldTextBanner.TITLE_EXTENSION, "new"),
                        true
                },
                {
                        "при изменении BODY у TextBanner - текст сильно изменился",
                        applyChange(new OldTextBanner(), OldTextBanner.class, OldTextBanner.BODY, "new"),
                        true
                },
                {
                        "при отсутствии изменений TITLE_EXTENSION у TextBanner - текст не сильно изменился",
                        applyChange(new OldTextBanner().withTitleExtension("old"), OldTextBanner.class,
                                OldTextBanner.TITLE_EXTENSION, "old"),
                        false
                },
                //новый баннер
                {
                        "при изменении TITLE у NewTextBanner - текст сильно изменился",
                        applyChange(new TextBanner(), TextBanner.class, TextBanner.TITLE, "new"),
                        true
                },
                {
                        "при изменении TITLE_EXTENSION у NewTextBanner - текст сильно изменился",
                        applyChange(new TextBanner(), TextBanner.class, TextBanner.TITLE_EXTENSION, "new"),
                        true
                },
                {
                        "при изменении BODY у NewTextBanner - текст сильно изменился",
                        applyChange(new TextBanner(), TextBanner.class, TextBanner.BODY, "new"),
                        true
                },
                {
                        "при отсутствии изменений TITLE_EXTENSION у NewTextBanner - текст не сильно изменился",
                        applyChange(new TextBanner().withTitleExtension("old"), TextBanner.class,
                                TextBanner.TITLE_EXTENSION, "old"),
                        false
                },
        });
    }

    private static <T extends ModelWithId> AppliedChanges<T> applyChange(
            T banner,
            Class<T> clazz,
            ModelProperty<? super T, String> property,
            String changeValue) {
        return new ModelChanges<>(banner.getId(), clazz)
                .process(changeValue, property)
                .applyTo(banner);
    }

    @Test
    public void testIsBannerTextChangedSignificantly() {
        boolean actualTextChange =
                BannerTextChangeUtil.isBannerTextChangedSignificantly(appliedChanges);

        assertThat(actualTextChange, equalTo(expectedTextChange));
    }
}
