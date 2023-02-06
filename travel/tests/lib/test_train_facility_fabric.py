# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from unittest import TestCase

from common.apps.train.models import CoachType, Facility
from travel.rasp.wizards.train_wizard_api.lib.facility_provider import facility_provider
from travel.rasp.wizards.train_wizard_api.lib.train_facility_fabric import TrainFacilityFabric
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_price_pb2 import Price
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_place_info_pb2 import TrainPlace, TrainPlaceInfo


class TestTrainFacilityFabric(TestCase):
    def setUp(self):
        facility_provider.build_cache()
        self._fabric = TrainFacilityFabric(
            facility_provider=facility_provider
        )

    def _make_place(self, coach_type_id=CoachType.COMMON_ID, coach_number=1, number='3', facilities_ids=[]):
        return TrainPlace(
            coach_number=coach_number,
            group=1,
            number=number,
            company_id=1,
            coach_type_id=coach_type_id,
            facilities_ids=facilities_ids,
            price=Price(
                value=100,
                base_value=100
            )
        )

    def test_without_facilities(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(number='1'),
            self._make_place(number='1A'),
        ])
        facilities_ids = self._fabric.get_facilities_ids(
            place_info
        )

        assert facilities_ids == ()

    def test_facilities_for_single_place(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(number='1', facilities_ids=[Facility.WIFI_ID, Facility.EATING_ID]),
            self._make_place(number='1A'),
        ])
        facilities_ids = self._fabric.get_facilities_ids(
            place_info
        )

        assert facilities_ids == (Facility.EATING_ID, Facility.WIFI_ID,)

    def test_facilities_for_two_place(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(number='1', facilities_ids=[Facility.WIFI_ID, Facility.EATING_ID]),
            self._make_place(number='1A', facilities_ids=[Facility.NEAR_TOILET_ID, Facility.WIFI_ID]),
        ])
        facilities_ids = self._fabric.get_facilities_ids(
            place_info
        )

        assert facilities_ids == (Facility.EATING_ID, Facility.WIFI_ID, Facility.NEAR_TOILET_ID,)

    def test_unknown_facilities(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(number='1', facilities_ids=[Facility.WIFI_ID, Facility.EATING_ID, 9999]),
        ])
        facilities_ids = self._fabric.get_facilities_ids(
            place_info
        )

        assert facilities_ids == (Facility.EATING_ID, Facility.WIFI_ID,)
