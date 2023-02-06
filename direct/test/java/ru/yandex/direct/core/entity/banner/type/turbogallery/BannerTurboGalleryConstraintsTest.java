package ru.yandex.direct.core.entity.banner.type.turbogallery;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidHref;
import static ru.yandex.direct.core.entity.banner.type.turbogallery.BannerTurboGalleryConstraints.validTurboGalleryHref;

@RunWith(Parameterized.class)
public class BannerTurboGalleryConstraintsTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return List.of(new Object[][]{
                { "", invalidHref() },
                { "    \n   \t   ", invalidHref() },
                { "yandex.ru/turbo", invalidHref() },
                { "http://ya.ru/turbo", invalidHref() },
                { "http://yandex.com/turbo", invalidHref() },
                { "http://www.yandex.ru/turbo", invalidHref() },
                { "http://yandex.ru/turba", invalidHref() },
                { "http://yandex.ru/turbo/my gallery", invalidHref() },

                { null, null },
                { "http://yandex.ru/turbo", null },
                { "https://yandex.ru/turbo", null },
                { "http://yandex.ru/turbo/any-path/without!whitespaces?including=<>+{}{}()#*?having_any_length", null }
        });
    }

    @Parameterized.Parameter
    public String href;

    @Parameterized.Parameter(1)
    public Defect expectedDefect;

    @Test
    public void test() {
        var defect = validTurboGalleryHref().apply(href);
        assertThat(defect, equalTo(expectedDefect));
    }
}
