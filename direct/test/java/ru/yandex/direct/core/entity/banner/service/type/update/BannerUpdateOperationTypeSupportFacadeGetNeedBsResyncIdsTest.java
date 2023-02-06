package ru.yandex.direct.core.entity.banner.service.type.update;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class BannerUpdateOperationTypeSupportFacadeGetNeedBsResyncIdsTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    private static final String DEFAULT_OLD_VALUE = "oldValue";
    private static final String DEFAULT_NEW_VALUE = "newValue";
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final long BANNER_ID = 1L;

    @Autowired
    private BannerUpdateOperationTypeSupportFacade serviceUnderTest;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerWithSystemFields banner;

    @Parameterized.Parameter(2)
    public ModelProperty modelProperty;

    @Parameterized.Parameter(3)
    public Object oldValue;

    @Parameterized.Parameter(4)
    public Object newValue;

    @Parameterized.Parameter(5)
    public Boolean needBsResync;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер с неизмененным HREF. Не нужно переотправлять в БК",
                        new TextBanner(),
                        TextBanner.HREF,
                        "sameValue",
                        "sameValue",
                        false
                },
                {
                        "Текстовый баннер с измененным HREF. Нужно переотправить в БК",
                        new TextBanner(),
                        TextBanner.HREF,
                        DEFAULT_OLD_VALUE,
                        DEFAULT_NEW_VALUE,
                        true
                },
                {
                        "Текстовый баннер с измененным BODY. Нужно переотправить в БК",
                        new TextBanner(),
                        TextBanner.BODY,
                        DEFAULT_OLD_VALUE,
                        DEFAULT_NEW_VALUE,
                        true
                },
                {
                        "Текстовый баннер с измененным TITLE. Нужно переотправить в БК",
                        new TextBanner(),
                        TextBanner.TITLE,
                        DEFAULT_OLD_VALUE,
                        DEFAULT_NEW_VALUE,
                        true
                },
                {
                        "Текстовый баннер с измененным FLAGS. Нужно переотправить в БК",
                        new TextBanner(),
                        TextBanner.FLAGS,
                        new BannerFlags().with(BannerFlags.AGE, Age.AGE_16),
                        new BannerFlags().with(BannerFlags.AGE, Age.AGE_12),
                        true
                },
                {
                        "Текстовый баннер с измененным TITLE_EXTENSION. Нужно переотправить в БК",
                        new TextBanner(),
                        TextBanner.TITLE_EXTENSION,
                        DEFAULT_OLD_VALUE,
                        DEFAULT_NEW_VALUE,
                        true
                },
                {
                        "Баннер \"Продвижение контента\" с измененным VISIT_URL. Нужно переотправить в БК",
                        new ContentPromotionBanner(),
                        ContentPromotionBanner.VISIT_URL,
                        DEFAULT_OLD_VALUE,
                        DEFAULT_NEW_VALUE,
                        true
                },
        });
    }

    @Test
    public void testGetNeedBsResyncIds() {
        var ac = applyChange(banner, modelProperty, oldValue, newValue);
        Set<Long> needBsResyncIds = serviceUnderTest.getNeedBsResyncIds(List.of(ac));
        if (needBsResync) {
            assertThat(needBsResyncIds, contains(BANNER_ID));
        } else {
            assertThat(needBsResyncIds, empty());
        }
    }

    private <T extends Banner, V> AppliedChanges<T> applyChange(
            T banner,
            ModelProperty property,
            V oldValue,
            V newValue) {

        banner.withId(BANNER_ID);
        property.set(banner, oldValue);

        return new ModelChanges<>(banner.getId(), banner.getClass())
                .process(newValue, property)
                .applyTo(banner);
    }


}
