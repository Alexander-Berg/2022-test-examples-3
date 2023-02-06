# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest
from django.utils.encoding import force_text
from hamcrest import assert_that, contains_inanyorder, has_properties

from travel.avia.admin.avia_scripts.sync_with_rasp.sync_www_station_with_rasp import sync_stations, USE_DIRECTION_MAPPING
from travel.avia.library.python.common.models.geo import Station, StationCode, CodeSystem
from travel.avia.library.python.tester.factories import create_station, create_translated_title, create_translated_text, create_district

pytestmark = [pytest.mark.dbuser]

IATA_CODE_SYSTEM_ID = 4


def test_sync_www_station(rasp_repositories):
    Station.objects.all().delete()

    station_in_rasp = create_station(id=100)
    station_not_in_rasp = create_station(id=500500)

    rasp_station = rasp_repositories.create_station(Id=station_in_rasp.id, StationCodes={IATA_CODE_SYSTEM_ID: 'XXX'})
    rasp_new_station = rasp_repositories.create_station(Id=102, StationCodes={IATA_CODE_SYSTEM_ID: 'QQQ'})

    sync_stations(rasp_repositories.station_repository)

    db_stations = list(Station.objects.all())
    assert len(db_stations) == 3
    assert_that(db_stations, contains_inanyorder(
        has_properties(id=station_not_in_rasp.id),
        _has_properties_by_rasp_station(rasp_station),
        _has_properties_by_rasp_station(rasp_new_station),
    ))


def _has_properties_by_rasp_station(rasp_station):
    return has_properties(
        id=rasp_station.Id,
        address=force_text(rasp_station.LocalAddress),
        title=force_text(rasp_station.LocalTitle),
        popular_title=force_text(rasp_station.LocalPopularTitle),
        majority_id=rasp_station.Majority,
        settlement_id=rasp_station.SettlementId or None,
        region_id=rasp_station.RegionId or None,
        country_id=rasp_station.CountryId or None,
        time_zone=rasp_station.TimeZoneCode,
        t_type_id=rasp_station.TransportType,
        type_choices=rasp_station.TypeChoices,
        hidden=rasp_station.IsHidden,
        express_id=rasp_station.StationCodes[2] or None,
        sirena_id=rasp_station.StationCodes[1] or None,
        time_zone_not_check=rasp_station.TimeZoneNotCheck,
        site_url=rasp_station.SiteUrl,
        not_generalize=rasp_station.NotGeneralize,
        station_type_id=rasp_station.Type,
        longitude=rasp_station.Longitude,
        latitude=rasp_station.Latitude,
        map_zoom=rasp_station.MapZoom,
        use_direction=USE_DIRECTION_MAPPING[rasp_station.UseDirection] or None,
        has_aeroexpress=rasp_station.HasAeroexpress,
        near_metro=rasp_station.NearMetro,
        photo=rasp_station.Photo,
        schema_image=rasp_station.SchemaImage,
        panorama_url=rasp_station.PanoramaUrl,
        show_settlement=rasp_station.ShowSettlement,
        tablo_state=rasp_station.TabloState,
        tablo_state_prev=rasp_station.TabloStatePrev,
        fuzzy_only=rasp_station.FuzzyOnly,
        virtual_end=rasp_station.VirtualEnd,
        incomplete_bus_schedule=rasp_station.IncompleteBusSchedule,
        show_mode=rasp_station.ShowMode,
        is_fuzzy=rasp_station.IsFuzzy,
        is_searchable_to=rasp_station.IsSearchableTo,
        is_searchable_from=rasp_station.IsSearchableFrom,
        in_station_schedule=rasp_station.InStationSchedule,
        in_thread=rasp_station.InThread,
        show_tablo_stat=rasp_station.ShowTabloStat,
        title_ru_override=rasp_station.ShouldOverrideTitle.Ru,
        title_en_override=rasp_station.ShouldOverrideTitle.En,
        title_uk_override=rasp_station.ShouldOverrideTitle.Uk,
        title_tr_override=rasp_station.ShouldOverrideTitle.Tr,
        title_ru_preposition_v_vo_na=force_text(rasp_station.TitleRuPreposition),
        new_L_popular_title=has_properties(
            ru_nominative=force_text(rasp_station.PopularTitle.Ru),
            en_nominative=force_text(rasp_station.PopularTitle.En),
            uk_nominative=force_text(rasp_station.PopularTitle.Uk),
            tr_nominative=force_text(rasp_station.PopularTitle.Tr),
        ),
        new_L_title=has_properties(
            ru_nominative=force_text(rasp_station.Title.Ru),
            ru_genitive=force_text(rasp_station.TitleRuGenitiveCase),
            ru_accusative=force_text(rasp_station.TitleRuAccusativeCase),
            ru_locative=force_text(rasp_station.TitleRuPrepositionalCase),
            en_nominative=force_text(rasp_station.Title.En),
            uk_nominative=force_text(rasp_station.Title.Uk),
            tr_nominative=force_text(rasp_station.Title.Tr),
        ),
        new_L_how_to_get_to_city=has_properties(
            ru=force_text(rasp_station.HowToGetToCity.Ru),
            en=force_text(rasp_station.HowToGetToCity.En),
            uk=force_text(rasp_station.HowToGetToCity.Uk),
            tr=force_text(rasp_station.HowToGetToCity.Tr),
        ),
        new_L_address=has_properties(
            ru=force_text(rasp_station.Address.Ru),
            en=force_text(rasp_station.Address.En),
            uk=force_text(rasp_station.Address.Uk),
            tr=force_text(rasp_station.Address.Tr),
        ),
    )


