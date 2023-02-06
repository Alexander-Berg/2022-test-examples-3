package ru.yandex.direct.core.entity.banner.type.displayhref;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInDisplayHref;
import static ru.yandex.direct.core.entity.banner.type.displayhref.BannerWithDisplayHrefConstraints.displayHrefCharsAreAllowed;

public class BannerWithDisplayHrefConstraintsCharsAreAllowedTest extends BannerWithDisplayHrefConstraintsBaseTest {

    public BannerWithDisplayHrefConstraintsCharsAreAllowedTest() {
        super(displayHrefCharsAreAllowed());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"допустимые символы в display href: символы  английского алфавита",
                        "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz", null},
                {"допустимые символы в display href: символы русского алфавита",
                        "АаБбВвГгДдЕеЁёЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЬьЫыЪъЭэЮюЯя", null},
                {"допустимые символы в display href: символы  украинского алфавита", "ҐґЄєІіЇї", null},
                {"допустимые символы в display href: символы  турецкого алфавита", "ÇçĞğİiÖöŞşÜü", null},
                {"допустимые символы в display href: символы  казахского алфавита", "ӘәҒғҚқҢңӨөҰұҮүҺһІі", null},
                {"допустимые символы в display href: символы  немецкого алфавита", "ÄäÖöÜüß", null},
                {"допустимые символы в display href: символы  белорусского алфавита", "ІіЎў", null},
                {"допустимые символы в display href: цифры", "0123456789", null},
                {"допустимые символы в display href: другие символы", "-№/%#", null},

                {"недопустимые символы в display href: @", "@", restrictedCharsInDisplayHref("@")},
                {"недопустимые символы в display href: *", "*", restrictedCharsInDisplayHref("*")},
                {"недопустимые символы в display href: !", "!", restrictedCharsInDisplayHref("!")},
                {"недопустимые символы в display href: .", ".", restrictedCharsInDisplayHref(".")}
        });
    }

}
