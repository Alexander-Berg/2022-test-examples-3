package ru.yandex.direct.api.v5.entity.vcards.converter;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.vcards.MapPoint;
import com.yandex.direct.api.v5.vcards.ObjectFactory;
import com.yandex.direct.api.v5.vcards.VCardGetItem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.vcard.model.InstantMessenger;
import ru.yandex.direct.core.entity.vcard.model.Phone;
import ru.yandex.direct.core.entity.vcard.model.PointOnMap;
import ru.yandex.direct.core.entity.vcard.model.Vcard;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class VCardConverterTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();
    private static final Long VCARD_ID = 1L;
    private static final Long CAMPAIGN_ID = 1L;
    private static final String COMPANY_NAME = "Яндекс";
    private static final String OGRN = "ОГРН";
    private static final String WORK_TIME_INT = "08#13#14#20";
    private static final String WORK_TIME_EXT = "08;13;14;20";
    private static final Phone PHONE =
            new Phone().withCountryCode("+7").withCityCode("495").withPhoneNumber("739-70-00").withExtension("0001");
    private static final String CONTACT_PERSON = "Аркадий Волож";
    private static final String CONTACT_EMAIL = "imperator@ya.ru";
    private static final InstantMessenger MESSENGER = new InstantMessenger().withType("skype").withLogin("vasya");
    private static final com.yandex.direct.api.v5.vcards.InstantMessenger MESSENGER_ITEM =
            new com.yandex.direct.api.v5.vcards.InstantMessenger().withMessengerClient("skype")
                    .withMessengerLogin("vasya");
    private static final String COUNTRY = "Россия";
    private static final String CITY = "Кимры";
    private static final Long METRO_ID = 1L;
    private static final String STREET = "50 лет ВЛКСМ";
    private static final String HOUSE = "15";
    private static final String BUILDING = "б";
    private static final String APARTMENT = "12";
    private static final String MESSAGE = "Hey, there!";
    private static final BigDecimal ONE = BigDecimal.valueOf(1L);
    private static final BigDecimal TWO = BigDecimal.valueOf(2L);
    private static final BigDecimal THREE = BigDecimal.valueOf(3L);
    private static final BigDecimal FOUR = BigDecimal.valueOf(4L);
    private static final BigDecimal FIVE = BigDecimal.valueOf(5L);
    private static final BigDecimal SIX = BigDecimal.valueOf(6L);
    private static final PointOnMap AUTO_POINT =
            new PointOnMap().withX(TWO).withY(TWO).withX1(ONE).withY1(ONE).withX2(THREE).withY2(THREE);
    private static final PointOnMap MANUAL_POINT =
            new PointOnMap().withX(FIVE).withY(FIVE).withX1(FOUR).withY1(FOUR).withX2(SIX).withY2(SIX);
    private static final MapPoint AUTO_POINT_ITEM =
            new MapPoint().withX(TWO).withY(TWO).withX1(ONE).withY1(ONE).withX2(THREE).withY2(THREE);
    private static final MapPoint MANUAL_POINT_ITEM =
            new MapPoint().withX(FIVE).withY(FIVE).withX1(FOUR).withY1(FOUR).withX2(SIX).withY2(SIX);
    @Parameterized.Parameter
    @SuppressWarnings("unused")
    public String description;
    @Parameterized.Parameter(1)
    public Vcard vcard;
    @Parameterized.Parameter(2)
    public VCardGetItem expectedItem;
    @Parameterized.Parameter(3)
    public BeanFieldPath path;
    private VCardConverter converter = new VCardConverter();

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"конвертация id (null)",
                        new Vcard().withId(null),
                        new VCardGetItem().withId(null),
                        newPath("Id")},
                {"конвертация id",
                        new Vcard().withId(VCARD_ID),
                        new VCardGetItem().withId(VCARD_ID),
                        newPath("Id")},
                {"конвертация campaign id (null)",
                        new Vcard().withCampaignId(null),
                        new VCardGetItem().withCampaignId(null),
                        newPath("CampaignId")},
                {"конвертация campaign id",
                        new Vcard().withCampaignId(CAMPAIGN_ID),
                        new VCardGetItem().withCampaignId(CAMPAIGN_ID),
                        newPath("CampaignId")},
                {"конвертация company name (null)",
                        new Vcard().withCompanyName(null),
                        new VCardGetItem().withCompanyName(FACTORY.createVCardGetItemCompanyName(null)),
                        newPath("CompanyName")},
                {"конвертация company name",
                        new Vcard().withCompanyName(COMPANY_NAME),
                        new VCardGetItem().withCompanyName(FACTORY.createVCardGetItemCompanyName(COMPANY_NAME)),
                        newPath("CompanyName")},
                {"конвертация OGRN (null)",
                        new Vcard().withOgrn(null),
                        new VCardGetItem().withOgrn(FACTORY.createVCardGetItemOgrn(null)),
                        newPath("Ogrn")},
                {"конвертация OGRN",
                        new Vcard().withOgrn(OGRN),
                        new VCardGetItem().withOgrn(FACTORY.createVCardGetItemOgrn(OGRN)),
                        newPath("Ogrn")},
                {"конвертация work time (null)",
                        new Vcard().withWorkTime(null),
                        new VCardGetItem().withWorkTime(null),
                        newPath("WorkTime")},
                {"конвертация work time",
                        new Vcard().withWorkTime(WORK_TIME_INT),
                        new VCardGetItem().withWorkTime(WORK_TIME_EXT),
                        newPath("WorkTime")},
                {"конвертация phone (null)",
                        new Vcard().withPhone(null),
                        new VCardGetItem().withPhone(FACTORY.createPhone()),
                        newPath("Phone")},
                {"конвертация phone",
                        new Vcard().withPhone(PHONE),
                        new VCardGetItem().withPhone(FACTORY.createPhone().withCountryCode(PHONE.getCountryCode())
                                .withCityCode(PHONE.getCityCode()).withPhoneNumber(PHONE.getPhoneNumber())
                                .withExtension(PHONE.getExtension())),
                        newPath("Phone")},
                {"конвертация contact person (null)",
                        new Vcard().withContactPerson(null),
                        new VCardGetItem().withContactPerson(FACTORY.createVCardGetItemContactPerson(null)),
                        newPath("ContactPerson")},
                {"конвертация contact person",
                        new Vcard().withContactPerson(CONTACT_PERSON),
                        new VCardGetItem().withContactPerson(FACTORY.createVCardGetItemContactPerson(CONTACT_PERSON)),
                        newPath("ContactPerson")},
                {"конвертация contact email (null)",
                        new Vcard().withEmail(null),
                        new VCardGetItem().withContactEmail(FACTORY.createVCardGetItemContactEmail(null)),
                        newPath("ContactEmail")},
                {"конвертация contact person",
                        new Vcard().withEmail(CONTACT_EMAIL),
                        new VCardGetItem().withContactEmail(FACTORY.createVCardGetItemContactEmail(CONTACT_EMAIL)),
                        newPath("ContactEmail")},
                {"конвертация instant messenger (null)",
                        new Vcard().withInstantMessenger(null),
                        new VCardGetItem().withInstantMessenger(FACTORY.createVCardGetItemInstantMessenger(null)),
                        newPath("InstantMessenger")},
                {"конвертация instant messenger",
                        new Vcard().withInstantMessenger(MESSENGER),
                        new VCardGetItem().withInstantMessenger(
                                FACTORY.createVCardGetItemInstantMessenger(MESSENGER_ITEM)),
                        newPath("InstantMessenger")},
                {"конвертация country (null)",
                        new Vcard().withCountry(null),
                        new VCardGetItem().withCountry(null),
                        newPath("Country")},
                {"конвертация country",
                        new Vcard().withCountry(COUNTRY),
                        new VCardGetItem().withCountry(COUNTRY),
                        newPath("Country")},
                {"конвертация city (null)",
                        new Vcard().withCountry(null),
                        new VCardGetItem().withCountry(null),
                        newPath("City")},
                {"конвертация city",
                        new Vcard().withCity(CITY),
                        new VCardGetItem().withCity(CITY),
                        newPath("City")},
                {"конвертация metro station id (null)",
                        new Vcard().withMetroId(null),
                        new VCardGetItem().withMetroStationId(FACTORY.createVCardGetItemMetroStationId(null)),
                        newPath("MetroStationId")},
                {"конвертация metro station id",
                        new Vcard().withMetroId(METRO_ID),
                        new VCardGetItem().withMetroStationId(FACTORY.createVCardGetItemMetroStationId(METRO_ID)),
                        newPath("MetroStationId")},
                {"конвертация street (null)",
                        new Vcard().withStreet(null),
                        new VCardGetItem().withStreet(FACTORY.createVCardGetItemStreet(null)),
                        newPath("Street")},
                {"конвертация street",
                        new Vcard().withStreet(STREET),
                        new VCardGetItem().withStreet(FACTORY.createVCardGetItemStreet(STREET)),
                        newPath("Street")},
                {"конвертация house (null)",
                        new Vcard().withHouse(null),
                        new VCardGetItem().withHouse(FACTORY.createVCardGetItemHouse(null)),
                        newPath("House")},
                {"конвертация house",
                        new Vcard().withHouse(HOUSE),
                        new VCardGetItem().withHouse(FACTORY.createVCardGetItemHouse(HOUSE)),
                        newPath("House")},
                {"конвертация building (null)",
                        new Vcard().withBuild(null),
                        new VCardGetItem().withBuilding(FACTORY.createVCardGetItemBuilding(null)),
                        newPath("Building")},
                {"конвертация building",
                        new Vcard().withBuild(BUILDING),
                        new VCardGetItem().withBuilding(FACTORY.createVCardGetItemBuilding(BUILDING)),
                        newPath("Building")},
                {"конвертация apartment (null)",
                        new Vcard().withApart(null),
                        new VCardGetItem().withApartment(FACTORY.createVCardGetItemApartment(null)),
                        newPath("Apartment")},
                {"конвертация apartment",
                        new Vcard().withApart(APARTMENT),
                        new VCardGetItem().withApartment(FACTORY.createVCardGetItemApartment(APARTMENT)),
                        newPath("Apartment")},
                {"конвертация point on map (no auto or manual)",
                        new Vcard().withAutoPoint(null).withManualPoint(null),
                        new VCardGetItem().withPointOnMap(FACTORY.createVCardGetItemPointOnMap(null)),
                        newPath("PointOnMap")},
                {"конвертация point on map (auto)",
                        new Vcard().withAutoPoint(AUTO_POINT).withManualPoint(null),
                        new VCardGetItem().withPointOnMap(FACTORY.createVCardGetItemPointOnMap(AUTO_POINT_ITEM)),
                        newPath("PointOnMap")},
                {"конвертация point on map (manual)",
                        new Vcard().withAutoPoint(null).withManualPoint(MANUAL_POINT),
                        new VCardGetItem().withPointOnMap(FACTORY.createVCardGetItemPointOnMap(MANUAL_POINT_ITEM)),
                        newPath("PointOnMap")},
                {"конвертация extra message (null)",
                        new Vcard().withExtraMessage(null),
                        new VCardGetItem().withExtraMessage(FACTORY.createVCardGetItemExtraMessage(null)),
                        newPath("ExtraMessage")},
                {"конвертация extra message",
                        new Vcard().withExtraMessage(MESSAGE),
                        new VCardGetItem().withExtraMessage(FACTORY.createVCardGetItemExtraMessage(MESSAGE)),
                        newPath("ExtraMessage")},
        });
    }

    @Test
    public void test() {
        VCardGetItem actualItem = converter.convert(vcard);
        assertThat(actualItem, beanDiffer(expectedItem).useCompareStrategy(DefaultCompareStrategies.onlyFields(path)));
    }
}
