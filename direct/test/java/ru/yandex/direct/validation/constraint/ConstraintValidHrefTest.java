package ru.yandex.direct.validation.constraint;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;

@RunWith(Parameterized.class)
public class ConstraintValidHrefTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"валидный href", "http://www.ya.ru", null},
                {"валидный href с параметрами", "https://disk.yandex.ru/promo?utm=newgb&from={site}", null},
                {"недопустимый протокол 1", "ftp://ftp.server01.ru/index", invalidValue()},
                {"недопустимый протокол 2", "tel://02", invalidValue()},
                {"протокол отсутствует", "www", invalidValue()},
                {"хост отсутствует", "http://", invalidValue()},
                {
                        "национальные символы в get параметрах",
                        "http://udochka2.ohota-24.ru/#reviews?i=2?utm_source=rsya&utm_medium=cpc&utm_campaign={campaign_id}&utm_content=bs2_{ad_id}&utm_term=удочка}",
                        null
                },
                {
                        "национальные символы в хосте",
                        "https://яндекс.рф",
                        null
                },
                {
                        "punycode",
                        "http://xn--d1acpjx3f.xn--p1ai",
                        null
                },
                {
                        "punycode + нацсимволы",
                        "https://xn--d1acpjx3f.рф",
                        null
                },
                {
                        "punycode + ASCII доменная зона",
                        "https://xn--d1acpjx3f.ru",
                        null
                },
                {
                        "национальные символы в хосте",
                        "http://atatürk.tr/history",
                        null
                },
                {
                        "знаки препинания в get",
                        "http://ad.doubleclick.net/clk;228044917;38592682;t?http://www.klm.com/travel/ru_ru/index.htm?popup=no&WT.srch=1&WT.vr.rac_of=1&WT.vr.rac_ma=RU%20KLM%20Branding&WT.vr.rac_cr=KLM&WT.seg_1={keyword:nil}&WT.vr.rac_pl=RU&WT.vr.rac_pa=Yandex&WT.mc_id=2213229|3468187|38592682|228044917|785093&WT.srch=1",
                        null
                },
                {
                        "с ? после якоря",
                        "http://www.rbc.ru#sdf.html?asdfasdf&sdaf=asdfasdfsad",
                        invalidValue()
                },
                {
                        "с якорем без слеша и ?",
                        "http://www.rbc.ru#sdf.html",
                        null
                },
                {
                        "с якорем без слеша и ?, зато с параметром в {} перед якорем",
                        "http://www.rbc.ru{keyword}#sdf.html",
                        invalidValue()
                },
                {
                        "c неприметным шаблоном в домене",
                        "http://www.rbc.ru#keyword#sdf.html?asdfasdf&sdaf=asdfasdfsad",
                        invalidValue()
                },
                {
                        "c шаблоном в пути",
                        "http://www.rbc.ru/#keyword#sdf.html?asdfasdf&sdaf=asdfasdfsad",
                        null
                },
                {
                        "c вполне приметным шаблоном в домене",
                        "https://Myetherwallet.com#keyword#s1.Drolcoma.com/vacancy/?utm_term={keyword}&utm_campaign" +
                                "={region_name}&id=ul81rt",
                        invalidValue()
                },
                {
                        "национальные символы в get-параметрах, но ещё и параметр в домене",
                        "http://udochka2.ohota-24{ad_id}.ru/#reviews?i=2?utm_source=rsya&utm_medium=cpc&utm_campaign" +
                                "={campaign_id}&utm_content=bs2_{ad_id}&utm_term=удочка}",
                        invalidValue()
                },
                {
                        "без шаблонов, просто разломанный href",
                        "http://www.rbc.ru#keyword/lalala#sdf.html?asdfasdf&sdaf=asdfa",
                        invalidValue()
                },
                {
                        "с подстановочным параметром после ? без слеша",
                        "http://ya.ru?test={phrase_id}",
                        null
                },
                {
                        "с единичной фигурной скобкой до слеша",
                        "http://ya}keyword.ru/test",
                        invalidValue()
                },
                {
                        "",
                        "http://www.vsedlyauborki.ru/catalog/5/25#Derjateli_shubok_dlya_myt'ya_okon",
                        null
                },
                {
                        "непечатываемые символы",
                        "\nhttps://direct.yandex.ru",
                        invalidValue()
                },
                {
                        "нет домена первого уровня",
                        "http://.ru",
                        invalidValue()
                },
                {
                        "непечатываемые символы",
                        "https://direct.yandex.ru\n",
                        invalidValue()
                },
                {
                        "домен первого уровня больше 63 символов",
                        "http://landingqweasdzxcrqweasdzxcrqweasdzxcrqweasdzxcrqweasdzxcrqweasdzxc.roooz/",
                        invalidValue()
                },
                {
                        "длина доменов ровно 63 символа",
                        "http://k4u7cJuQyviPWLGjQcla6RaYruxrm1Vd4WlvPT7QY8HcDQmvIhYJ6dHS4A163yA.U4VN5zVSqgj50eoVY2vyciwVrzw0rTTC802PeD0Eo2exLdZwm5wX15KUQG7aEPd.roooz/",
                        null
                },
                {

                        "доменная зона превышает ограничение",
                        "http://yandex.ruuuuuuuuuuuuuuu/",
                        invalidValue()
                },
                {
                        "tld начинается с буквы",
                        "http://domain.123domain",
                        invalidValue()
                },
                {
                        "минимальная длина домена - 1",
                        "http://y.ru",
                        null
                },
                {
                        "минимальная длина поддомена - 1",
                        "http://d.yandex.ru",
                        null
                },
                {
                        "домен должен начинаться с буквы и цифры",
                        "http://vodnik.1000size.ru",
                        null
                },
                {
                        "домен должен начинаться с буквы и цифры",
                        "http://-1000size.ru",
                        invalidValue()
                },
                {
                        "ссылка может быть ссылкой на турбо-страницу (https)",
                        "https://yandex.ru/turbo?text=c4nv45-272922f2-9ef5-4444-b0a8-14b4aa8fe680",
                        null
                },
                {
                        "ссылка может быть ссылкой на турбо-страницу (http)",
                        "http://yandex.ru/turbo?text=c4nv45-272922f2-9ef5-4444-b0a8-14b4aa8fe680",
                        null
                },
                {
                        "с крестиком и кавычкой в пути",
                        "http://stem-nsk.ru/product/Montazhnaya-armatura-dlya-KIP/Krany-trehhodovye-dlya-manometrov" +
                                "/Kran-dlya-manometra-trehhodovoy-muftoviy-11b18bk-M20×1-5-G1-2\"-1-6-Mpa/",
                        null
                },
        });
    }

    private final String href;
    private final Defect<Void> expectedDefect;

    public ConstraintValidHrefTest(String testName, String href, Defect<Void> defect) {
        this.href = href;
        this.expectedDefect = defect;
    }

    @Test
    public void testParametrized() {
        assertThat(StringConstraints.validHref().apply(href), is(expectedDefect));
    }
}
