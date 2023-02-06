# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from unittest import TestCase

import pytest
from rest_framework import serializers

from common.models.currency import Price
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import Place
from travel.rasp.wizards.train_wizard_api.direction.filters.place_count_filter import PlaceCountFilter
from travel.rasp.wizards.train_wizard_api.direction.segments import TrainSegment, TrainVariant


class TestPlaceCountFilter(TestCase):
    def test_load__unknown_values(self):
        error_message = 'invalid place count filter value: values should be one of [1, 2, 3, 4]'

        not_valid_params = [
            ['unknown'],
            ['5'],
            ['-1'],
            ['0'],
            ['one'],
        ]

        for params in not_valid_params:
            with pytest.raises(serializers.ValidationError) as error:
                PlaceCountFilter.load(params)

            assert error.value.detail == [error_message]

    def test_load__multiple_values(self):
        error_message = (
            'invalid place count filter value: you have sent many values, but you should send only one value'
        )

        not_valid_params = [
            ['unknown', '1'],
            ['5', '1'],
            ['1', '3'],
            ['1', '5'],
            ['one', '1'],
        ]

        for params in not_valid_params:
            with pytest.raises(serializers.ValidationError) as error:
                PlaceCountFilter.load(params)

            assert error.value.detail == [error_message]

    def test_load__valid_values(self):
        valid_params = [
            ['1'],
            ['2'],
            ['3'],
            ['4'],
        ]

        for params in valid_params:
            PlaceCountFilter.load(params)

    def _make_variant(self, place_count):
        p = None
        if place_count is not None:
            p = Place(
                coach_type='',
                count=0,
                max_seats_in_the_same_car=place_count,
                price=Price(100, 'RUB'),
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
        for value in range(10) + [None]:
            f = PlaceCountFilter.load([])
            f.bind([
                self._make_variant(value)
            ])
            assert f.is_bound

    def test_list_selectors__all_cases(self):
        def check(params, expected_values, actual_variants):
            f = PlaceCountFilter.load(params)
            f.bind(actual_variants)
            assert f.list_selectors() == expected_values

        variants = [
            self._make_variant(None), self._make_variant(0),
            self._make_variant(1), self._make_variant(1),
            self._make_variant(2), self._make_variant(2),
            self._make_variant(3), self._make_variant(3),
            self._make_variant(4), self._make_variant(100),
        ]

        check([], [True] * len(variants), variants)
        check(['1'], [False] * 2 + [True] * (len(variants) - 2), variants)
        check(['2'], [False] * 4 + [True] * (len(variants) - 4), variants)
        check(['3'], [False] * 6 + [True] * (len(variants) - 6), variants)
        check(['4'], [False] * 8 + [True] * (len(variants) - 8), variants)
