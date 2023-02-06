package ru.yandex.direct.core.entity.campaign.converter;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal;
import ru.yandex.direct.core.entity.vcard.model.InstantMessenger;
import ru.yandex.direct.core.entity.vcard.model.Phone;
import ru.yandex.direct.core.entity.vcard.model.PointOnMap;
import ru.yandex.direct.core.entity.vcard.model.PointPrecision;
import ru.yandex.direct.core.entity.vcard.model.Vcard;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class CampaignConverterTest {

    public static final String COMPANY_NAME = RandomStringUtils.randomAlphabetic(10);
    public static final String CONTACT_PERSON = RandomStringUtils.randomAlphabetic(10);
    public static final String WORK_TIME = "0#4#10#00#18#15";
    public static final String CITY_CODE = "343";
    public static final String COUNTRY_CODE = "+7";
    public static final String PHONE = "3762214";
    public static final String EMAIL = "12312@ya.ru";
    public static final String MESSENGER_NAME = "icq";
    public static final String MESSENGER_LOGIN = "11111";
    public static final String EXTRA_MESSAGE = RandomStringUtils.randomAlphabetic(160) + " " +
            RandomStringUtils.randomAlphabetic(10) + " " +
            RandomStringUtils.randomAlphabetic(10);
    public static final String EXT = "133";
    public static final String ORG_DETAILS_ID = "844388";
    public static final String OGRN = "1027700132195";
    public static final String COUNTRY = "Россия";
    public static final String CITY = "Екатеринбург";
    public static final String STREET = "горького";
    public static final String HOUSE = "11";
    public static final String BUILD = "1";
    public static final String AUTO_X1 = "60.60114";
    public static final String AUTO_Y1 = "56.841128";
    public static final String AUTO_X2 = "60.60935";
    public static final String AUTO_Y2 = "56.845628";
    public static final String MANUAL_X1 = "60.5980209006274";
    public static final String MANUAL_Y1 = "56.84095851159816";
    public static final String MANUAL_X2 = "60.61145340337394";
    public static final String MANUAL_Y2 = "56.84566231797192";
    public static final String AUTO_X = "60.605245";
    public static final String AUTO_Y = "56.843378";
    public static final String MANUAL_X = "60.606425171966315";
    public static final String MANUAL_Y = "56.843472075741744";
    public static final String AUTO_PRECISION = "near";
    private static final String CONTACT_INFO_FROM_DB_WITH_OGRN;
    private static final String CONTACT_INFO_FROM_DB_WITHOUT_OGRN;
    private static final String CONTACT_INFO_FROM_DB_WITHOUT_POINTS;
    private static final String CONTACT_INFO_TO_DB;
    private static final Vcard VCARD;
    private static final Vcard VCARD_WITHOUT_POINTS;

    static {
        CONTACT_INFO_FROM_DB_WITH_OGRN = "---\n" +
                "name: " + COMPANY_NAME + "\n" +
                "worktime: " + WORK_TIME + "\n" +
                "contactperson: " + CONTACT_PERSON + "\n" +
                "city_code: " + CITY_CODE + "\n" +
                "country_code: " + COUNTRY_CODE + "\n" +
                "phone: " + PHONE + "\n" +
                "contact_email: " + EMAIL + "\n" +
                "im_client: " + MESSENGER_NAME + "\n" +
                "im_login: " + MESSENGER_LOGIN + "\n" +
                "extra_message: " + EXTRA_MESSAGE + "\n" +
                "ext: " + EXT + "\n" +
                "org_details_id: " + ORG_DETAILS_ID + "\n" +
                "ogrn: " + OGRN + "\n" +
                "geo_id: ~\n" +
                "country: " + COUNTRY + "\n" +
                "city: " + CITY + "\n" +
                "street: " + STREET + "\n" +
                "house: " + HOUSE + "\n" +
                "build: " + BUILD + "\n" +
                "apart: ~\n" +
                "metro: ~\n" +
                "auto_bounds: " + AUTO_X1 + "," + AUTO_Y1 + "," + AUTO_X2 + "," + AUTO_Y2 + "\n" +
                "manual_bounds: " + MANUAL_X1 + "," + MANUAL_Y1 + "," + MANUAL_X2 + "," + MANUAL_Y2 + "\n" +
                "auto_point: " + AUTO_X + "," + AUTO_Y + "\n" +
                "manual_point: " + MANUAL_X + "," + MANUAL_Y + "\n" +
                "point_type: ~\n" +
                "auto_precision: " + AUTO_PRECISION + "\n";

        CONTACT_INFO_FROM_DB_WITHOUT_POINTS = "---\n" +
                "name: " + COMPANY_NAME + "\n" +
                "worktime: " + WORK_TIME + "\n" +
                "contactperson: " + CONTACT_PERSON + "\n" +
                "city_code: " + CITY_CODE + "\n" +
                "country_code: " + COUNTRY_CODE + "\n" +
                "phone: " + PHONE + "\n" +
                "contact_email: " + EMAIL + "\n" +
                "im_client: " + MESSENGER_NAME + "\n" +
                "im_login: " + MESSENGER_LOGIN + "\n" +
                "extra_message: " + EXTRA_MESSAGE + "\n" +
                "ext: " + EXT + "\n" +
                "org_details_id: " + ORG_DETAILS_ID + "\n" +
                "ogrn: " + OGRN + "\n" +
                "geo_id: ~\n" +
                "country: " + COUNTRY + "\n" +
                "city: " + CITY + "\n" +
                "street: " + STREET + "\n" +
                "house: " + HOUSE + "\n" +
                "build: " + BUILD + "\n" +
                "apart: ~\n" +
                "metro: ~\n" +
                "auto_bounds: ~\n" +
                "manual_bounds: ~\n" +
                "auto_point: ~\n" +
                "manual_point: ~\n" +
                "point_type: ~\n" +
                "auto_precision: ~\n";

        CONTACT_INFO_FROM_DB_WITHOUT_OGRN = "---\n" +
                "name: " + COMPANY_NAME + "\n" +
                "worktime: " + WORK_TIME + "\n" +
                "contactperson: " + CONTACT_PERSON + "\n" +
                "city_code: " + CITY_CODE + "\n" +
                "country_code: " + COUNTRY_CODE + "\n" +
                "phone: " + PHONE + "\n" +
                "contact_email: " + EMAIL + "\n" +
                "im_client: " + MESSENGER_NAME + "\n" +
                "im_login: " + MESSENGER_LOGIN + "\n" +
                "extra_message: " + EXTRA_MESSAGE + "\n" +
                "ext: " + EXT + "\n" +
                "org_details_id: " + ORG_DETAILS_ID + "\n" +
                "ogrn_num: " + OGRN + "\n" +
                "geo_id: ~\n" +
                "country: " + COUNTRY + "\n" +
                "city: " + CITY + "\n" +
                "street: " + STREET + "\n" +
                "house: " + HOUSE + "\n" +
                "build: " + BUILD + "\n" +
                "apart: ~\n" +
                "metro: ~\n" +
                "auto_bounds: " + AUTO_X1 + "," + AUTO_Y1 + "," + AUTO_X2 + "," + AUTO_Y2 + "\n" +
                "manual_bounds: " + MANUAL_X1 + "," + MANUAL_Y1 + "," + MANUAL_X2 + "," + MANUAL_Y2 + "\n" +
                "auto_point: " + AUTO_X + "," + AUTO_Y + "\n" +
                "manual_point: " + MANUAL_X + "," + MANUAL_Y + "\n" +
                "point_type: ~\n" +
                "auto_precision: " + AUTO_PRECISION + "\n";

        CONTACT_INFO_TO_DB = "---\n" +
                "name: " + COMPANY_NAME + "\n" +
                "worktime: " + WORK_TIME + "\n" +
                "contactperson: " + CONTACT_PERSON + "\n" +
                "city_code: " + CITY_CODE + "\n" +
                "country_code: " + COUNTRY_CODE + "\n" +
                "phone: " + PHONE + "\n" +
                "contact_email: " + EMAIL + "\n" +
                "im_client: " + MESSENGER_NAME + "\n" +
                "im_login: " + MESSENGER_LOGIN + "\n" +
                "extra_message: " + EXTRA_MESSAGE + "\n" +
                "ext: " + EXT + "\n" +
                "org_details_id: " + ORG_DETAILS_ID + "\n" +
                "ogrn: " + OGRN + "\n" +
                "geo_id: ~\n" +
                "country: " + COUNTRY + "\n" +
                "city: " + CITY + "\n" +
                "street: " + STREET + "\n" +
                "house: " + HOUSE + "\n" +
                "build: " + BUILD + "\n" +
                "apart: ~\n" +
                "metro: ~\n" +
                "auto_bounds: " + AUTO_X1 + "," + AUTO_Y1 + "," + AUTO_X2 + "," + AUTO_Y2 + "\n" +
                "manual_bounds: " + MANUAL_X1 + "," + MANUAL_Y1 + "," + MANUAL_X2 + "," + MANUAL_Y2 + "\n" +
                "auto_point: " + AUTO_X + "," + AUTO_Y + "\n" +
                "manual_point: " + MANUAL_X + "," + MANUAL_Y + "\n" +
                "point_type: ~\n" +
                "auto_precision: " + AUTO_PRECISION + "\n";

        VCARD = new Vcard()
                .withAutoPoint(new PointOnMap()
                        .withX(new BigDecimal(AUTO_X))
                        .withY(new BigDecimal(AUTO_Y))
                        .withX1(new BigDecimal(AUTO_X1))
                        .withY1(new BigDecimal(AUTO_Y1))
                        .withX2(new BigDecimal(AUTO_X2))
                        .withY2(new BigDecimal(AUTO_Y2)))
                .withPrecision(PointPrecision.NEAR)
                .withBuild(BUILD)
                .withCity(CITY)
                .withCountry(COUNTRY)
                .withPhone(new Phone()
                        .withCountryCode(COUNTRY_CODE)
                        .withCityCode(CITY_CODE)
                        .withExtension(EXT)
                        .withPhoneNumber(PHONE))
                .withExtraMessage(EXTRA_MESSAGE)
                .withHouse(HOUSE)
                .withInstantMessenger(new InstantMessenger().withLogin(MESSENGER_LOGIN)
                        .withType(MESSENGER_NAME))
                .withManualPoint(new PointOnMap()
                        .withX(new BigDecimal(MANUAL_X))
                        .withY(new BigDecimal(MANUAL_Y))
                        .withX1(new BigDecimal(MANUAL_X1))
                        .withY1(new BigDecimal(MANUAL_Y1))
                        .withX2(new BigDecimal(MANUAL_X2))
                        .withY2(new BigDecimal(MANUAL_Y2)))
                .withCompanyName(COMPANY_NAME)
                .withContactPerson(CONTACT_PERSON)
                .withOgrn(OGRN)
                .withOrgDetailsId(844388L)
                .withStreet(STREET)
                .withWorkTime(WORK_TIME)
                .withEmail(EMAIL);

        VCARD_WITHOUT_POINTS = new Vcard()
                .withBuild(BUILD)
                .withCity(CITY)
                .withCountry(COUNTRY)
                .withPhone(new Phone()
                        .withCountryCode(COUNTRY_CODE)
                        .withCityCode(CITY_CODE)
                        .withExtension(EXT)
                        .withPhoneNumber(PHONE))
                .withExtraMessage(EXTRA_MESSAGE)
                .withHouse(HOUSE)
                .withInstantMessenger(new InstantMessenger().withLogin(MESSENGER_LOGIN)
                        .withType(MESSENGER_NAME))
                .withCompanyName(COMPANY_NAME)
                .withContactPerson(CONTACT_PERSON)
                .withOgrn(OGRN)
                .withOrgDetailsId(844388L)
                .withStreet(STREET)
                .withWorkTime(WORK_TIME)
                .withEmail(EMAIL);
    }

    @Test
    public void vcardFromDbWithOgrn() {
        Vcard actualVcard = CampaignConverter.vcardFromDb(CONTACT_INFO_FROM_DB_WITH_OGRN);

        assertThat(actualVcard).isEqualTo(VCARD);
    }

    @Test
    public void vcardFromDbWithoutOgrn() {
        Vcard actualVcard = CampaignConverter.vcardFromDb(CONTACT_INFO_FROM_DB_WITHOUT_OGRN);

        assertThat(actualVcard).isEqualTo(VCARD);
    }

    @Test
    public void vcardToDb() {
        String actualVcardDbView = CampaignConverter.vcardToDb(VCARD);

        assertThat(actualVcardDbView).isEqualTo(CONTACT_INFO_TO_DB);
    }


    @Test
    public void vcardWithoutPointsToDb() {
        String actualVcardDbView = CampaignConverter.vcardToDb(VCARD_WITHOUT_POINTS);

        assertThat(actualVcardDbView).isEqualTo(CONTACT_INFO_FROM_DB_WITHOUT_POINTS);
    }

    @Test
    public void meaningfulGoalsToDb_NullMetrikaSourceOfValue_NotSerialize() {

        MeaningfulGoal meaningfulGoal = new MeaningfulGoal()
                .withGoalId(1L)
                .withConversionValue(BigDecimal.TEN);
        String meaningfulGoalsToDb = CampaignConverter.meaningfulGoalsToDb(List.of(meaningfulGoal), false);
        assertThat(meaningfulGoalsToDb).doesNotContain("is_metrika_source_of_value");
    }
}
