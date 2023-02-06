package ru.yandex.crypta.search.custom_audience;

import java.text.MessageFormat;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.crypta.audience.proto.TAge;
import ru.yandex.crypta.audience.proto.TCity;
import ru.yandex.crypta.audience.proto.TCountry;
import ru.yandex.crypta.audience.proto.TDevice;
import ru.yandex.crypta.audience.proto.TGender;
import ru.yandex.crypta.audience.proto.TIncome;
import ru.yandex.crypta.common.exception.WrongRequestException;
import ru.yandex.crypta.siberia.bin.custom_audience.common.proto.TCaRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ParamsParserTest {
    @Test
    public void allFieldsGivenOnce() {
        var args = "gender=male income=B1 income=B2 age=0-17 age=25-34 device=desktop device=phone country=russia country=turkey city=moscow city=saint-petersburg region=213 region=214 host=yandex.ru word=lol segment=217/13 affinitive_site=google.com top_common_site=auto.ru";
        var result = new ParamsParser().parseArgs(args);

        var builder = TCaRule.newBuilder()
                .addAffinitiveSites("google.com")
                .addHosts("yandex.ru")
                .addWords("lol")
                .addTopCommonSites("auto.ru")
                .setGender(TGender.MALE)
                .addIncomes(TIncome.INCOME_B1)
                .addIncomes(TIncome.INCOME_B2)
                .addAges(TAge.FROM_0_TO_17)
                .addAges(TAge.FROM_25_TO_34)
                .addDevices(TDevice.DESKTOP)
                .addDevices(TDevice.PHONE)
                .addCountries(TCountry.RUSSIA)
                .addCountries(TCountry.TURKEY)
                .addCities(TCity.MOSCOW)
                .addCities(TCity.SAINT_PETERSBURG)
                .addRegions(213)
                .addRegions(214);

        builder.addSegmentsBuilder().setKeyword(217).setID(13);
        var reference = builder.build();

        assertEquals(reference, result);
    }

    @Test
    public void repeatedFieldsGivenMultipleTimes() {
        var args = "host=a.ru host=b.ru host=c.ru word=d word=e segment=217/13 segment=217/14 affinitive_site=google.com affinitive_site=youtube.com top_common_site=auto.ru top_common_site=ru.wikipedia.org";
        var result = ParamsParser.parseArgs(args);

        var builder = TCaRule.newBuilder()
                .addHosts("a.ru")
                .addHosts("b.ru")
                .addHosts("c.ru")
                .addWords("d")
                .addWords("e")
                .addTopCommonSites("auto.ru")
                .addTopCommonSites("ru.wikipedia.org")
                .addAffinitiveSites("google.com")
                .addAffinitiveSites("youtube.com");

        builder.addSegmentsBuilder().setKeyword(217).setID(13);
        builder.addSegmentsBuilder().setKeyword(217).setID(14);
        var reference = builder.build();

        assertEquals(reference, result);
    }

    @Test
    public void multipleSingleValues() {
        for (var arg: new String[]{
                "gender=male",
        }) {
            var args = MessageFormat.format("{0} {0}", arg);
            Assert.assertThrows(WrongRequestException.class, () -> ParamsParser.parseArgs(args));
        }
    }

    @Test
    public void invalidValues() {
        for (var arg: new String[]{
                "gender=m",
                "income=rich",
                "age=150+",
                "device=stone_tablet",
                "has_crypta_id=hz",
                "country=usa",
                "city=kazan",
                "segment=segment",
        }) {
            Assert.assertThrows(WrongRequestException.class, () -> ParamsParser.parseArgs(arg));
        }
    }

    @Test
    public void invalidKeys() {
        for (var arg: new String[]{
                "a=b",
                "gender=male a=b",
        }) {
            Assert.assertThrows(WrongRequestException.class, () -> ParamsParser.parseArgs(arg));
        }
    }

    @Test
    public void nullResponse() {
        for (var arg: new String[]{
                "",
                " ",
        }) {
            assertNull(ParamsParser.parseArgs(arg));
        }
    }
}
