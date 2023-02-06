package ru.yandex.calendar.frontend.ews;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class WindowsTimeZonesTest {

    @Test
    public void conversion() {
        Assert.A.equals("Romance Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Paris")).get());
        Assert.A.equals("Russian Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Moscow")).get());
        Assert.A.equals("FLE Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Tallinn")).get());
        Assert.A.equals("FLE Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Kiev")).get());
        Assert.A.equals("FLE Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Riga")).get());
        Assert.A.equals("FLE Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Sofia")).get());
        Assert.A.equals("FLE Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Vilnius")).get());
        Assert.A.equals("Greenwich Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("GMT")).get()); // !"GMT Standard Time"
        Assert.A.equals("Russia Time Zone 3", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Samara")).get());
        Assert.A.equals("Saratov Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Saratov")).get());
        Assert.A.equals("West Asia Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Indian/Maldives")).get());
        Assert.A.equals("Central European Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Luxembourg")).get());
        Assert.A.equals("Central European Standard Time", WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Warsaw")).get());
        Assert.some(WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Istanbul")));
        Assert.some(WindowsTimeZones.getWinNameByZone(DateTimeZone.forID("Europe/Zurich")));

        Assert.equals(DateTimeZone.forID("Europe/London"), WindowsTimeZones.getZoneByWinName("GMT Standard Time").get());
        Assert.A.equals(DateTimeZone.forID("GMT"), WindowsTimeZones.getZoneByWinName("Greenwich Standard Time").get());
        Assert.A.equals(DateTimeZone.forID("Pacific/Guadalcanal"), WindowsTimeZones.getZoneByWinName("Central Pacific Standard Time").get());
        Assert.A.equals(DateTimeZone.forID("Australia/Perth"), WindowsTimeZones.getZoneByWinName("W. Australia Standard Time").get());
        Assert.A.none(WindowsTimeZones.getZoneByWinName("Moscow Standard Time"));
        Assert.A.none(WindowsTimeZones.getZoneByWinName("MSK"));
    }

    @Test
    public void findByLocalizedNames() {
        // English GMT / UTC
        Assert.A.equals(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(GMT+03:00) Moscow, St. Petersburg, Volgograd").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(UTC+03:00) Moscow, St. Petersburg, Volgograd").get());
        // Russian GMT / UTC
        Assert.A.equals(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(GMT+03:00) Волгоград, Москва, Санкт-Петербург").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(UTC+03:00) Волгоград, Москва, Санкт-Петербург").get());
        // Old exchange events
        Assert.A.equals(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(GMT+03:00) Москва, Санкт-Петербург, Волгоград").get());
        // DST offset
        Assert.A.equals(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(GMT+04:00) Волгоград, Москва, Санкт-Петербург").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(UTC+04:00) Волгоград, Москва, Санкт-Петербург").get());
        // No offset
        Assert.A.equals(DateTimeZone.forID("GMT"), WindowsTimeZones.getZoneByWinName("(GMT) Монровия, Рейкьявик").get());
        Assert.A.equals(DateTimeZone.forID("GMT"), WindowsTimeZones.getZoneByWinName("(UTC) Монровия, Рейкьявик").get());
        // Some others
        Assert.A.equals(DateTimeZone.forID("Asia/Bangkok"), WindowsTimeZones.getZoneByWinName("(UTC+07:00) Бангкок, Джакарта, Ханой").get());

        // https://st.yandex-team.ru/downloadAttachment?key=CAL-6878&attachmentId=609785
        Assert.some(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(UTC+03:00) Moscow, St. Petersburg, Volgograd (RTZ2)"));
        Assert.some(DateTimeZone.forID("Europe/Moscow"), WindowsTimeZones.getZoneByWinName("(UTC+03:00) Волгоград, Москва, Санкт-Петербург (RTZ 2)"));

        //https://st.yandex-team.ru/GREG-1053
        Assert.A.equals(DateTimeZone.forID("Europe/Samara"), WindowsTimeZones.getZoneByWinName("(UTC+04:00) Izhevsk, Samara").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Samara"), WindowsTimeZones.getZoneByWinName("(UTC+04:00) Ижевск, Самара").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Saratov"), WindowsTimeZones.getZoneByWinName("(UTC+04:00) Saratov").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Saratov"), WindowsTimeZones.getZoneByWinName("(UTC+04:00) Саратов").get());

        Assert.A.equals(DateTimeZone.forID("Asia/Yerevan"), WindowsTimeZones.getZoneByWinName("(UTC+04:00) Yerevan").get());
        Assert.A.equals(DateTimeZone.forID("Asia/Yerevan"), WindowsTimeZones.getZoneByWinName("(UTC+04:00) Ереван").get());

        Assert.A.equals(DateTimeZone.forID("Europe/Tallinn"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Kiev"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Riga"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Sofia"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Vilnius"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Helsinki, Kyiv, Riga, Sofia, Tallinn, Vilnius").get());

        Assert.A.equals(DateTimeZone.forID("Europe/Tallinn"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Вильнюс, Киев, Рига, София, Таллин, Хельсинки").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Kiev"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Вильнюс, Киев, Рига, София, Таллин, Хельсинки").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Riga"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Вильнюс, Киев, Рига, София, Таллин, Хельсинки").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Sofia"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Вильнюс, Киев, Рига, София, Таллин, Хельсинки").get());
        Assert.A.equals(DateTimeZone.forID("Europe/Vilnius"), WindowsTimeZones.getZoneByWinName("(UTC+02:00) Вильнюс, Киев, Рига, София, Таллин, Хельсинки").get());
    }

    @Test
    public void doubles() {
        String tzIdForFLE = "Europe/Kiev"; // precedence over "Europe/Helsinki"
        Assert.A.equals(DateTimeZone.forID(tzIdForFLE), WindowsTimeZones.getZoneByWinName("FLE Standard Time").get());
    }
}