def test_move_iata_code_to_other_station(rasp_repositories):
    Station.objects.all().delete()
    _old_station = create_station(id=500500, __={'codes': {'iata': 'QQQ'}})  # noqa: F841
    _rasp_old_station = rasp_repositories.create_station(Id=500500, StationCodes={IATA_CODE_SYSTEM_ID: 'XXX'})  # noqa: F841
    rasp_new_station = rasp_repositories.create_station(Id=300300, StationCodes={IATA_CODE_SYSTEM_ID: 'QQQ'})

    sync_stations(rasp_repositories.station_repository)

    assert Station.objects.all().count() == 2
    assert StationCode.objects.get(system_id=IATA_CODE_SYSTEM_ID, code='QQQ').station_id == rasp_new_station.Id


def test_swap_sirena_id(rasp_repositories):
    sirena_system_id = CodeSystem.objects.get(code='sirena').id

    Station.objects.all().delete()

    create_station(id=1, sirena_id='1')
    create_station(id=2, sirena_id='2')

    rasp_repositories.create_station(Id=1, StationCodes={sirena_system_id: '2'})
    rasp_repositories.create_station(Id=2, StationCodes={sirena_system_id: '1'})

    sync_stations(rasp_repositories.station_repository)

    assert Station.objects.all().count() == 2
    assert Station.objects.get(id=1).sirena_id == '2'
    assert Station.objects.get(id=2).sirena_id == '1'


def test_swap_express_id(rasp_repositories):
    express_system_id = CodeSystem.objects.get(code='express').id

    Station.objects.all().delete()

    create_station(id=1, express_id='1')
    create_station(id=2, express_id='2')

    rasp_repositories.create_station(Id=1, StationCodes={express_system_id: '2', IATA_CODE_SYSTEM_ID: 'XXX'})
    rasp_repositories.create_station(Id=2, StationCodes={express_system_id: '1', IATA_CODE_SYSTEM_ID: 'QQQ'})

    sync_stations(rasp_repositories.station_repository)

    assert Station.objects.all().count() == 2
    assert Station.objects.get(id=1).express_id == '2'
    assert Station.objects.get(id=2).express_id == '1'


def test_save_not_rasp_fields(rasp_repositories):
    Station.objects.all().delete()

    create_station(
        id=1,
        district=create_district(title='My strange district title'),
        new_L_title_id=create_translated_title(de_nominative='My strange title').id,
        new_L_popular_title_id=create_translated_title(de_nominative='My strange title 2').id,
        new_L_address_id=create_translated_text(de='My strange address').id,
        new_L_how_to_get_to_city_id=create_translated_text(de='My strange how to get to city').id,
    )
    rasp_repositories.create_station(Id=1)

    sync_stations(rasp_repositories.station_repository)

    assert Station.objects.all().count() == 1
    station = Station.objects.get(id=1)
    assert station.district.title == 'My strange district title'
    assert station.new_L_title.de_nominative == 'My strange title'
    assert station.new_L_popular_title.de_nominative == 'My strange title 2'
    assert station.new_L_address.de == 'My strange address'
    assert station.new_L_how_to_get_to_city.de == 'My strange how to get to city'
