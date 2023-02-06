# -*- coding: utf-8 -*-

from django.test.client import Client
from lxml import etree

from common.models.geo import ExternalDirection, ExternalDirectionMarker, Settlement, Country
from common.models.transport import TransportType

from common.tester.testcase import TestCase
from common.tester.factories import create_station, create_suburban_zone, create_settlement, create_region


class TestSuburbanZone(TestCase):
    client = Client()

    def get_xml_and_stations(self, url, kwargs=None):
        response = self.client.get(url, kwargs)
        zone_xml = etree.fromstring(response.content)
        stations = zone_xml.findall('station')
        return zone_xml, stations

    def test_add_station_codes(self):
        zone = create_suburban_zone(id=1, settlement_id=Settlement.MOSCOW_ID, code=1)
        ext_dir = ExternalDirection.objects.create(suburban_zone=zone, full_title='t_d_1', title='t_1', code=1)
        station = create_station(id=111, type_choices='suburban', __={'codes': {'esr': 'esr_1'}})
        ExternalDirectionMarker.objects.create(external_direction=ext_dir, station=station, order=0)

        zone_xml, stations = self.get_xml_and_stations('/export/suburban/zones/{}'.format(zone.id),
                                                       {'add_station_codes': 'true'})
        assert zone_xml.tag == 'suburban_zone'
        assert zone_xml.attrib['id'] == '1'
        assert len(stations) == 1
        assert stations[0].attrib['esr'] == 'esr_1'
        assert stations[0].attrib['yandex_rasp_code'] == '111'

        zone_xml, stations = self.get_xml_and_stations('/export/suburban/zones/{}'.format(zone.id))
        assert stations[0].attrib.get('yandex_rasp_code') is None

    def test_different_zones(self):
        stations = [create_station(id=111, type_choices='suburban', __={'codes': {'esr': 'esr_1'}}),
                    create_station(id=222, type_choices='suburban', __={'codes': {'esr': 'esr_2'}}),
                    create_station(id=333)]
        zones = []

        for i in range(1, 4):
            zones.append(create_suburban_zone(id=i, settlement_id=Settlement.MOSCOW_ID, code=i))
            ext_dir = ExternalDirection.objects.create(suburban_zone=zones[i - 1], full_title='t_d_{}'.format(i),
                                                       title='t_{}'.format(i), code=i)
            ExternalDirectionMarker.objects.create(external_direction=ext_dir, station=stations[i - 1], order=0)

        zone_xml, stations = self.get_xml_and_stations('/export/suburban/zones/{}'.format(zones[0].id))
        assert zone_xml.tag == 'suburban_zone'
        assert zone_xml.attrib['id'] == '1'
        assert len(stations) == 1
        assert stations[0].attrib['esr'] == 'esr_1'

        zone_xml, stations = self.get_xml_and_stations('/export/suburban/zones/{}'.format(zones[1].id))
        assert zone_xml.attrib['id'] == '2'
        assert len(stations) == 1
        assert stations[0].attrib['esr'] == 'esr_2'

        zone_xml, stations = self.get_xml_and_stations('/export/suburban/zones/{}'.format(zones[2].id))
        assert zone_xml.attrib['id'] == '3'
        assert len(stations) == 0


