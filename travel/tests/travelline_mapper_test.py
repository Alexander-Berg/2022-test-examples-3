# -*- coding: utf-8 -*-

from __future__ import unicode_literals

import json

from library.python import resource

from travel.hotels.feeders.partners.travelline.lib.travelline import TravellineRowMapper, Travelline


class TestTravellineMapper(object):
    PARTNER = 'test'

    def test_travelline_hotel_mapping(self):
        mapper = TravellineRowMapper(100500)

        hotel = mapper.map(
            json.loads(resource.find('/hotel.json')),
            {
                "lang": "ru-ru",
                "is_yandex_hotel": True
            },
        ).to_dict(partner_name=self.PARTNER, allowed_fields=Travelline.allowed_fields)

        assert hotel['originalId'] == '17'
        assert hotel['_travellineMaxLOS'] == 19
        assert hotel['name'] == [
            {'lang': 'RU', 'value': 'Гостиница Спутник'}
        ]

        assert hotel['address'] == [
            {'lang': 'RU', 'one_line': 'Россия, г. Санкт-Петербург, пр-т Мориса Тореза, д. 36'},
        ]
        assert hotel['country'] == 'RU'
        assert hotel['phone'] == [
            {'type': 'PHONE', 'value': '+7 (812) 457-04-57'},
            {'type': 'PHONE', 'value': '+7 (800) 775-54-57'}
        ]
        assert hotel['feature'] == [
            {'enum_id': 'three', 'id': 'star'},
        ]
        # Check only 11 photos, skip room photos
        assert hotel['photos'][:11] == [
            {'link': 'https://www.travelline.ru/resource/images/p/17/636066097320108730-4e647cac-43a8-4e4f-8aef-d0f0e302ad2c'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/637066564560248328-ac2689f5-658e-42d7-b267-82bbbca1fb49'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/637066564596457002-f9d7e283-ef02-45eb-b733-4950bc8c7d61'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/637066564179114262-1265114e-f782-422e-a827-0ce96d4a637b'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/637066563590260791-476349d9-dad0-4654-969d-dc531d0e2feb'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/636096236067798347-26b6ae71-5b04-43a6-aa42-d7b1c50ed1fc'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/636096236077796997-fa9e4cab-451c-43cf-a607-6f8adb09b803'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/637066563993407964-5fa187a0-42ad-4ccc-bc37-f0d0b967c97a'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/636096236087618601-9972ef36-5da5-4b83-b04d-3f431f892c4a'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/636096236096150480-0a081089-54f7-46a0-b18a-0f56ac914a36'},
            {'link': 'https://www.travelline.ru/resource/images/p/17/637066563590483754-6dad1144-071b-47eb-a762-5bb53d3194ff'}
        ]

    def test_photos_extraction(self):
        mapper = TravellineRowMapper(100500)

        hotel = mapper.map(
            json.loads(resource.find('/hotel-with-room-photos.json')),
            {
                "lang": "ru-ru",
                "is_yandex_hotel": True
            },
        ).to_dict(partner_name=self.PARTNER)

        assert hotel['originalId'] == '17'
        assert hotel['name'] == [
            {'lang': 'RU', 'value': 'Гостиница Спутник'}
        ]

        assert hotel['photos'] == [
            {
                'link': 'https://www.travelline.ru/room_2_photo_1.jpg',
            },
            {
                'link': 'https://www.travelline.ru/reception_photo.jpg'
            },
            {
                'link': 'https://www.travelline.ru/room_1_photo_1.jpg',
            },
            {
                'link': 'https://www.travelline.ru/room_1_photo_2_no_in_hotel_photo.jpg',
            },
            {
                'link': 'https://www.travelline.ru/room_2_photo_2_no_in_hotel_photo.jpg',
            },
        ]
