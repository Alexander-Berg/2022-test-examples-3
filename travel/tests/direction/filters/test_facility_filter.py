# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from rest_framework import serializers

from common.apps.train.models import Facility
from common.tester.testcase import TestCase
from travel.rasp.wizards.train_wizard_api.direction.filters.facility_filter import FacilityFilter
from travel.rasp.wizards.train_wizard_api.direction.segments import TrainSegment, TrainVariant
from travel.rasp.wizards.train_wizard_api.lib.facility_provider import facility_provider


class TestFacilityFilter(TestCase):
    def setUp(self):
        facility_provider.build_cache()

    def test_load__unknown_values(self):
        error_message = (
            'invalid facility filter value: values should be one of '
            '[BED, COND, EAT, PAP, SAN, TRAN, TV, WIFI, nearToilet, side, upper]'
        )

        not_valid_params = [
            ['EATTING'],
            ['1L'],
            ['unknown'],
            ['EAT', 'unknown'],
            ['unknown', 'EAT'],
        ]

        for params in not_valid_params:
            with pytest.raises(serializers.ValidationError) as error:
                FacilityFilter.load(params)

            assert error.value.detail == [error_message]

    def test_load__valid_values(self):
        valid_params = [
            [],
            ['EAT'],
            ['SAN', 'side'],
        ]

        for params in valid_params:
            FacilityFilter.load(params)

    def _make_variant(self, facilities_ids):
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
            updated_at=None,
            facilities_ids=facilities_ids,
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
        ), places_group=None)

    def test_bind__all_cases(self):
        for facilities_ids in [[Facility.EATING_ID], [Facility.WIFI_ID], []]:
            f = FacilityFilter.load([])
            f.bind([
                self._make_variant(facilities_ids)
            ])
            assert f.is_bound

    def test_list_selectors__all_cases(self):
        def check(params, expected_values, actual_variants):
            f = FacilityFilter.load(params)
            f.bind(actual_variants)
            assert f.list_selectors() == expected_values

        variants = [
            self._make_variant([]),
            self._make_variant([Facility.EATING_ID]),
            self._make_variant([Facility.EATING_ID, Facility.WIFI_ID])
        ]

        check([], [True] * len(variants), variants)
        check(['EAT'], [False, True, True], variants)
        check(['EAT', 'WIFI'], [False, False, True], variants)
