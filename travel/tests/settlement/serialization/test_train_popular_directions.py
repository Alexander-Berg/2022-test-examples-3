# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

from common.tester.factories import create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.serialization.train_popular_directions import (
    TrainPopularSettlementSchema
)


class TestTrainPopularSettlementSchema(TestCase):
    def setUp(self):
        self._city = create_settlement(
            title='city',
            slug='city_slug',
            title_ru_accusative='accusative_city_title',
            title_ru_genitive='genitive_city_title',
        )

        self._city_without_additional_translations = create_settlement(
            title='city_without_additional_translations',
            slug='city_without_additional_translations',
        )

    @staticmethod
    def _assert(city, extend_expected, context=None):
        expected = {
            'directionTitle': None,
            'title': 'city',
            'id': city.id,
            'key': 'c{}'.format(city.id),
            'slug': 'city_slug'
        }
        expected.update(extend_expected)
        result = TrainPopularSettlementSchema(
            context=context
        ).dumps(city)

        assert not result.errors

        data = json.loads(result.data)

        assert data == expected

    def test_without_direction(self):
        self._assert(self._city, {})
        self._assert(self._city_without_additional_translations, {
            'title': 'city_without_additional_translations',
            'slug': 'city_without_additional_translations'
        })

    def test_for_from_direction(self):
        self._assert(self._city, {
            'directionTitle': 'из genitive_city_title'
        }, {'destination_direction': 'from'})

        self._assert(self._city_without_additional_translations, {
            'title': 'city_without_additional_translations',
            'slug': 'city_without_additional_translations'
        }, {'destination_direction': 'from'})

    def test_for_to_direction(self):
        self._assert(self._city, {
            'directionTitle': 'в accusative_city_title'
        }, {'destination_direction': 'to'})

        self._assert(self._city_without_additional_translations, {
            'title': 'city_without_additional_translations',
            'slug': 'city_without_additional_translations'
        }, {'destination_direction': 'to'})
