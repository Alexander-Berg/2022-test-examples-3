# coding: utf-8

import pytest

from common.models.geo import ExternalDirection, ExternalDirectionMarker, Settlement, Country
from common.models.transport import TransportType

from common.tester.factories import create_suburban_zone, create_settlement, create_region
from common.tester.utils.django_cache import replace_django_cache

from travel.rasp.export.tests.v3.factories import create_station
from travel.rasp.export.tests.v3.helpers import api_get_json


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


@pytest.fixture()
def zone_cache():
    with replace_django_cache('default') as cache:
        try:
            yield cache
        finally:
            cache.clear()


class TestSuburbanZone(object):
    def test_add_station_codes(self, zone_cache):
        zone = create_suburban_zone(id=1, settlement_id=Settlement.MOSCOW_ID, code=1)
        ext_dir = ExternalDirection.objects.create(suburban_zone=zone, full_title='t_d_1', title='t_1', code=1)
        station = create_station(id=111, type_choices='suburban', __={'codes': {'esr': 'esr_1'}})
        ExternalDirectionMarker.objects.create(external_direction=ext_dir, station=station, order=0)

        response = api_get_json('/v3/suburban/zone/{}'.format(zone.id))
        stations = response['zone_stations']
        assert response['id'] == 1
        assert len(stations) == 1
        assert stations[0]['esr'] == 'esr_1'
        assert stations[0]['yandex_rasp_code'] == 111

    def test_different_zones(self, zone_cache):
        stations = [create_station(id=111, type_choices='suburban', __={'codes': {'esr': 'esr_1'}}),
                    create_station(id=222, type_choices='suburban', __={'codes': {'esr': 'esr_2'}}),
                    create_station(id=333)]
        zones = []

        for i in range(1, 4):
            zones.append(create_suburban_zone(id=i, settlement_id=Settlement.MOSCOW_ID, code=i))
            ext_dir = ExternalDirection.objects.create(suburban_zone=zones[i - 1], full_title='t_d_{}'.format(i),
                                                       title='t_{}'.format(i), code=i, id=100 + i)
            ExternalDirectionMarker.objects.create(external_direction=ext_dir, station=stations[i - 1], order=0)

        response = api_get_json('/v3/suburban/zone/{}'.format(zones[0].id))
        stations = response['zone_stations']
        assert response['id'] == 1
        assert len(stations) == 1
        assert stations[0]['esr'] == 'esr_1'
        assert stations[0]['direction_id'] == 101

        response = api_get_json('/v3/suburban/zone/{}'.format(zones[1].id))
        stations = response['zone_stations']
        assert response['id'] == 2
        assert len(stations) == 1
        assert stations[0]['esr'] == 'esr_2'
        assert stations[0]['direction_id'] == 102

        response = api_get_json('/v3/suburban/zone/{}'.format(zones[2].id))
        stations = response['zone_stations']
        assert response['id'] == 3
        assert len(stations) == 0

    def test_need_direction(self, zone_cache):
        def get_sett(esr, stations):
            for station in stations:
                if station['esr'] == esr:
                    return station

        zone = create_suburban_zone(id=1, settlement_id=Settlement.MOSCOW_ID, code=1)
        region = create_region(title='region_1')
        region_2 = create_region(title='region_2')
        train_type = TransportType.objects.get(id=TransportType.TRAIN_ID)
        suburban_type = TransportType.objects.get(id=TransportType.SUBURBAN_ID)
        stations = [create_station(id=511, t_type=train_type, type_choices='suburban', title='same_title',
                                   region=region, suburban_zone=zone, __={'codes': {'esr': 'esr_1'}}),
                    create_station(id=512, t_type=suburban_type, type_choices='suburban', title='same_title',
                                   region=region, suburban_zone=zone, __={'codes': {'esr': 'esr_2'}}),
                    create_station(id=513, t_type=train_type, type_choices='suburban', title='not_same_title',
                                   region=region, suburban_zone=zone, __={'codes': {'esr': 'esr_3'}}),
                    create_station(id=514, t_type=suburban_type, type_choices='suburban', title='same_title',
                                   region=region_2, suburban_zone=zone, __={'codes': {'esr': 'esr_4'}}),
                    ]
        ext_dir = ExternalDirection.objects.create(suburban_zone=zone, full_title='dir_f_t', title='dir_t')
        for i, station in enumerate(stations):
            ExternalDirectionMarker.objects.create(external_direction=ext_dir, station=station, order=i)

        response = api_get_json('/v3/suburban/zone/{}'.format(zone.id))
        stations = response['zone_stations']
        assert len(stations) == 4
        assert get_sett('esr_1', stations)['need_direction'] is True
        assert get_sett('esr_1', stations)['title'] == 'same_title'
        assert get_sett('esr_2', stations)['need_direction'] is True
        assert get_sett('esr_2', stations)['title'] == 'same_title'
        assert get_sett('esr_3', stations)['need_direction'] is False
        assert get_sett('esr_3', stations)['title'] == 'not_same_title'
        assert get_sett('esr_4', stations)['need_direction'] is False
        assert get_sett('esr_4', stations)['title'] == 'same_title'

    def test_station_not_only_suburban(self, zone_cache):
        zone = create_suburban_zone(id=1, settlement_id=Settlement.MOSCOW_ID, code=1)
        ext_dir1 = ExternalDirection.objects.create(suburban_zone=zone, full_title='t_d_1', title='t_1', code=1)
        ext_dir2 = ExternalDirection.objects.create(suburban_zone=zone, full_title='t_d_2', title='t_2', code=2)
        station1 = create_station(id=111, type_choices='train', __={'codes': {'esr': 'esr_1'}})
        station2 = create_station(id=222, type_choices='aeroex', __={'codes': {'esr': 'esr_2'}})
        station3 = create_station(id=333, type_choices='train,suburban', __={'codes': {'esr': 'esr_3'}})

        ExternalDirectionMarker.objects.create(external_direction=ext_dir1, station=station1, order=0)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir1, station=station2, order=1)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir2, station=station3, order=2)

        response = api_get_json('/v3/suburban/zone/{}'.format(zone.id))
        assert response['id'] == 1
        stations = response['zone_stations']
        assert len(stations) == 3
        assert stations[0]['esr'] == 'esr_1'
        assert stations[0]['yandex_rasp_code'] == 111
        assert stations[0]['direction'] == 't_d_1'
        assert stations[1]['esr'] == 'esr_2'
        assert stations[1]['yandex_rasp_code'] == 222
        assert stations[1]['direction'] == 't_d_1'
        assert stations[2]['esr'] == 'esr_3'
        assert stations[2]['yandex_rasp_code'] == 333
        assert stations[2]['direction'] == 't_d_2'

    def test_404(self, zone_cache):
        response = api_get_json('/v3/suburban/zone/{}'.format(666), response_status_code=404)
        assert response['error']['text'] == 'No SuburbanZone matches the given query.'
        assert response['error']['status_code'] == 404


