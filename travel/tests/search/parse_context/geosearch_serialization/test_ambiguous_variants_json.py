# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization import ambiguous_variants_json


@mock.patch('travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_serialization.ambiguous_variant_json',
            side_effect=lambda variant, national_version, language: str(variant) + ' json')
class TestAmbiguousVariantsJson(TestCase):
    """
    Тесты на JSON-сериализацию списка объектов AmbiguousVariant.
    """
    def setUp(self):
        self.national_version = 'ua'
        self.language = 'uk'

    def test_zero_variants(self, m_variant_json):
        """
        Сериализация пустого списка.
        """
        assert ambiguous_variants_json([], self.national_version, self.language) == []
        m_variant_json.assert_not_called()

    def test_single_variant(self, m_variant_json):
        """
        Сериализация списка из единственного варианта.
        """
        variants = ['variant_1']
        assert ambiguous_variants_json(variants, self.national_version, self.language) == ['variant_1 json']
        m_variant_json.assert_called_once_with('variant_1', self.national_version, self.language)

    def test_two_variants(self, m_variant_json):
        """
        Сериализация списка из двух вариантов.
        """
        variants = ['variant_1', 'variant_2']
        assert ambiguous_variants_json(variants, self.national_version, self.language) == [
            'variant_1 json',
            'variant_2 json'
        ]
        m_variant_json.mock_calls = [
            mock.call('variant_1', self.national_version, self.language),
            mock.call('variant_2', self.national_version, self.language)
        ]
