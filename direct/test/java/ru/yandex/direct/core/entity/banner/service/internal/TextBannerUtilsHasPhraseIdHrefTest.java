package ru.yandex.direct.core.entity.banner.service.internal;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.old.BannerUtils;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TextBannerUtilsHasPhraseIdHrefTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"http://www.yandex.ru/{phrase_id}", true},
                {"http://www.yandex.ru/{phraseid}", true},
                {"http://www.yandex.ru/{param127}", true},
                {"http://www.yandex.ru/{retargeting_id}", true},
                {"http://www.yandex.ru/{phraseid2}", false},
        });
    }

    private String href;
    private boolean expectedHasPhraseIdHref;

    public TextBannerUtilsHasPhraseIdHrefTest(String href, boolean expectedHasPhraseIdHref) {
        this.href = href;
        this.expectedHasPhraseIdHref = expectedHasPhraseIdHref;
    }

    @Test
    public void parametrizedTest() {
        OldTextBanner banner = (new OldTextBanner()).withHref(href);
        Boolean hasPhraseIdHref = BannerUtils.BANNER_HAS_PHRASEID_HREF.test(banner);
        assertThat(hasPhraseIdHref, is(expectedHasPhraseIdHref));
    }
}
