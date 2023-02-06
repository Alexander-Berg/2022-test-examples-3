package ru.yandex.direct.core.entity.banner.service.validation;

import java.util.Collection;

import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerTextConstraints.charsAreAllowed;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInField;

public class BannerTextConstraintsAllowedCharsTest extends BannerTextConstraintsBaseTest {

    public BannerTextConstraintsAllowedCharsTest() {
        super(charsAreAllowed());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "допустимые символы: символы английского алфавита",
                        "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz",
                        null
                },
                {
                        "допустимые символы: символы русского алфавита",
                        "АаБбВвГгДдЕеЁёЖжЗзИиЙйКкЛлМмНнОоПпРрСсТтУуФфХхЦцЧчШшЩщЬьЫыЪъЭэЮюЯя",
                        null
                },
                {
                        "допустимые символы: символы украинского алфавита",
                        "ҐґЄєІіЇї",
                        null
                },
                {
                        "допустимые символы: символы турецкого алфавита",
                        "ÇçĞğİiÖöŞşÜü",
                        null
                },
                {
                        "допустимые символы: символы казахского алфавита",
                        "ӘәҒғҚқҢңӨөҰұҮүҺһІі",
                        null
                },
                {
                        "допустимые символы: символы немецкого алфавита",
                        "ÄäÖöÜüß",
                        null
                },
                {
                        "допустимые символы: символы белорусского алфавита",
                        "ІіЎў",
                        null
                },

                {
                        "допустимые символы: числа",
                        "0123456789",
                        null
                },
                {
                        "допустимые символы: доп. знаки",
                        "-+,. \"!?\\()%$€;:/&'*_=#№«»\u00a0–—−",
                        null
                },
                {
                        "допустимые спец. символы",
                        "™®©’°⁰¹²³⁴⁵⁶⁷⁸⁹\u20bd",
                        null
                },
                {
                        "узбеские апострофы",
                        "ʻʼ",
                        null
                },
                {
                        "недопустимые символы 1",
                        "@",
                        restrictedCharsInField()
                },
                {
                        "недопустимые символы 2",
                        "لويكيبيديا",
                        restrictedCharsInField()
                },
        });
    }
}
