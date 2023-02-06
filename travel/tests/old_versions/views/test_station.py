# -*- coding: utf-8 -*-

from lxml import etree
from django.test.client import Client

from common.tester.testcase import TestCase
from common.tester.factories import create_station


class TestStationInfo(TestCase):
    def setUp(self):
        self.client = Client()
        self.esr_code = '123'
        self.stops_data = [
            {
                'name': u'Станция {}'.format(i),
                'lang': 'ru-RU',
                'distance': i * 100.0,
                'color': '#fffff{}'.format(i),
                'coords': {
                    'latitude': float(i),
                    'longitude': float(i + 3),
                },
            }
            for i in range(1, 3)
        ]

        create_station(
            __={
                'codes': {'esr': self.esr_code},
                'phones': [
                    {'phone': '+79121111111'},
                    {'phone': '+79121111112'},
                ],
            },
            address=u'Ленина 13, 7',
            latitude=1.0,
            longitude=2.0,
        )

    def test_valid_info(self):
        self.check_valid('/export/suburban/station/{}/info/')

    def test_valid_v2_info(self):
        self.check_valid('/export/v2/suburban/station/{}/info/')

    def check_valid(self, url):
        response = self.client.get(url.format(self.esr_code))

        assert response.status_code == 200
        station_xml = etree.fromstring(response.content)

        assert station_xml.attrib['address'] == u'Ленина 13, 7'
        assert station_xml.attrib['latitude'] == u'1.0'
        assert station_xml.attrib['longitude'] == u'2.0'

        phones = sorted(el.text for el in station_xml.xpath('phones/phone'))
        assert phones == ['+79121111111', '+79121111112']
