# -*- coding: utf-8 -*-

from __future__ import unicode_literals

from library.python import resource

from travel.hotels.feeders.partners.expedia.lib.expedia import ExpediaRowMapper


class TestExpediaMapper(object):
    PARTNER = 'test'

    @staticmethod
    def sort_by_id(arr):
        return sorted(arr, key=lambda x: x['id'])

    def test_expedia_hotel_mapping(self):
        mapper = ExpediaRowMapper(name=self.PARTNER, debug=True, timestamp=100500)

        hotel = mapper.map(
            resource.find('/expedia-hotel.json'),
            {'lang': 'en'},
        ).to_dict(partner_name=self.PARTNER)

        assert hotel['originalId'] == '22552964'
        assert hotel['name'] == [
            {'lang': 'EN', 'value': 'CA2673 - Sonoma Resort'},
        ]
        assert hotel['address'] == [
            {'lang': 'EN', 'one_line': 'United States of America, Kissimmee, 2673 Calistoga Avenue'},
        ]
        assert hotel['country'] == 'US'
        assert hotel['phone'] == [
            {'value': '1-352-552-0029'}
        ]
        assert self.sort_by_id(hotel['feature']) == self.sort_by_id([
            {'id': 'TV in room', 'value': True},
            {'id': 'air_conditioning', 'value': True},
            {'id': 'check in', 'enum_id': 'checkin_16'},
            {'id': 'check out', 'enum_id': 'checkout_10'},
            {'id': 'hairdryer', 'value': True},
            {'id': 'hotel_type', 'enum_id': 'accommodation_type_ private_house'},
            {'id': 'internet_in_hotel', 'enum_id': 'free internet'},
            {'id': 'kitchen in the room', 'value': True},
            {'id': 'parking type', 'enum_id': 'parking type free'},
            {'id': 'pool_type', 'enum_id': 'outdoor_pool'},
            {'id': 'star', 'enum_id': 'four'},
            {'id': 'wi_fi', 'value': True},
            {'id': 'covid_hotel_safety_marking_floor', 'value': True},
            {'id': 'covid_hotel_safety_placing_tables_at_distance', 'value': True},
            {'id': 'covid_hotel_safety_employees_use_personal_protective_gear', 'value': True},
        ])
        assert hotel['photos'] == [
            {'link': 'https://i.travelapi.com/hotels/23000000/22560000/22553000/22552964/b01da730_z.jpg'},
            {'link': 'https://i.travelapi.com/hotels/23000000/22560000/22553000/22552964/3b81dd5a_z.jpg'},
            {'link': 'https://i.travelapi.com/hotels/23000000/22560000/22553000/22552964/225948aa_z.jpg'},
        ]

    def test_expedia_hotel_mapping_with_room_photos(self):
        mapper = ExpediaRowMapper(name=self.PARTNER, debug=True, timestamp=100500)

        hotel = mapper.map(
            resource.find('/expedia-hotel-with-rooms-photo.json'),
            {'lang': 'en'},
        ).to_dict(partner_name=self.PARTNER)

        assert hotel['originalId'] == '10623459'
        assert hotel['name'] == [
            {'lang': 'EN', 'value': 'Hun Club'},
        ]
        assert hotel['address'] == [
            {'lang': 'EN', 'one_line': 'Turkey, Antalya, Liman Mah. Bileydiler Caddesi No: 7, Konyaalti'},
        ]
        assert hotel['country'] == 'TR'
        assert hotel['phone'] == [
            {'value': '90-506-1565039'},
            {'value': '90-242-4394949'},
        ]
        assert self.sort_by_id(hotel['feature']) == self.sort_by_id([
            {'id': 'cleaning frequency', 'enum_id': 'on certain days'},
            {'id': 'around the clock front desk', 'value': True},
            {'id': 'kids_pool', 'value': True},
            {'id': 'TV in room', 'value': True},
            {'id': 'free transfer', 'enum_id': 'airport'},
            {'id': 'hairdryer', 'value': True},
            {'id': 'wi_fi', 'value': True},
            {'id': 'internet_in_hotel', 'enum_id': 'free internet'},
            {'id': 'type of transfer', 'enum_id': 'paid transfer'},
            {'id': 'check in', 'enum_id': 'checkin_14'},
            {'id': 'air_conditioning', 'value': True},
            {'id': 'hotel garden', 'value': True},
            {'id': 'refrigerator', 'value': True},
            {'id': 'gym', 'value': True},
            {'id': 'kitchen equipment', 'enum_id': 'stove'},
            {'id': 'kitchen equipment', 'enum_id': 'microwave'},
            {'id': 'kitchen equipment', 'enum_id': 'kitchen dishwasher', },
            {'id': 'kitchen equipment', 'enum_id': 'teapot'},
            {'id': 'kitchen in the room', 'value': True},
            {'id': 'hotel_type', 'enum_id': 'accommodation_type_apartments'},
            {'id': 'pool_type', 'enum_id': 'outdoor_pool'},
            {'id': 'iron', 'value': True},
            {'id': 'slippers', 'value': True},
            {'id': 'check out', 'enum_id': 'checkout_12'},
            {'id': 'parking type', 'enum_id': 'parking type free'},
            {'id': 'washing machine', 'value': True},
        ])
        assert hotel['photos'] == [
            {
                'link': 'https://i.travelapi.com/room_2_photo_1.jpg',
            },
            {
                'link': 'https://i.travelapi.com/reception_photo.jpg'
            },
            {
                'link': 'https://i.travelapi.com/room_1_photo_1.jpg',
            },
            {
                'link': 'https://i.travelapi.com/room_1_photo_2_no_in_hotel_photo.jpg',
            },
            {
                'link': 'https://i.travelapi.com/common_photo_for_all_rooms.jpg',
            },
            {
                'link': 'https://i.travelapi.com/room_2_photo_2_no_in_hotel_photo.jpg',
            },
        ]

    def test_expedia_extract_room_data(self):
        mapper = ExpediaRowMapper(name=self.PARTNER, debug=True, timestamp=100500)

        hotel = mapper.map(
            resource.find('/expedia-hotel-russian.json'),
            {'lang': 'ru'},
        )

        room = filter(lambda r: r['id'] == '216325229', hotel.room_types.values)[0]
        assert room['lang'] == 'RU'
        assert room['value'] == 'Стандартный двухместный номер с 1 или 2 кроватями'

        room = filter(lambda r: r['id'] == '216325230', hotel.room_types.values)[0]
        assert room['lang'] == 'RU'
        assert room['value'] == 'Семейный двухместный номер с 1 или 2 кроватями'

        room = filter(lambda r: r['id'] == '216325231', hotel.room_types.values)[0]
        assert room['lang'] == 'RU'
        assert room['value'] == 'Полулюкс'
