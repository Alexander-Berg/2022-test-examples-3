# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from unittest import TestCase

import pytest
from rest_framework import serializers

from common.models.currency import Price
from travel.rasp.wizards.train_wizard_api.direction.filters.price_filter import PriceFilter
from travel.rasp.wizards.train_wizard_api.direction.segments import TrainSegment, TrainVariant
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import Place


class TestPriceFilter(TestCase):
    def test_load__unknown_values(self):
        error_message = 'invalid price filter value: it should be one of [-1000, 1000-2000, 2000-3000, 3000-]'

        not_valid_params = [
            ['unknown'],
            ['-1000', 'unknown'],
            ['unknown', '-1000'],
            ['1000'],
            ['1000-'],
            ['-3000'],
            ['3000-4000'],
        ]

        for params in not_valid_params:
            with pytest.raises(serializers.ValidationError) as error:
                PriceFilter.load(params)

            assert error.value.detail == [error_message]

    def test_load__valid_values(self):
        valid_params = [
            [],
            ['-1000'],
            ['1000-2000'],
            ['2000-3000'],
            ['3000-'],
        ]

        for params in valid_params:
            PriceFilter.load(params)

    def _make_variant(self, price_value):
        p = None
        if price_value is not None:
            p = Place(
                coach_type='',
                count=0,
                max_seats_in_the_same_car=0,
                price=Price(price_value, 'RUB'),
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
        values = [
            0, 1, 100, 500, 999, 999.99,
            1000, 1001, 1100, 1500, 1999, 1999.99,
            2000, 2001, 2100, 2500, 2999, 2999.99,
            3000, 3001, 3100, 3500, 5000, 999999,
            None
        ]
        for value in values:
            f = PriceFilter.load(['-1000'])
            f.bind([
                self._make_variant(value)
            ])

            assert f.is_bound

    def test_list_selectors__all_cases(self):
        def check(params, expected_values, actual_variants):
            f = PriceFilter.load(params)
            f.bind(actual_variants)
            assert f.list_selectors() == expected_values

        variants = [
            self._make_variant(0), self._make_variant(500),
            self._make_variant(1000), self._make_variant(1500),
            self._make_variant(2000), self._make_variant(2500),
            self._make_variant(3000), self._make_variant(3500),
            self._make_variant(None)
        ]

        check([], [True] * len(variants), variants)
        check(['-1000'], [True] * 2 + [False] * (len(variants) - 2), variants)
        check(['1000-2000'], [False] * 2 + [True] * 2 + [False] * (len(variants) - 4), variants)
        check(['2000-3000'], [False] * 4 + [True] * 2 + [False] * (len(variants) - 6), variants)
        check(['3000-'], [False] * 6 + [True] * 2 + [False] * (len(variants) - 8), variants)
