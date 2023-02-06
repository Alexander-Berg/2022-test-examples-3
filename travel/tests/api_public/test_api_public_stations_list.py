# coding: utf-8

import mock

from common.models.geo import (
    ExternalDirectionMarker, StationCode, CodeSystem, ExternalDirection, Station, StationType
)
from common.models.transport import TransportType
from common.tester.factories import (
    create_station, create_settlement, create_station_code, create_country, create_region
)
from common.tester.testcase import TestCase
from travel.rasp.tasks.api_public.api_public_stations_list import (
    station_2_dict, generate_xml, generate_json, generate_stations_dict
)


class TestStationListScript(TestCase):
    common_station_attrib = {
        'direction': 'dir_title',
        'latitude': '1.2',
        'longitude': '2.2',
        'title': 'station_title',
        'transport_type': TransportType.objects.get(id=TransportType.TRAIN_ID).L_title(),
        'station_type': StationType.objects.get(id=StationType.TRAIN_STATION_ID).L_title()
    }
    points_attrib = {
        'c1': {'title': 'country'},
        'r1': {'title': 'region'},
        's1': {'title': 'settlement'}
    }
    codes = {
        'esr_code': 'esr_1',
        'yandex_code': 'station_1'
    }
    station = common_station_attrib.copy()
    station['codes'] = codes

    def test_station_2_dict(self):
        station = create_station(title_uk=u'uk_title')
        direction = ExternalDirection.objects.create(full_title=u'f_t', title=u'dir_tilte', title_uk=u'uk_dir_title')
        ExternalDirectionMarker.objects.create(station=station, external_direction=direction, order=0)
        code_system = CodeSystem.objects.get(code='esr')
        esr_codes = StationCode.StationCodeByStationIdGetter(code_system.id, [station.id], default='')

        station_dict = station_2_dict(station, esr_codes, 'ru')
        assert station_dict == {
            'codes': {
                'yandex_code': station.point_key
            },
            'direction': direction.title,
            'latitude': station.latitude,
            'longitude': station.longitude,
            'title': station.title,
            'transport_type': 'bus',
            'station_type': 'bus_stop',
        }

        create_station_code(station=station, system=code_system, code='esr_code')
        esr_codes = StationCode.StationCodeByStationIdGetter(code_system.id, [station.id])
        station_dict = station_2_dict(station, esr_codes, 'uk')
        assert (station_dict['direction'] == direction.title_uk and
                station_dict['title'] == station.title_uk and
                station_dict['codes']['esr_code'] == 'esr_code')

    def test_generate_xml(self):
        stations_dict = {'c1': {'r1': {'s1': [self.station]}}}
        xml_stations = generate_xml(stations_dict, self.points_attrib)
        assert xml_stations.xpath('/response/country/title')[0].text == 'country'
        assert xml_stations.xpath('/response/country/codes/yandex_code')[0].text == 'c1'
        assert xml_stations.xpath('/response/country/region/title')[0].text == 'region'
        assert xml_stations.xpath('/response/country/region/codes/yandex_code')[0].text == 'r1'
        assert xml_stations.xpath('/response/country/region/settlement/title')[0].text == 'settlement'
        assert xml_stations.xpath('/response/country/region/settlement/codes/yandex_code')[0].text == 's1'

        assert xml_stations.xpath('/response/country/region/settlement/station/title')[0].text == 'station_title'
        assert xml_stations.xpath('/response/country/region/settlement/station/direction')[0].text == 'dir_title'
        assert xml_stations.xpath('/response/country/region/settlement/station/longitude')[0].text == '2.2'
        assert xml_stations.xpath('/response/country/region/settlement/station/latitude')[0].text == '1.2'

        assert xml_stations.xpath('/response/country/region/settlement/station/transport_type')[0].text == u'Поезд'
        assert xml_stations.xpath('/response/country/region/settlement/station/station_type')[0].text == u'вокзал'

        assert (
            xml_stations.xpath('/response/country/region/settlement/station/codes/yandex_code')[0].text
            == 'station_1'
        )
        assert xml_stations.xpath('/response/country/region/settlement/station/codes/esr_code')[0].text == 'esr_1'

        stations_dict = {'unknown': {'unknown': {'unknown': [self.station]}}}
        xml_stations = generate_xml(stations_dict, {})
        assert xml_stations.xpath('/response/country/title')[0].text is None
        assert len(xml_stations.xpath('/response/country/codes')) == 1
        assert len(xml_stations.xpath('/response/country/codes/yandex_code')) == 0
        assert xml_stations.xpath('/response/country/region/title')[0].text is None
        assert len(xml_stations.xpath('/response/country/region/codes')) == 1
        assert len(xml_stations.xpath('/response/country/region/codes/yandex_code')) == 0
        assert xml_stations.xpath('/response/country/region/settlement/title')[0].text is None
        assert len(xml_stations.xpath('/response/country/region/settlement/codes')) == 1
        assert len(xml_stations.xpath('/response/country/region/settlement/codes/yandex_code')) == 0

    def test_generate_json(self):
        stations_dict = {'c1': {'r1': {'s1': [self.station]}}}
        json_stations = generate_json(stations_dict, self.points_attrib)

        assert json_stations == {'countries': [
            {'title': 'country',
             'codes': {'yandex_code': 'c1'},
             'regions': [
                 {'title': 'region',
                  'codes': {'yandex_code': 'r1'},
                  'settlements': [
                      {'title': 'settlement',
                       'codes': {'yandex_code': 's1'},
                       'stations': [self.station]}
                  ]}
             ]}
        ]}

        stations_dict = {'unknown': {'unknown': {'unknown': [self.station]}}}
        json_stations = generate_json(stations_dict, {})

        assert json_stations == {'countries': [
            {'title': '',
             'codes': {},
             'regions': [
                 {'title': '',
                  'codes': {},
                  'settlements': [
                      {'title': '',
                       'codes': {},
                       'stations': [self.station]}
                  ]}
             ]}
        ]}

    def test_generate_stations_dict(self):
        country = create_country(title='ru_country_title', title_uk='uk_country_title')
        region = create_region(title='ru_region_title', title_uk='uk_region_title')
        settlement = create_settlement(title='ru_settlement_title', title_uk='uk_settlement_title')
        station = create_station(country=country, region=region, settlement=settlement)

        with mock.patch('travel.rasp.tasks.api_public.api_public_stations_list.station_2_dict') as m_st_2_d:
            station_data = {'station': 'data'}
            m_st_2_d.return_value = station_data

            stations_dict, points_attrib = generate_stations_dict('ru', 'ru')
            assert stations_dict == {
                country.point_key: {
                    region.point_key: {
                        settlement.point_key: [station_data]
                    }
                }
            }
            assert points_attrib == {
                settlement.point_key: {'title': u'ru_settlement_title'},
                country.point_key: {'title': u'ru_country_title'},
                region.point_key: {'title': u'ru_region_title'}
            }
            assert m_st_2_d.call_args_list[0][0][0] == station
            assert m_st_2_d.call_args_list[0][0][2] == 'ru'

            stations_dict, points_attrib = generate_stations_dict('ru', 'uk')
            assert points_attrib == {
                settlement.point_key: {'title': u'uk_settlement_title'},
                country.point_key: {'title': u'uk_country_title'},
                region.point_key: {'title': u'uk_region_title'}
            }
            assert m_st_2_d.call_args_list[1][0][0] == station
            assert m_st_2_d.call_args_list[1][0][2] == 'uk'

            with mock.patch.object(Station, 'translocal_country') as m_tr_country:
                another_country = create_country(title='another_country')
                m_tr_country.return_value = another_country

                stations_dict, points_attrib = generate_stations_dict('uk', 'ru')
                assert stations_dict == {
                    another_country.point_key: {
                        region.point_key: {
                            settlement.point_key: [station_data]
                        }
                    }
                }
                assert points_attrib == {
                    settlement.point_key: {'title': u'ru_settlement_title'},
                    another_country.point_key: {'title': u'another_country'},
                    region.point_key: {'title': u'ru_region_title'}
                }
                assert m_tr_country.call_args_list[0][1] == {'national_version': 'uk'}

            create_station()
            stations_dict, points_attrib = generate_stations_dict('ru', 'ru')
            assert stations_dict == {
                country.point_key: {
                    region.point_key: {
                        settlement.point_key: [station_data]
                    }
                },
                'unknown': {
                    'unknown': {
                        'unknown': [station_data]
                    }
                }
            }
            assert set(points_attrib.keys()) == {country.point_key, region.point_key, settlement.point_key}
