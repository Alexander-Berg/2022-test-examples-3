package ru.yandex.direct.web.entity.banner.converter;

import java.math.BigDecimal;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.direct.core.entity.vcard.model.InstantMessenger;
import ru.yandex.direct.core.entity.vcard.model.Phone;
import ru.yandex.direct.core.entity.vcard.model.PointOnMap;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.web.entity.banner.model.WebBannerVcard;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.SimpleConversionMatcher.converts;
import static ru.yandex.direct.web.entity.banner.converter.BannerVcardConverter.toCoreVcard;

public class BannerVcardConverterTest {

    private static final String VALID_X = "37.617530";
    private static final String VALID_Y = "55.755446";
    private static final String VALID_X1 = "37.614069";
    private static final String VALID_X2 = "55.752683";
    private static final String VALID_Y1 = "37.622280";
    private static final String VALID_Y2 = "55.757313";

    private static final String VALID_POINT = String.format("%s,%s", VALID_X, VALID_Y);
    private static final String VALID_BOUNDS =
            String.format("%s,%s,%s,%s", VALID_X1, VALID_X2, VALID_Y1, VALID_Y2);

    @Test
    public void convertPrimitives() {
        Set<String> fieldsNotToFill = ImmutableSet.of("manualPoint", "manualBounds",
                "imClient", "imLogin", "countryCode", "cityCode", "phone", "ext", "extraMessage");
        assertThat(BannerVcardConverter::toCoreVcard,
                converts(new WebBannerVcard(), fieldsNotToFill));
    }

    @Test
    public void convertBlankStringsToNull() {
        WebBannerVcard webVcard = new WebBannerVcard()
                .withCountry(" ")
                .withCity(" ")
                .withStreet(" ")
                .withHouse(" ")
                .withBuild(" ")
                .withApart(" ")
                .withExtraMessage(" ")
                .withName(" ")
                .withOgrn(" ")
                .withContactPerson(" ")
                .withContactEmail(" ")
                .withWorkTime(" ");
        assertThat(toCoreVcard(webVcard), beanDiffer(new Vcard()));
    }

    @Test
    public void convertPhone() {
        WebBannerVcard webVcard = new WebBannerVcard()
                .withCountryCode("+7")
                .withCityCode("812")
                .withPhone("777-77-77")
                .withExt("444");

        Phone expected = new Phone()
                .withCountryCode("+7")
                .withCityCode("812")
                .withPhoneNumber("777-77-77")
                .withExtension("444");

        Phone actual = toCoreVcard(webVcard).getPhone();

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void convertPartialPhone() {
        WebBannerVcard vcardWeb = new WebBannerVcard().withExt("123");

        Phone expected = new Phone()
                .withExtension("123");

        assertThat(toCoreVcard(vcardWeb).getPhone(), beanDiffer(expected));
    }

    @Test
    public void convertNullPhone() {
        WebBannerVcard vcardWeb = new WebBannerVcard().withCity("city");
        assertThat(toCoreVcard(vcardWeb).getPhone(), nullValue());
    }

    @Test
    public void convertBlankPhone() {
        WebBannerVcard vcardWeb = new WebBannerVcard()
                .withCity("city")
                .withCountryCode(" ")
                .withCityCode(" ")
                .withPhone(" ")
                .withExt(" ");
        assertThat(toCoreVcard(vcardWeb).getPhone(), nullValue());
    }

    @Test
    public void convertInstantMessenger() {
        WebBannerVcard webVcard = new WebBannerVcard()
                .withImClient("Skype")
                .withImLogin("abc1");

        InstantMessenger expected = new InstantMessenger()
                .withType("Skype")
                .withLogin("abc1");

        InstantMessenger actual = toCoreVcard(webVcard).getInstantMessenger();

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void convertPartialInstantMessenger() {
        WebBannerVcard webVcard = new WebBannerVcard()
                .withImLogin("abc1");

        InstantMessenger expected = new InstantMessenger()
                .withLogin("abc1");

        InstantMessenger actual = toCoreVcard(webVcard).getInstantMessenger();

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void convertBlankInstantMessenger() {
        WebBannerVcard vcardWeb = new WebBannerVcard()
                .withCity("city")
                .withImClient(" ")
                .withImLogin(" ");
        assertThat(toCoreVcard(vcardWeb).getInstantMessenger(), nullValue());
    }

    @Test
    public void convertNullInstantMessenger() {
        WebBannerVcard vcardWeb = new WebBannerVcard().withCity("city");
        assertThat(toCoreVcard(vcardWeb).getInstantMessenger(), nullValue());
    }

    @Test
    public void convertManualPoint() {
        WebBannerVcard webVcard = new WebBannerVcard()
                .withManualPoint(VALID_POINT)
                .withManualBounds(VALID_BOUNDS);

        PointOnMap expected = new PointOnMap()
                .withX(new BigDecimal(VALID_X))
                .withY(new BigDecimal(VALID_Y))
                .withX1(new BigDecimal(VALID_X1))
                .withY1(new BigDecimal(VALID_X2))
                .withX2(new BigDecimal(VALID_Y1))
                .withY2(new BigDecimal(VALID_Y2));

        PointOnMap actual = toCoreVcard(webVcard).getManualPoint();

        assertThat(actual, beanDiffer(expected));
    }

    @Test
    public void convertBlankManualPoint() {
        WebBannerVcard webVcard = new WebBannerVcard()
                .withCity("city")
                .withManualPoint(" ")
                .withManualBounds(" ");
        assertThat(toCoreVcard(webVcard).getManualPoint(), nullValue());
    }

    @Test
    public void convertNullManualPoint() {
        WebBannerVcard webVcard = new WebBannerVcard().withCity("city");
        assertThat(toCoreVcard(webVcard).getManualPoint(), nullValue());
    }

    @Test
    public void replacesNewLineAndTabSymbolsInExtraMessage() {
        WebBannerVcard webVcard = new WebBannerVcard()
                .withExtraMessage("extra\nmessage\tcontaining\ntabs and new\tlines");
        assertThat(toCoreVcard(webVcard).getManualPoint(), nullValue());
    }
}
