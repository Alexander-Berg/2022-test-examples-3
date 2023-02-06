# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.models.geo import Settlement, Station, Country
from common.tester.factories import create_settlement, create_station, create_country
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import AmbiguousVariant
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization import ambiguous_variant_json


POINT_JSON = {'point': 'point json'}


@mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.result_without_errors_point_json',
            return_value=POINT_JSON)
class TestAmbiguousVariantJson(TestCase):
    """
    Тесты на JSON-сериализацию объектов AmbiguousVariant.
    """
    def setUp(self):
        self.national_version = 'ua'
        self.language = 'uk'
        self.id = 789
        self.title = 'Воскресенское'
        self.additional_title = 'Мелеузовский р-н, Республика Башкортостан, Россия'

    def test_settlement_with_additional_title_selected(self, m_point_json):
        """
        В полном названии города присутствует уточнение. Вариант помечен флагом "выбран".
        Пример уточнения в названии - значение по ключу add:
        {'title': 'г. Благовещенск', add: 'Мелеузовский район, Башкортостан, Россия'}
        """
        is_selected = True
        settlement = create_settlement(id=self.id)
        with mock.patch.object(Settlement, 'L_omonim_title',
                               return_value={'title': self.title, 'add': self.additional_title}) as m_L_omonim_title:
            assert ambiguous_variant_json(AmbiguousVariant(settlement, is_selected),
                                          self.national_version, self.language) == {
                'point': 'point json',
                'ambiguousTitle': {
                    'title': self.title,
                    'additionalTitle': self.additional_title,
                },
                'isSelected': is_selected
            }

        m_L_omonim_title.assert_called_once_with(show_district=True,
                                                 lang=self.language, national_version=self.national_version)
        m_point_json.assert_called_once_with(settlement, self.language)

    def test_settlement_without_additional_title_not_selected(self, m_point_json):
        """
        В полном названии города отсутствует уточнение (например, это город - столица региона).
        Вариант не помечен флагом "выбран".
        """
        is_selected = False
        settlement = create_settlement(id=self.id)
        with mock.patch.object(Settlement, 'L_omonim_title', return_value={'title': self.title}) as m_L_omonim_title:
            assert ambiguous_variant_json(AmbiguousVariant(settlement, is_selected),
                                          self.national_version, self.language) == {
                'point': 'point json',
                'ambiguousTitle': {
                    'title': self.title,
                    'additionalTitle': None,
                },
                'isSelected': is_selected
            }

        m_L_omonim_title.assert_called_once_with(show_district=True,
                                                 lang=self.language, national_version=self.national_version)
        m_point_json.assert_called_once_with(settlement, self.language)

    def test_station_with_additional_title_selected(self, m_point_json):
        """
        В полном названии станции присутствует уточнение. Вариант помечен флагом "выбран".
        Пример уточнения в названии - значение по ключу add:
        {'title': 'авт. ост. Воскресенское', add: 'Тульская область'}
        """
        is_selected = True
        settlement = create_station(id=self.id)
        with mock.patch.object(Station, 'L_omonim_title',
                               return_value={'title': self.title, 'add': self.additional_title}) as m_L_omonim_title:
            assert ambiguous_variant_json(AmbiguousVariant(settlement, is_selected),
                                          self.national_version, self.language) == {
                'point': 'point json',
                'ambiguousTitle': {
                    'title': self.title,
                    'additionalTitle': self.additional_title,
                },
                'isSelected': is_selected
            }

        m_L_omonim_title.assert_called_once_with(show_district=True,
                                                 lang=self.language, national_version=self.national_version)
        m_point_json.assert_called_once_with(settlement, self.language)

    def test_country_without_additional_title_selected(self, m_point_json):
        """
        В полном названии страны отсутствует уточнение (для стран вообще не формируется уточнение).
        Вариант помечен флагом "выбран".
        """
        is_selected = True
        settlement = create_country(id=self.id)
        with mock.patch.object(Country, 'L_omonim_title', return_value={'title': 'Парагвай'}) as m_L_omonim_title:
            assert ambiguous_variant_json(AmbiguousVariant(settlement, is_selected),
                                          self.national_version, self.language) == {
                'point': 'point json',
                'ambiguousTitle': {
                    'title': 'Парагвай',
                    'additionalTitle': None,
                },
                'isSelected': is_selected
            }

        m_L_omonim_title.assert_called_once_with(show_district=True,
                                                 lang=self.language, national_version=self.national_version)
        m_point_json.assert_called_once_with(settlement, self.language)
