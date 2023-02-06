import json

from library.python import resource

from travel.hotels.suggest.builder.dictionary_builder import HotelNameRowMapper


class TestHotelNameRowMapper:
    def test_mapping(self):
        row = json.loads(resource.find('row.json'))
        result = list(HotelNameRowMapper()(row))

        assert len(result) == 8

        assert result[0]['lowerName'].startswith('radisson slavyanskaya отель и бизнес центр, москва')
        assert result[0]['name'] == 'Radisson Slavyanskaya Отель и Бизнес Центр, Москва'
        assert result[0]['description'] == 'Гостиница · Россия, Москва, площадь Европы, 2'
        assert result[0]['permalink'] == 1054982517
        assert result[0]['popularity'] == 1924.2620301625122

        assert result[1]['lowerName'].startswith('radisson')
        assert result[1]['name'] == 'Radisson Slavyanskaya Отель и Бизнес Центр, Москва'
        assert result[1]['description'] == 'Гостиница · Россия, Москва, площадь Европы, 2'
        assert result[1]['permalink'] == 1054982517
        assert result[1]['popularity'] == 1924.2620301625122
