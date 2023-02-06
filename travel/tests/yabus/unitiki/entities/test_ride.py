# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import hamcrest
import mock
import pytest

from travel.proto.dicts.buses.carrier_pb2 import TCarrier

from yabus.common.pointconverter import PointConverter
from yabus.common.entities.ride import Ride as BaseRide
from yabus.unitiki.entities.ride import Ride


class TestFee(object):
    @pytest.mark.parametrize('obj, expected', (
        (
            {'price': 150, 'tariff': 100, 'tariff_currency': 1},
            50
        ),
        (
            {'price': 150, 'tariff': 100, 'tariff_currency': 5},
            0
        ),
        (
            {'price': 100, 'tariff': 100, 'tariff_currency': 1},
            0
        ),
        (
            {'price': 100},
            0
        ),
        (
            {'tariff': 100},
            0
        ),
        (
            {},
            0
        ),
    ))
    def test_output(self, obj, expected):
        assert Ride.Fee('price', 'tariff', 'tariff_currency').output(None, obj) == pytest.approx(expected)


class TestRide(object):
    @mock.patch('yabus.unitiki.entities.ride.Ride.carrier_matcher')
    def test_some(self, m_carrier_matcher):
        carrier_model = TCarrier(id=1, name="carrier")
        m_carrier_matcher.get_carrier = mock.Mock(return_value=carrier_model)

        with mock.patch.object(PointConverter, 'backmap') as m_converter:
            def fake_backmap(supplier_id):
                if supplier_id == 's9306':
                    return 'rasp_from_id'
                if supplier_id == 's78065':
                    return 'rasp_to_id'
                return 'Please mock me'

            m_converter.side_effect = fake_backmap
            ride = dict(Ride.init({
                "bus": {
                    "has_air_conditioning": None,
                    "has_tv": None,
                    "number": None,
                    "place_cnt": None,
                    "title": "Toyota Alphard"
                },
                "bus_images": None,
                "buy_place_cnt_max": "5",
                "can_be_annulated": "1",
                "carrier_rating": None,
                "carrier_title": "Индивидуальный предприниматель Тулибаев Вадим Олегович",
                "currency_agent_id": 1,
                "currency_source_id": 1,
                "datetime_end": "2019-07-30 09:00:00",
                "datetime_end_inaccurate": 0,
                "datetime_start": "2019-07-30 00:00:00",
                "distance": 590,
                "is_relevant": 0,
                "place_cnt": 3,
                "price_agent_fee": 1,
                "price_agent_max": 2373,
                "price_source_tariff": 2100,
                "price_unitiki": 2373,
                "properties": None,
                "ride_segment_id": "229294401",
                "route_name": "Уфа-Ульяновск",
                "station_end": {
                    "city_id": 3430,
                    "city_title": "Ульяновск",
                    "city_type": "город",
                    "country_id": 1,
                    "country_iso": "RU",
                    "country_title": "Россия",
                    "district_title": "",
                    "lat": "54.317002",
                    "lng": "48.402243",
                    "region_title": "Ульяновская область",
                    "station_address": "шоссе Димитровградское,  дом 20",
                    "station_id": 78065,
                    "station_is_verified": "1",
                    "station_lat": "54.33705671989529",
                    "station_lng": "48.54757335058242",
                    "station_title": "Автостанция Верхняя Терраса"
                },
                "station_start": {
                    "city_id": 31,
                    "city_title": "Уфа",
                    "city_type": "город",
                    "country_id": 1,
                    "country_iso": "RU",
                    "country_title": "Россия",
                    "district_title": "",
                    "lat": "54.734721",
                    "lng": "55.957829",
                    "region_title": "Республика Башкортостан",
                    "station_address": "село Булгаково, улица Аэропорт, владение 1",
                    "station_id": 9306,
                    "station_is_verified": "1",
                    "station_lat": "54.565467",
                    "station_lng": "55.883763",
                    "station_title": "Аэропорт Уфа"
                },
                "ticket_printed_required": "0",
                "vendor_id": 1862
            }))

        hamcrest.assert_that(ride, hamcrest.has_entries({
            '@id': hamcrest.instance_of(str),
            'arrival': '2019-07-30T09:00:00',
            'refundConditions': BaseRide.refund_conditions,
            'number': None,
            'currency': 'RUB',
            'partnerName': 'Unitiki',
            'partner': 'unitiki-new',
            'supplierModel': {
                'code': 'unitiki-new',
                'id': 1,
                'name': 'unitiki-new',
                'registerType': {'code': 'register_type_code', 'id': 1},
            },
            'fee': hamcrest.close_to(273, 0.1),
            'from': {
                'id': 'rasp_from_id',
                'desc': 'Уфа, Аэропорт Уфа',
                'supplier_id': 's9306'
            },
            'partnerPhone': None,
            'onlinePrice': None,
            'connector': 'unitiki-new',
            'to': {
                'id': 'rasp_to_id',
                'desc': 'Ульяновск, Автостанция Верхняя Терраса',
                'supplier_id': 's78065'
            },
            'carrierID': 'Индивидуальный предприниматель Тулибаев Вадим Олегович',
            'freeSeats': 3,
            'status': {
                'id': 1,
                'name': 'sale'
            },
            'bus': 'Toyota Alphard',
            'price': hamcrest.close_to(2373, 0.1),
            'partnerEmail': None,
            'canPayOffline': False,
            'bookOnly': False,
            'benefits': None,
            'name': 'Уфа-Ульяновск',
            'departure': '2019-07-30T00:00:00',
            'carrier': 'Индивидуальный предприниматель Тулибаев Вадим Олегович',
            'ticketLimit': 5,
            'onlineRefund': True,
            'bookFields': ['name', 'gender', 'birthDate', 'phone', 'document', 'email'],
            'carrierModel': {
                'timetable': u'\u043f\u043d-\u043f\u0442: 10:00\u201318:00',
                'id': 1,
                'name': 'carrier',
                'registerType': {'code': u'register_type_code', 'id': 1},
            },
        }))
