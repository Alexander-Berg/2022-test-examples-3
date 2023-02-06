package ru.yandex.direct.web.testing.data;

import ru.yandex.direct.web.entity.banner.model.WebBannerVcard;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;

public class TestBannerVcards {

    private TestBannerVcards() {
    }

    public static WebBannerVcard randomHouseWebVcard() {
        return new WebBannerVcard()
                .withName("my company")
                .withContactPerson("Boss")
                .withContactEmail("boss@company.com")
                .withCountryCode("+7")
                .withCityCode("812")
                .withPhone("777-77-77")
                .withImClient("icq")
                .withImLogin("123456789")
                .withCountry("Россия")
                .withCity("Санкт-Петербург")
                .withStreet("Пискаревский проспект")
                .withHouse(randomNumeric(7))
                .withBuild("2")
                .withApart("777")
                .withMetro(20347L)
                .withManualPoint("37.617530,55.755446")
                .withManualBounds("37.614069,55.752683,37.622280,55.757313")
                .withWorkTime("0#3#10#00#18#00;4#6#10#00#11#00")
                .withExtraMessage("good message")
                .withOgrn("5077746977435");
    }
}
