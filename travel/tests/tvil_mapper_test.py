# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import json

from library.python import resource

from travel.hotels.feeders.lib.model import enums
from travel.hotels.feeders.partners.tvil.lib.tvil import TvilRowMapper


class TestTvilMapper(object):
    PARTNER = 'test'

    def test_tvil_hotel_mapping(self):
        mapper = TvilRowMapper()

        hotel = mapper.map(
            json.loads(resource.find('/tvil-hotel.json')),
            json.loads(resource.find('/tvil-city-info.json'))
        ).to_dict(partner_name=self.PARTNER, allowed_fields=["_description"])

        assert hotel['originalId'] == '430321'
        assert hotel['name'] == [
            {'lang': 'EN', 'value': 'Malaya gostinica "NAUTILUS"'},
            {'lang': 'RU', 'value': 'Малая гостиница \"NAUTILUS\"'}
        ]
        assert hotel['address'] == [
            {'lang': 'EN', 'one_line': 'Russia, Divnomorskoe, Morskaya ulica, 4'},
            {'lang': 'RU', 'one_line': 'Россия, Дивноморское, Морская улица, 4'}
        ]
        assert hotel['lat'] == 45.127845
        assert hotel['lon'] == 33.522614
        assert hotel['country'] == 'TR'
        assert hotel['rubric'][0]['value'] == enums.HotelRubric.HOTEL.value
        assert hotel['photos'] == [
            {'link': 'https://hmd.tvil.ru/tmp/20170722/e11/1424423.jpeg'},
            {'link': 'https://hmd.tvil.ru/tmp/20180610/e11/1770602.jpeg'},
            {'link': 'https://hmd.tvil.ru/tmp/20180629/e11/1813571.jpeg'},
            {'link': 'https://hmd.tvil.ru/tmp/20180629/e11/1813572.jpeg'}
        ]
        feature = hotel['feature']
        assert {'id': 'around the clock front desk', 'value': True} in feature
        assert {'id': 'wi_fi', 'value': True} in feature
        assert {'id': 'pets', 'value': True} in feature
        assert {'id': 'children_playground', 'value': True} in feature
        assert {'id': 'air_conditioning', 'value': True} in feature
        assert {'id': 'veranda terrace patio', 'value': True} in feature
        assert {'id': 'shared kitchen', 'value': True} in feature
        assert {'id': 'refrigerator', 'value': True} in feature
        assert {'id': 'bbq', 'value': True} in feature
        assert {'enum_id': 'stove', 'id': 'kitchen equipment'} in feature
        assert {'id': 'car_park', 'value': True} in feature
        assert {'id': 'car_park', 'value': True} in feature
        assert {'id': 'banquet hall', 'value': True} in feature
        assert {'id': 'wired_internet', 'value': True} in feature
