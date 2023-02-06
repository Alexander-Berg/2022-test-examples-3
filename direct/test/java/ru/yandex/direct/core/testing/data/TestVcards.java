package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ru.yandex.direct.core.entity.vcard.model.InstantMessenger;
import ru.yandex.direct.core.entity.vcard.model.Phone;
import ru.yandex.direct.core.entity.vcard.model.PointOnMap;
import ru.yandex.direct.core.entity.vcard.model.PointPrecision;
import ru.yandex.direct.core.entity.vcard.model.PointType;
import ru.yandex.direct.core.entity.vcard.model.Vcard;

import static java.time.LocalDateTime.now;

public class TestVcards {
    public static final int SCALE = 6;
    public static final PointType DEFAULT_POINT_TYPE = PointType.HOUSE;
    public static final PointPrecision DEFAULT_PRECISION = PointPrecision.EXACT;

    // не должен возвращать рандомных полей
    public static Vcard fullVcard() {
        return fullVcard(vcardUserFields(null), null);
    }

    // не должен возвращать рандомных полей
    public static Vcard fullVcard(Long clientUid, Long campaignId) {
        return fullVcard(vcardUserFields(campaignId), clientUid);
    }

    // не должен возвращать рандомных полей
    public static Vcard fullVcard(Vcard userFieldsVcard, Long clientUid) {
        return userFieldsVcard
                .withUid(clientUid)
                .withLastChange(now())
                .withLastDissociation(now())
                .withGeoId(1289L)
                .withAutoPoint(autoPoint())
                .withPointType(DEFAULT_POINT_TYPE)
                .withPrecision(DEFAULT_PRECISION);
    }

    /**
     * Возвращает все поля, которые приходят от пользователя
     *
     * @param campaignId id кампании
     */
    // не должен возвращать рандомных полей
    public static Vcard vcardUserFields(Long campaignId) {
        PointOnMap manualPoint = new PointOnMap()
                .withX(BigDecimal.valueOf(140L).setScale(SCALE, RoundingMode.CEILING))
                .withY(BigDecimal.valueOf(87L).setScale(SCALE, RoundingMode.CEILING))
                .withX1(BigDecimal.valueOf(120L).setScale(SCALE, RoundingMode.CEILING))
                .withY1(BigDecimal.valueOf(80L).setScale(SCALE, RoundingMode.CEILING))
                .withX2(BigDecimal.valueOf(180L).setScale(SCALE, RoundingMode.CEILING))
                .withY2(BigDecimal.valueOf(90L).setScale(SCALE, RoundingMode.CEILING));
        return new Vcard()
                .withCampaignId(campaignId)
                .withCompanyName("my company")
                .withContactPerson("Boss")
                .withEmail("boss@company.com")
                .withPhone(new Phone()
                        .withCountryCode("+7")
                        .withCityCode("812")
                        .withPhoneNumber("777-77-77")
                        .withExtension("123"))
                .withInstantMessenger(new InstantMessenger()
                        .withType("icq")
                        .withLogin("123456789"))
                .withCountry("Россия")
                .withCity("Санкт-Петербург")
                .withStreet("Пискаревский проспект")
                .withHouse("2")
                .withBuild("2")
                .withApart("777")
                .withMetroId(20347L)
                .withManualPoint(manualPoint)
                .withWorkTime("0#3#10#00#18#00;4#6#10#00#11#00")
                .withExtraMessage("good message")
                .withOgrn("5077746977435");
    }

    // не должен возвращать рандомных полей
    public static PointOnMap autoPoint() {
        return new PointOnMap()
                .withX(BigDecimal.valueOf(-170L).setScale(6, RoundingMode.CEILING))
                .withY(BigDecimal.valueOf(-89L).setScale(6, RoundingMode.CEILING))
                .withX1(BigDecimal.valueOf(-175L).setScale(6, RoundingMode.CEILING))
                .withY1(BigDecimal.valueOf(-90L).setScale(6, RoundingMode.CEILING))
                .withX2(BigDecimal.valueOf(-165L).setScale(6, RoundingMode.CEILING))
                .withY2(BigDecimal.valueOf(-80L).setScale(6, RoundingMode.CEILING));
    }
}
