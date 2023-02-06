# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from unittest import TestCase

import pytest
from rest_framework import serializers

from common.apps.train_order.enums import CoachType
from common.models.currency import Price
from travel.rasp.wizards.train_wizard_api.direction.filters.coach_type_filter import CoachTypeFilter
from travel.rasp.wizards.train_wizard_api.direction.segments import TrainSegment, TrainVariant
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import Place


class TestCoachTypeFilter(TestCase):
    def test_load__unknown_values(self):
        error_message = (
            'invalid coach filter value: values should be one of '
            '[common, compartment, platzkarte, sitting, soft, suite, unknown]'
        )

        not_valid_params = [
            ['not_valid'],
            ['soft', 'not_valid'],
            ['not_valid', 'soft'],
        ]

        for params in not_valid_params:
            with pytest.raises(serializers.ValidationError) as error:
                CoachTypeFilter.load(params)

            assert error.value.detail == [error_message]

    def test_load__valid_values(self):
        valid_params = [
            [],
            ['common'],
            ['common', 'compartment'],
            ['compartment'],
            ['platzkarte'],
            ['sitting'],
            ['soft'],
            ['suite'],
        ]

        for params in valid_params:
            CoachTypeFilter.load(params)

    def _make_variant(self, place_type, price=100):
        p = None
        if place_type is not None:
            p = Place(
                coach_type=place_type,
                count=0,
                max_seats_in_the_same_car=0,
                price=Price(price, 'RUB'),
                price_details=None,
                service_class='2Л',
            )

        return TrainVariant(segment=TrainSegment(
            train_number=None,
            train_title=None,
            train_brand=None,
            thread_type=None,
            duration=None,
            departure_station=None,
            departure_local_dt=None,
            arrival_station=None,
            arrival_local_dt=None,
            electronic_ticket=False,
            places=None,
            facilities_ids=None,
            updated_at=None,
            display_number=None,
            has_dynamic_pricing=None,
            two_storey=None,
            is_suburban=None,
            coach_owners=None,
            first_country_code=None,
            last_country_code=None,
            broken_classes=None,
            provider=None,
            raw_train_name=None,
            t_subtype_id=None,
        ), places_group=p)

    def test_bind__all_cases(self):
        values = [v.value for v in CoachType]
        values += ['unknown', None]
        for v in values:
            f = CoachTypeFilter.load([])
            f.bind([
                self._make_variant(v)
            ])
            assert f.is_bound

    def test_list_selectors__all_cases(self):
        def check(params, expected_values, actual_variants):
            f = CoachTypeFilter.load(params)
            f.bind(actual_variants)
            assert f.list_selectors() == expected_values

        variants = [
            self._make_variant(None), self._make_variant('unknown'),
            self._make_variant('common'), self._make_variant('compartment'),
            self._make_variant('platzkarte'), self._make_variant('sitting'),
            self._make_variant('soft'), self._make_variant('suite'),
        ]

        check([], [True] * len(variants), variants)
        check(['common'], [False] * 2 + [True] * 1 + [False] * (len(variants) - 3), variants)
        check(['compartment'], [False] * 3 + [True] * 1 + [False] * (len(variants) - 4), variants)
        check(['platzkarte'], [False] * 4 + [True] * 1 + [False] * (len(variants) - 5), variants)
        check(['sitting'], [False] * 5 + [True] * 1 + [False] * (len(variants) - 6), variants)
        check(['soft'], [False] * 6 + [True] * 1 + [False] * (len(variants) - 7), variants)
        check(['suite'], [False] * 7 + [True] * 1 + [False] * (len(variants) - 8), variants)

    def test_dump(self):
        variants = [
            self._make_variant('platzkarte', 1500),
            self._make_variant('compartment', 10000),
            self._make_variant('unknown'),
            self._make_variant('platzkarte', 1000),
            self._make_variant('compartment', 15000),
        ]
        f = CoachTypeFilter.load(['compartment'])
        f.bind(variants)
        f.update_availability([True] * len(variants))

        assert f.dump() == [
            {
                'available': False,
                'minimum_price': None,
                'selected': False,
                'value': 'common'
            },
            {
                'available': True,
                'minimum_price': {'currency': 'RUB', 'value': 10000},
                'selected': True,
                'value': 'compartment'
            },
            {

                'available': True,
                'minimum_price': {'currency': 'RUB', 'value': 1000},
                'selected': False,
                'value': 'platzkarte'
            },
            {

                'available': False,
                'selected': False,
                'value': 'sitting',
                'minimum_price': None

            },
            {

                'available': False,
                'selected': False,
                'value': 'soft',
                'minimum_price': None

            },
            {

                'available': False,
                'selected': False,
                'value': 'suite',
                'minimum_price': None
            },
            {

                'available': True,
                'selected': False,
                'value': 'unknown',
                'minimum_price': {u'currency': u'RUB', u'value': 100}
            }
        ]

    def test_get_search_params(self):
        f = CoachTypeFilter.load([])
        assert f.get_search_params() == ()

        f = CoachTypeFilter.load(['compartment', 'platzkarte'])
        expected = (
            ('trainTariffClass', 'compartment'),
            ('trainTariffClass', 'platzkarte'),
        )
        coach_params = f.get_search_params()
        assert coach_params == expected