class TestSuburbanZones(object):
    def test_add_station_codes(self, zone_cache):
        def get_sett(title, setts):
            for sett in setts:
                if sett['title'] == title:
                    return sett

        zone = create_suburban_zone(id=1, settlement_id=Settlement.MOSCOW_ID, code=1)
        region = create_region(title='region_1')
        create_settlement(
            id=101, title='sett_1', title_ru_genitive='sett_1_g',
            suburban_zone=zone, region=region, country=Country.objects.get(id=Country.RUSSIA_ID),
        )
        create_settlement(id=102, title='sett_2', suburban_zone=zone)

        response = api_get_json('/v3/suburban/zones/')
        response_zone = response['suburban_zones'][0]
        assert response_zone['id'] == 1
        assert hasattr(response_zone, 'majority') is False
        settlements = response_zone['settlements']
        assert len(settlements) == 2
        assert get_sett('sett_1', settlements) == {
            'title': 'sett_1',
            'title_genitive': 'sett_1_g',
            'country': u'Россия',
            'country_code': 'RU',
            'region': 'region_1',
            'longitude': 1.0,
            'latitude': 1.0,
            'geo_id': None,
            'id': 101,
        }

        assert get_sett('sett_2', settlements) == {
            'title': 'sett_2',
            'title_genitive': None,
            'country': None,
            'country_code': None,
            'region': None,
            'longitude': 1.0,
            'latitude': 1.0,
            'geo_id': None,
            'id': 102,
        }

    def test_zones_by_direction_stations(self, zone_cache):
        zone = create_suburban_zone(id=1, settlement_id=Settlement.MOSCOW_ID, code=1, title='zone_1')
        create_settlement(id=102, title='sett_2', suburban_zone=zone)

        # При использовании флага use_directions
        # привязка городов к зонам не учитывается.
        response = api_get_json('/v3/suburban/zones/', {'use_directions': True})
        response_zone = response['suburban_zones'][0]
        assert response_zone['id'] == 1
        assert hasattr(response_zone, 'settlements') is False

        ext_dir = ExternalDirection.objects.create(suburban_zone=zone, full_title='t_d_1', title='t_1', code=1)
        settlement = create_settlement(id=103, title='sett_1')
        station = create_station(settlement=settlement)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir, station=station, order=0)

        # Города с дефолтным значением (no) поля
        # use_in_suburban_app_suggests не учитываются.
        response = api_get_json('/v3/suburban/zones/', {'use_directions': True})
        response_zone = response['suburban_zones'][0]
        settlements = response_zone['settlements']
        assert len(settlements) == 1
        assert settlements[0] == {
            'title': 'sett_1',
            'title_genitive': None,
            'country': None,
            'country_code': None,
            'region': None,
            'longitude': 1.0,
            'latitude': 1.0,
            'geo_id': None,
            'id': 103,
            'use_in_suburban_app_suggests': 'no',
        }

        settlement.use_in_suburban_app_suggests = 'yes'
        settlement.save()

        # Города со значением поля yes учитываются.
        response = api_get_json('/v3/suburban/zones/', {'use_directions': True})
        response_zone = response['suburban_zones'][0]
        assert response_zone['majority'] == 10
        settlements = response_zone['settlements']
        assert len(settlements) == 1
        assert settlements[0]['use_in_suburban_app_suggests'] == 'yes'

        settlement.use_in_suburban_app_suggests = 'all_railway_stations'
        settlement.save()

        # При значении поля all_railway_stations добавляется
        # приписка "(все вокзалы)" к названию.
        response = api_get_json('/v3/suburban/zones/', {'use_directions': True})
        response_zone = response['suburban_zones'][0]
        settlements = response_zone['settlements']
        assert response_zone['settlements'][0]['title'] == u'sett_1 (все вокзалы)'
        assert settlements[0]['use_in_suburban_app_suggests'] == 'all_railway_stations'

        # Проверяем города двух направлений,
        # которые относятся к одной зоне.
        ext_dir_2 = ExternalDirection.objects.create(suburban_zone=zone, full_title='t_d_2', title='t_2', code=2)
        settlement_2 = create_settlement(id=104, title='sett_2', use_in_suburban_app_suggests='yes')
        station_2 = create_station(settlement=settlement_2)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir_2, station=station_2, order=0)
        response = api_get_json('/v3/suburban/zones/', {'use_directions': True})
        response_zone = response['suburban_zones'][0]
        assert {sett['title'] for sett in response_zone['settlements']} == {u'sett_1 (все вокзалы)', 'sett_2'}

        # Добавляем еще одну зону c еще одним направлением.
        zone_2 = create_suburban_zone(id=2, settlement_id=Settlement.MOSCOW_ID, code=2, title='zone_2')
        ext_dir_3 = ExternalDirection.objects.create(suburban_zone=zone_2, full_title='t_d_3', title='t_3', code=4)
        settlement_3 = create_settlement(id=105, title='sett_3', use_in_suburban_app_suggests='yes')
        station_3 = create_station(settlement=settlement_3)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir_3, station=station_3, order=0)
        ExternalDirectionMarker.objects.create(external_direction=ext_dir_3, station=station_2, order=1)
        response = api_get_json('/v3/suburban/zones/', {'use_directions': True})
        response_zone = response['suburban_zones'][1]
        assert {sett['title'] for sett in response_zone['settlements']} == {'sett_2', 'sett_3'}
