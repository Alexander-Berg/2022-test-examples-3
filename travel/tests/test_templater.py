import json
from unittest.case import TestCase

from library.python import resource

from travel.hotels.tools.region_pages_builder.renderer.renderer.templater import (
    Hotel, Region, RegionData, Station, Templater
)


class TestTemplater(TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        city = Region(json.loads(resource.find('city-row.json')), None, 'ru', None)
        hotel = Hotel(json.loads(resource.find('hotel-row.json')))
        station = Station(json.loads(resource.find('station-row.json')))
        cls.city_data = RegionData(
            region=city,
            all_hotels={hotel.permalink: hotel},
            stations={station.id: station},
            links=list(),
            filter_config=None,
        )

    def test_declension_filters(self):
        templater = Templater()

        assert templater.render("{{city}}", self.city_data) == "Москва"
        assert templater.render("{{city|nominative}}", self.city_data) == "Москва"
        assert templater.render("{{city|genitive}}", self.city_data) == "Москвы"
        assert templater.render("{{city|dative}}", self.city_data) == "Москве"
        assert templater.render("{{city|accusative}}", self.city_data) == "Москву"
        assert templater.render("{{city|instrumental}}", self.city_data) == "Москвой"
        assert templater.render("{{city|prepositional}}", self.city_data) == "Москве"

        assert templater.render("{{city|ablative}}", self.city_data) == "из Москвы"
        assert templater.render("{{city|directional}}", self.city_data) == "в Москву"
        assert templater.render("{{city|locative}}", self.city_data) == "в Москве"

    def test_render_simple_template(self):
        templater = Templater()

        assert templater.render("Лучшие отели {{city|genitive}}", self.city_data) == "Лучшие отели Москвы"

    def test_render_link(self):
        templater = Templater()
        assert templater.render("{{link('Some text', 'https://ya.ru')}}", self.city_data) == '<a href="https://ya.ru">Some text</a>'

    def test_render_readable_join(self):
        templater = Templater()
        assert templater.render("{{rjoin(['Wi-Fi'])}}", self.city_data) == 'Wi-Fi'
        assert templater.render("{{rjoin(['Wi-Fi'], limit=0)}}", self.city_data) == ''
        assert templater.render("{{rjoin(['Wi-Fi', 'оплата картой'])}}", self.city_data) == 'Wi-Fi и оплата картой'
        assert templater.render("{{rjoin(['Wi-Fi', 'парковка', 'оплата картой'])}}", self.city_data) == 'Wi-Fi, парковка и оплата картой'
        assert templater.render("{{rjoin(['Wi-Fi', 'парковка', 'оплата картой', 'бассейн'])}}", self.city_data) == 'Wi-Fi, парковка, оплата картой и бассейн'
        assert templater.render("{{rjoin(['Wi-Fi', 'парковка', 'оплата картой', 'бассейн'], limit=2)}}", self.city_data) == 'Wi-Fi и парковка'
        assert templater.render("{{rjoin(top_hotels_1.top_features, declension='accusative')}}", self.city_data) == 'Wi-Fi, парковку и оплату картой'
        assert templater.render("{{rjoin(['Wi-Fi', 'парковка', 'оплата картой'], declension='accusative')}}", self.city_data) == 'Wi-Fi, парковку и оплату картой'

    def test_hotel_render(self):
        templater = Templater()

        template = "Лучший отель города {{city}} это {{top_hotels_1}}. Если вы решите остановиться {{top_hotels_1|locative}}, то вы не останетесь разочарованы."

        assert templater.render(template, self.city_data) == "Лучший отель города Москва это Отель Аркадия. Если вы решите остановиться в Отеле Аркадия, то вы не останетесь разочарованы."

    def test_price_render(self):
        templater = Templater()

        template = "Цена на 4 звезды - {{city.min_price_stars_4}}, а на 5 - {{city.min_price_stars_5}}. Самое дешевое - {{city.min_price}}."
        rendered = templater.render(template, self.city_data)

        self.assertEqual(
            rendered,
            "Цена на 4 звезды - <span class=\"price\" currency=\"RUB\">40</span>, "
            "а на 5 - <span class=\"price\" currency=\"RUB\">50</span>. "
            "Самое дешевое - <span class=\"price\" currency=\"RUB\">15.5</span>."
        )


class TestCityParser:
    def test_parse(self):
        row = json.loads(resource.find('city-row.json'))
        city = Region(row, None, 'ru', None)
        assert city.nominative == 'Москва'
