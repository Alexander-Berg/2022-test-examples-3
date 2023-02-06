package ru.yandex.direct.core.entity.domain;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.domain.AggregatorDomainsUtils.extractAggregatorDomainFromHref;

@CoreTest
@RunWith(Parameterized.class)
public class ExtractAggregatorDomainTest {
    @Parameterized.Parameter(0)
    public String domain;

    @Parameterized.Parameter(1)
    public String href;

    @Parameterized.Parameter(2)
    public String expectedDomain;

    @Parameterized.Parameters(name = "domain = {0}, href = {1}, expectedDomain = {2}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {null, null, null},
                {null, "", null},
                {null, "http://vk.com/test", "test.vk.com"},
                {null, "http://vK.cOm/TeSt", "test.vk.com"},
                {null, "https://vk.com/test", "test.vk.com"},
                {null, "http://vk.com/test?param=1", "test.vk.com"},
                {null, "http://vk.com/test2#results", "test2.vk.com"},
                {null, "http://vk.com/test1/test2/?param=1#results", "test1.vk.com"},
                {null, "https://vk.com/part1/part2/part3/part4", "part1.vk.com"},
                {null, "https://vk.com/", null},
                {null, "https://vk.com", null},
                {null, "https://", null},
                {null, "http://vk.com:8012/test?param=1", "test.vk.com"},
                {null, "http://m.vk.com/test", "test.vk.com"},
                {null, "http://part1.part2.part3.vk.com:8012/test1/test2", "test1.vk.com"},
                {null, "http://notvk.com/test", null},
                {null, "http://com/test", null},
                {null, "http://vk.com//test", null},
                {null, "http://vk.com/test/", "test.vk.com"},
                {null, "http://vk.com/test1/test2", "test1.vk.com"},
                // замена недопустимых символов на -
                {null, "https://vk.com/part1_part2", "part1-part2.vk.com"},
                {null, "https://vk.com/part1_part2__part3___part4-part5--part6_-_part7/",
                        "part1-part2-part3-part4-part5-part6-part7.vk.com"},
                {null, "https://vk.com/-.-part1_part2_..-part3-_._/", "part1-part2-part3.vk.com"},
                {null, "https://vk.com/-_-_/test/", null},
                {null, "http://VK.COM/Русские_Буквы", "русские-буквы.vk.com"},
                // длина больше максимальной
                {null, "https://vk.com/" + StringUtils.repeat("z", 64), null},

                // instagram.com
                {null, "https://www.instagram.com", null},
                {null, "http://instagram.com/", null},
                {null, "https://www.instagram.com/test/", "test.instagram.com"},
                {null, "https://www.instagram.com/..test1.test2-test3--/test4/?param=1#test",
                        "test1-test2-test3.instagram.com"},

                // ok.ru
                {null, "http://ok.ru", null},
                {null, "http://ok.ru/", null},
                {null, "http://ok.ru/profile/123456789", "profile-123456789.ok.ru"},
                {null, "http://ok.ru/profile/123456789/test1/test2?param=1", "profile-123456789.ok.ru"},
                {null, "https://ok.ru/group/1234567", "group-1234567.ok.ru"},
                {null, "https://www.ok.ru/group1234567", "group-1234567.ok.ru"},
                {null, "https://www.ok.ru/group1234567/test1/test2#test3", "group-1234567.ok.ru"},
                {null, "https://www.ok.ru/testgroup1234567/", "testgroup1234567.ok.ru"},
                {null, "https://www.ok.ru/..test1.test2-test3--/test4/?param=1#test", "test1-test2-test3.ok.ru"},
                {null, "http://ok.ru/-..-/", null},
                {null, "http://ok.ru/1234", "1234.ok.ru"},
                {null, "http://ok.ru/1", "1.ok.ru"},
                {null, "https://ok.ru/dk?st.cmd=anonymMain", null},

                // youtube.com
                {null, "https://www.youtube.com", null},
                {null, "http://youtube.com/", null},
                {null, "https://www.youtube.com/watch?v=TeSt123", null},
                {null, "https://www.youtube.com/channel/tEsT1234", "channel-test1234.youtube.com"},
                {null, "https://www.youtube.com/channel/Test1_tEst2-test3/test4?param=1",
                        "channel-test1-test2-test3.youtube.com"},
                {null, "https://www.youtube.com/channel/", null},

                // sites.google.com
                {null, "https://sites.google.com", null},
                {null, "https://sites.google.com/", null},
                {null, "https://sites.google.com/site/test", "test-site.sites.google.com"},
                {null, "http://www.sites.google.com/site/test", "test-site.sites.google.com"},
                {null, "https://sites.google.com/site/-test1__test2-test3--/test4?param=1",
                        "test1-test2-test3-site.sites.google.com"},
                {null, "https://sites.google.com/site/", null},
                {null, "https://sites.google.com/view/test123/", "test123-view.sites.google.com"},
                {null, "https://sites.google.com/test1/test2/", null},

                //maps
                {"maps.yandex.ru", "https://yandex.ru/maps/?orgpage[id]=197060445683", "197060445683.maps.yandex.ru"},
                {"maps.yandex.ru", "https://yandex.ru/maps/org/197060445683", "197060445683.maps.yandex.ru"},
                {"maps.yandex.ru", "https://yandex.ru/maps/?orgpage%5Bid%5D=197060445683", "197060445683.maps.yandex" +
                        ".ru"},
                {"maps.yandex.ru", "https://maps.yandex.ru/org/197060445683", "197060445683.maps.yandex.ru"},
                {"maps.yandex.ru", "https://yandex.ru/web-maps/org/97342911280", "97342911280.maps.yandex.ru"},

                //uslugi profile
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/profile/NatalyaAleksandrovnaL-247529",
                        "natalyaaleksandrovnal-247529.uslugi.yandex.ru"},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/profile/NatalyaAleksandrovnaL-247529/wer",
                        "natalyaaleksandrovnal-247529.uslugi.yandex.ru"},
                {"uslugi.yandex.ru", "https://uslugi.yandex.ru/uslugi/profile/NatalyaAleksandrovnaL-247529",
                        "natalyaaleksandrovnal-247529.uslugi.yandex.ru"},
                {"uslugi.yandex.ru", "https://uslugi.yandex.ru/profile/NatalyaAleksandrovnaL-247529",
                        "natalyaaleksandrovnal-247529.uslugi.yandex.ru"},
                {"uslugi.yandex.ru", "https://uslugi.yandex.ru/profile/NatalyaAleksandrovnaL-247529/wer",
                        "natalyaaleksandrovnal-247529.uslugi.yandex.ru"},
                {"uslugi.yandex.ru", "https://uslugi.yandex.ru/profile/", null},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/profile/", null},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/profile////", null},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/", null},
                //uslugi worker_id
                {"uslugi.yandex.ru",
                        "https://yandex.ru/uslugi/search?worker_id=3bf33ca1-0390-4ba4-8c71-f637180fea17&card_id=a5557d7b-f752-4d77-a461-30959e36c59f",
                        "3bf33ca1-0390-4ba4-8c71-f637180fea17.uslugi.yandex.ru"},
                {"uslugi.yandex.ru",
                        "https://yandex.ru/uslugi/search?worker_id=3bf33ca1-0390-4ba4-8c71-f637180fea17",
                        "3bf33ca1-0390-4ba4-8c71-f637180fea17.uslugi.yandex.ru"},
                {"uslugi.yandex.ru",
                        "https://yandex.ru/uslugi/search/xyz?worker_id=3bf33ca1-0390-4ba4-8c71-f637180fea17",
                        "3bf33ca1-0390-4ba4-8c71-f637180fea17.uslugi.yandex.ru"},
                {"uslugi.yandex.ru",
                        "https://uslugi.yandex.ru/search?worker_id=3bf33ca1-0390-4ba4-8c71-f637180fea17",
                        "3bf33ca1-0390-4ba4-8c71-f637180fea17.uslugi.yandex.ru"},
                {"uslugi.yandex.ru",
                        "https://uslugi.yandex.ru/uslugi/search?worker_id=3bf33ca1-0390-4ba4-8c71-f637180fea17",
                        "3bf33ca1-0390-4ba4-8c71-f637180fea17.uslugi.yandex.ru"},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi?worker_id=3bf33ca1-0390-4ba4-8c71-f637180fea17", null},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/search?worker_id=&bcd=abcd", null},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/search?worker_id=", null},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/search?worker_id=abcd=abcd",
                        "abcd-abcd.uslugi.yandex.ru"},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/search?", null},
                {"uslugi.yandex.ru", "https://yandex.ru/uslugi/search", null},

                //collections
                {"collections.yandex.ru", "https://yandex.ru/collections/user/ortix-ru/kompressionnyi-trikotazh/",
                        "ortix-ru.collections.yandex.ru"},
                {"collections.yandex.ru", "https://yandex.ru/collections/user/ortix-ru/",
                        "ortix-ru.collections.yandex.ru"},
                {"collections.yandex.ru", "https://yandex.ru/collections/", null},
                {"collections.yandex.ru", "https://yandex.ru/collections/user/", null},

                //turbo
                {"turbo.site", "https://turbo.site/page364228", "page364228.turbo.site"},
                {"turbo.site", "https://turbo.site/page431431/irbis/?utm_source=yandex", "page431431.turbo.site"},
                {"turbo.site", "https://turbo.site/page435552/?utm_source=yadir", "page435552.turbo.site"},

                //t.me
                {"t.me", "https://t.me/", null},
                {"t.me", "https://t.me", null},
                {"t.me", "https://t.me/joinchat_lol", "joinchat-lol.t.me"},
                {"t.me", "https://t.me/joinchat/E8m9kU_YurXNGZl0ochB2A", "e8m9ku-yurxngzl0ochb2a.t.me"},
                {"t.me", "https://t.me/transformatortv", "transformatortv.t.me"},
                {"t.me", "https://t.me/s/khimki_city", "khimki-city.t.me"},

                //zen
                {"zen.yandex.ru", "https://zen.yandex.ru/t/%D0%BF%D0%BE%D1%82%D0%B5%D0%BD%D1%86%D0%B8%D1%8F", null},
                {"zen.yandex.ru", "https://zen.yandex.ru/info/ecom/otzyvy", null},
                {"zen.yandex.ru", "https://zen.yandex.ru/media/", null},
                {"zen.yandex.ru", "https://zen.yandex.ru/", null},
                {"zen.yandex.ru", "https://zen.yandex.ru", null},
                {"zen.yandex.ru", "https://zen.yandex.ru/user/kv9qrb65pd2rrpz9a60gg4pg9r", "kv9qrb65pd2rrpz9a60gg4pg9r.zen.yandex.ru"},
                {"zen.yandex.ru", "https://zen.yandex.ru/user/", null},
                {"zen.yandex.ru", "https://zen.yandex.ru/media1/", "media1.zen.yandex.ru"},
                {"zen.yandex.ru", "https://zen.yandex.ru/media/id", null},
                {"zen.yandex.ru", "https://zen.yandex.ru/profile/editor/", null},
                {"zen.yandex.ru", "https://zen.yandex.ru/profile/editor1/", "profile.zen.yandex.ru"},
                {"zen.yandex.ru", "https://zen.yandex.ru/neoromashka", "neoromashka.zen.yandex.ru"},
                {"m.zen.yandex.ru", "https://m.zen.yandex.ru/neoromashka", "neoromashka.zen.yandex.ru"},
                {"zen.yandex.ru", "https://zen.yandex.com/mfc", "mfc.zen.yandex.ru"},
                {"zen.yandex.ru", "http://zen.yandex.ru/culinaria", "culinaria.zen.yandex.ru"},
                {"zen.yandex.ru", "https://zen.yandex.ru/id/5eb95a1ed4dba80e6f24dbc0",
                        "5eb95a1ed4dba80e6f24dbc0.zen.yandex.ru"},
                {"zen.yandex.ru",
                        "https://zen.yandex.ru/media/id/5ad658032394dfd3dea38052/perfluence-rabotaet-chestno-platit-bystro-provereno-na-sebe-5ebbad119138e30ae87fd836",
                        "5ad658032394dfd3dea38052.zen.yandex.ru"},
                {"zen.yandex.ru", "https://zen.yandex.ru/media/vseprosport2/vyigrai-knigu-olega-romanceva-pravda-obo-mne-i-o-spartake-5dc83153e7b50530845da8d2",
                        "vseprosport2.zen.yandex.ru"},
                {"zen.yandex.ru", "https://zen.yandex.ru/profile/editor/domashnie_pitomcy",
                        "domashnie-pitomcy.zen.yandex.ru"},
                {"zen.yandex.ru", "https://zen.yandex.ru/profile/editor/id/5a96db4b168a91626fe6c5ee",
                        "5a96db4b168a91626fe6c5ee.zen.yandex.ru"},

                //pokupki
                {"pokupki.market.yandex.ru",
                        "https://pokupki.market.yandex.ru/product/kontaktnye-linzy-acuvue-oasys-with-hydraclear-plus" +
                        "-6-linz-r-8-4-d-2-75/100304633955?supplierid=626489&offerid=zV5vRIKFE2JPU45BGAQkmw&",
                        "626489.pokupki.market.yandex.ru"},
                {"pokupki.market.yandex.ru",
                        "https://pokupki.market.yandex.ru/product/kontaktnye-linzy-acuvue-oasys-with-hydraclear-plus" +
                                "-6-linz-r-8-4-d-2-75/100304633955?offerid=zV5vRIKFE2JPU45BGAQkmw&",
                        null},
                {"pokupki.market.yandex.ru",
                        "https://pokupki.market.yandex.ru/product/f/100304633955",
                        null},

                //profi
                {"profi.ru","http://profi.ru", null},
                {"profi.ru","http://profi.ru/", null},
                {"profi.ru","http://profi.ru//////", null},
                {"profi.ru","http://profi.ru//test", null},
                {"profi.ru","http://profi.ru/test?a=1&c=3#ddd", null},
                {"profi.ru","http://profi.ru/test?a=1&profileId=aaa&c=3#ddd", "aaa.profi.ru"}
        });
    }

    @Test
    public void getAggregatorDomain() {
        String actualDomain = extractAggregatorDomainFromHref(domain, href);
        assertThat("Получили ожидаемый домен из href", actualDomain, equalTo(expectedDomain));
    }
}