class TestSuburbanZoneV2(TestCase):
    client = Client()

    def get_xml_and_stations(self, url, kwargs=None):
        response = self.client.get(url, kwargs)
        zone_xml = etree.fromstring(response.content)
        stations = zone_xml.findall('station')
        return zone_xml, stations

    def test_add_station_codes(self):
        zone = create_suburban_zone(id=1, settlement_id=Settlement.MOSCOW_ID, code=1)
        ext_dir = ExternalDirection.objects.create(suburban_zone=zone, full_title='t_d_1', title='t_1', code=1)
        station = create_station(id=111, type_choices='suburban', __={'codes': {'esr': 'esr_1'}})
        ExternalDirectionMarker.objects.create(external_direction=ext_dir, station=station, order=0)

        zone_xml, stations = self.get_xml_and_stations('/export/v2/suburban/zones/{}'.format(zone.id),
                                                       {'add_station_codes': 'true'})
        assert zone_xml.tag == 'suburban_zone'
        assert zone_xml.attrib['id'] == '1'
        assert len(stations) == 1
        assert stations[0].attrib['esr'] == 'esr_1'
        assert stations[0].attrib['yandex_rasp_code'] == '111'

        zone_xml, stations = self.get_xml_and_stations('/export/suburban/zones/{}'.format(zone.id))
        assert stations[0].attrib.get('yandex_rasp_code') is None

    def test_different_zones(self):
        stations = [create_station(id=111, type_choices='suburban', __={'codes': {'esr': 'esr_1'}}),
                    create_station(id=222, type_choices='suburban', __={'codes': {'esr': 'esr_2'}}),
                    create_station(id=333)]
        zones = []

        for i in range(1, 4):
            zones.append(create_suburban_zone(id=i, settlement_id=Settlement.MOSCOW_ID, code=i))
            ext_dir = ExternalDirection.objects.create(suburban_zone=zones[i - 1], full_title='t_d_{}'.format(i),
                                                       title='t_{}'.format(i), code=i)
            ExternalDirectionMarker.objects.create(external_direction=ext_dir, station=stations[i - 1], order=0)

        zone_xml, stations = self.get_xml_and_stations('/export/v2/suburban/zones/{}'.format(zones[0].id))
        assert zone_xml.tag == 'suburban_zone'
        assert zone_xml.attrib['id'] == '1'
        assert len(stations) == 1
        assert stations[0].attrib['esr'] == 'esr_1'

        zone_xml, stations = self.get_xml_and_stations('/export/v2/suburban/zones/{}'.format(zones[1].id))
        assert zone_xml.attrib['id'] == '2'
        assert len(stations) == 1
        assert stations[0].attrib['esr'] == 'esr_2'

        zone_xml, stations = self.get_xml_and_stations('/export/v2/suburban/zones/{}'.format(zones[2].id))
        assert zone_xml.attrib['id'] == '3'
        assert len(stations) == 0

    def test_need_direction(self):
        def get_sett(esr, stations):
            for station in stations:
                if station.attrib['esr'] == esr:
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

        zone_xml, stations = self.get_xml_and_stations('/export/v2/suburban/zones/{}'.format(zone.id))
        assert zone_xml.tag == 'suburban_zone'
        assert len(stations) == 4
        assert get_sett('esr_1', stations).attrib['need_direction'] == 'true'
        assert get_sett('esr_1', stations).attrib['title'] == 'same_title'
        assert get_sett('esr_2', stations).attrib['need_direction'] == 'true'
        assert get_sett('esr_2', stations).attrib['title'] == 'same_title'
        assert get_sett('esr_3', stations).attrib['need_direction'] == 'false'
        assert get_sett('esr_3', stations).attrib['title'] == 'not_same_title'
        assert get_sett('esr_4', stations).attrib['need_direction'] == 'false'
        assert get_sett('esr_4', stations).attrib['title'] == 'same_title'


class TestSuburbanZonesV2(TestCase):
    client = Client()

    def test_add_station_codes(self):
        def get_sett(title, setts):
            for sett in setts:
                if sett.attrib['title'] == title:
                    return sett

        zone = create_suburban_zone(id=1, settlement_id=Settlement.MOSCOW_ID, code=1)
        region = create_region(title='region_1')
        create_settlement(id=101, title='sett_1', suburban_zone=zone, region=region, country=Country.objects.get(id=Country.RUSSIA_ID))
        create_settlement(id=102, title='sett_2', suburban_zone=zone)

        response = self.client.get('/export/v2/suburban/zones/')
        xml_tree = etree.fromstring(response.content)
        assert xml_tree.tag == 'suburban_zones'
        assert xml_tree.xpath('/suburban_zones/zone')[0].attrib['id'] == '1'
        settlements = xml_tree.xpath("/suburban_zones/zone/settlement")
        assert len(settlements) == 2
        assert get_sett('sett_1', settlements).attrib == {'title': 'sett_1',
                                                          'country': u'Россия',
                                                          'region': 'region_1',
                                                          'longitude': '1.0',
                                                          'latitude': '1.0',
                                                          'geo_id': '',
                                                          'id': '101'}

        assert get_sett('sett_2', settlements).attrib == {'title': 'sett_2',
                                                          'country': '',
                                                          'region': '',
                                                          'longitude': '1.0',
                                                          'latitude': '1.0',
                                                          'geo_id': '',
                                                          'id': '102'}
