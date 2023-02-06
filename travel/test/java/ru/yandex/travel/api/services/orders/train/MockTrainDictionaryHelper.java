package ru.yandex.travel.api.services.orders.train;

import java.util.ArrayList;

import ru.yandex.travel.api.services.dictionaries.country.CountryDataProvider;
import ru.yandex.travel.api.services.dictionaries.country.MockCountryDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.readable_timezone.MockTrainReadableTimezoneDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.readable_timezone.TrainReadableTimezoneDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.settlement.MockTrainSettlementDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.settlement.TrainSettlementDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station.MockTrainStationDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station.TrainStationDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station_code.MockTrainStationCodeDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station_code.TrainStationCodeDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station_express_alias.MockTrainStationExpressAliasDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.station_express_alias.TrainStationExpressAliasDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.time_zone.MockTrainTimeZoneDataProvider;
import ru.yandex.travel.api.services.dictionaries.train.time_zone.TrainTimeZoneDataProvider;
import ru.yandex.travel.dicts.rasp.proto.ECodeSystem;
import ru.yandex.travel.dicts.rasp.proto.TCountry;
import ru.yandex.travel.dicts.rasp.proto.TReadableTimezone;
import ru.yandex.travel.dicts.rasp.proto.TSettlement;
import ru.yandex.travel.dicts.rasp.proto.TStation;
import ru.yandex.travel.dicts.rasp.proto.TStationCode;
import ru.yandex.travel.dicts.rasp.proto.TStationExpressAlias;
import ru.yandex.travel.dicts.rasp.proto.TTimeZone;

class MockTrainDictionaryHelper {
    static CountryDataProvider countryDataProvider() {
        var countries = new ArrayList<TCountry>();

        countries.add(TCountry.newBuilder().setId(225)
                .setGeoId(225)
                .setTitleDefault("Россия")
                .setCode("RU").build());

        return MockCountryDataProvider.INSTANCE.build(countries);
    }

    static TrainReadableTimezoneDataProvider trainReadableTimezoneDataProvider() {
        var timeZones = new ArrayList<TReadableTimezone>();

        timeZones.add(TReadableTimezone.newBuilder().setLanguage("ru")
                .setKey("by_tz_Europe/Moscow")
                .setValue("по Московскому времени").build());

        return MockTrainReadableTimezoneDataProvider.INSTANCE.build(timeZones);
    }

    static TrainSettlementDataProvider trainSettlementDataProvider() {
        var settlements = new ArrayList<TSettlement>();

        settlements.add(TSettlement.newBuilder().setId(2)
                .setCountryId(225)
                .setRegionId(10174)
                .setGeoId(2)
                .setTitleDefault("Санкт-Петербург").build());

        settlements.add(TSettlement.newBuilder().setId(213)
                .setCountryId(225)
                .setRegionId(1)
                .setGeoId(213)
                .setTitleDefault("Москва").build());

        settlements.add(TSettlement.newBuilder().setId(54)
                .setCountryId(225)
                .setRegionId(11162)
                .setGeoId(54)
                .setTitleDefault("Екатеринбург").build());

        return MockTrainSettlementDataProvider.INSTANCE.build(settlements);
    }

    static TrainStationDataProvider trainStationDataProvider() {
        var stations = new ArrayList<TStation>();

        stations.add(TStation.newBuilder().setId(9612620)
                .setTitleDefault("Москва")
                .setTitleRuPreposition("в")
                .setIsHidden(false)
                .setTimeZoneId(1)
                .setRailwayTimeZoneId(1)
                .setSettlementId(213)
                .setCountryId(225).build());

        stations.add(TStation.newBuilder().setId(9607404)
                .setTitleDefault("Санкт-Петербург")
                .setTitleRuPreposition("в")
                .setIsHidden(false)
                .setTimeZoneId(1)
                .setRailwayTimeZoneId(1)
                .setSettlementId(2)
                .setCountryId(225).build());

        return MockTrainStationDataProvider.INSTANCE.build(stations);
    }

    static TrainStationCodeDataProvider trainStationCodeDataProvider() {
        var stationCodes = new ArrayList<TStationCode>();

        stationCodes.add(TStationCode.newBuilder().setStationId(9612620)
                .setCode("2064110")
                .setSystemId(ECodeSystem.CODE_SYSTEM_EXPRESS).build());

        stationCodes.add(TStationCode.newBuilder().setStationId(9612620)
                .setCode("2064110")
                .setSystemId(ECodeSystem.CODE_SYSTEM_BUSCOMUA).build());

        stationCodes.add(TStationCode.newBuilder().setStationId(9607404)
                .setCode("2064001")
                .setSystemId(ECodeSystem.CODE_SYSTEM_EXPRESS).build());

        return MockTrainStationCodeDataProvider.INSTANCE.build(stationCodes);
    }

    static TrainStationExpressAliasDataProvider trainStationExpressAliasDataProvider() {
        var stationCodes = new ArrayList<TStationExpressAlias>();

        stationCodes.add(TStationExpressAlias.newBuilder().setStationId(9612620)
                .setAlias("МОСКВА ЯР").build());

        stationCodes.add(TStationExpressAlias.newBuilder().setStationId(9612620)
                .setAlias("С ПЕТЕРБУРГ ГЛАВН").build());

        return MockTrainStationExpressAliasDataProvider.INSTANCE.build(stationCodes);
    }

    static TrainTimeZoneDataProvider trainTimeZoneDataProvider() {
        var timeZones = new ArrayList<TTimeZone>();

        timeZones.add(TTimeZone.newBuilder().setId(1).setCode("Europe/Moscow").build());

        return MockTrainTimeZoneDataProvider.INSTANCE.build(timeZones);
    }
}
