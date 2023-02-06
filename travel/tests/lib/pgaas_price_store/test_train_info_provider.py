# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, time, timedelta
from django.conf import settings
from logging import Logger

from contextlib2 import closing
from mock import Mock
from typing import cast

from common.models.geo import Settlement
from travel.rasp.wizards.train_wizard_api.lib.express_system_provider import ExpressSystemProvider
from travel.rasp.wizards.train_wizard_api.lib.facility_provider import FacilityProvider
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.db_models import TrainInfo
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.train_info_provider import TrainInfoProvider, TrainInfoModel
from travel.rasp.wizards.train_wizard_api.lib.storage_timed_execute import wait_for_future_and_build_info
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_place_info_pb2 import TrainPlaceInfo
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_tariff_info_pb2 import TrainTariffInfo
from travel.rasp.wizards.train_wizard_api.tests.helpers.postgres_test_case import TestCase


class TestTrainInfoProvider(TestCase):
    def setUp(self):
        super(TestTrainInfoProvider, self).setUp()
        self._fake_express_system_provider = Mock(spec=ExpressSystemProvider)
        self._railway_utils = Mock()
        self._fake_logger = Mock(Logger)
        self._fake_facility_provider = Mock(spec=FacilityProvider)
        self._fake_facility_provider.get_code_by = Mock(return_value='some_code')
        self._provider = TrainInfoProvider(
            storage_store=self._storage_store,
            express_system_provider=cast(ExpressSystemProvider, self._fake_express_system_provider),
            railway_utils=self._railway_utils,
            facility_provider=self._fake_facility_provider,
            logger=cast(Logger, self._fake_logger),
        )

        self._departure_point = Settlement(id=10)
        self._arrival_point = Settlement(id=1000)
        self._departure_express_id = self._departure_point.id * 2
        self._arrival_express_id = self._arrival_point.id * 2
        self._min_departure_dt = date(2017, 9, 15)
        self._timeout = settings.DBAAS_TRAIN_WIZARD_API_SELECT_TIMEOUT

    def test_arrival_point_has_not_express_code(self):
        self._fake_express_system_provider.find_related_express_ids.side_effect = [
            {self._departure_express_id}, None
        ]

        future, context = self._provider.find(
            departure_point=self._departure_point,
            arrival_point=self._arrival_point,
            departure_date=self._min_departure_dt,
        )

        self._fake_logger.warn.assert_called_once_with(
            'Can not find prices by [%s-%s], because can not find express codes for one of points [%s-%s]',
            {self._departure_express_id}, None,
            'c10', 'c1000',
        )
        result = wait_for_future_and_build_info(self._provider, future, context, self._timeout)
        assert result == ()

    def test_departure_point_has_not_express_code(self):
        self._fake_express_system_provider.find_related_express_ids.side_effect = [None, {self._arrival_express_id}]

        future, context = self._provider.find(
            departure_point=self._departure_point,
            arrival_point=self._arrival_point,
            departure_date=self._min_departure_dt,
        )

        self._fake_logger.warn.assert_called_once_with(
            'Can not find prices by [%s-%s], because can not find express codes for one of points [%s-%s]',
            None, {self._arrival_express_id},
            'c10', 'c1000',
        )
        result = wait_for_future_and_build_info(self._provider, future, context, self._timeout)
        assert result == ()

    def test_empty_search(self):
        self._fake_express_system_provider.find_related_express_ids.side_effect = [
            {self._departure_express_id}, {self._arrival_express_id}
        ]

        future, context = self._provider.find(
            departure_point=self._departure_point,
            arrival_point=self._arrival_point,
            departure_date=self._min_departure_dt,
        )

        assert self._fake_logger.warn.call_count == 0
        self._fake_logger.info.assert_called_once_with(
            'Start search: [%s(%s)-%s(%s)-%s-%s]',
            'c10', {self._departure_express_id},
            'c1000', {self._arrival_express_id},
            self._min_departure_dt, 1
        )

        result = wait_for_future_and_build_info(self._provider, future, context, self._timeout)
        assert result == ()

    def _create_train_info(self, departure_id, arrival_id, departure_at, number):
        return TrainInfo(
            departure_point_express_id=departure_id,
            arrival_point_express_id=arrival_id,
            departure_at=departure_at,
            number=number,
            tariffs_info=TrainTariffInfo(),
            places_info=TrainPlaceInfo(),
            facilities_ids=(),
            created_at=datetime(2018, 9, 1),
            updated_at=datetime(2020, 9, 1),
        )

    def test_search_two_train_in_the_same_day(self):
        with closing(self._storage_store.get('master').get_session()) as session:
            session.add(self._create_train_info(
                departure_id=self._departure_express_id,
                arrival_id=self._arrival_express_id,
                departure_at=datetime.combine(self._min_departure_dt, time.min),
                number='some_number_1',
            ))
            session.add(self._create_train_info(
                departure_id=self._departure_express_id,
                arrival_id=self._arrival_express_id,
                departure_at=datetime.combine(self._min_departure_dt, time.min),
                number='some_number_2',
            ))
            session.commit()

        self._fake_express_system_provider.find_related_express_ids.side_effect = [
            {self._departure_express_id}, {self._arrival_express_id}
        ]

        future, context = self._provider.find(
            departure_point=self._departure_point,
            arrival_point=self._arrival_point,
            departure_date=self._min_departure_dt,
        )

        assert self._fake_logger.warn.call_count == 0
        self._fake_logger.info.assert_called_once_with(
            'Start search: [%s(%s)-%s(%s)-%s-%s]',
            'c10', {self._departure_express_id},
            'c1000', {self._arrival_express_id},
            self._min_departure_dt, 1
        )

        result = wait_for_future_and_build_info(self._provider, future, context, self._timeout)
        assert result == (
            TrainInfoModel(
                departure_at=datetime.combine(self._min_departure_dt, time.min),
                number='some_number_1',
                facilities_ids=[],
                electronic_ticket=False,
                updated_at=datetime(2020, 9, 1),
            ),
            TrainInfoModel(
                departure_at=datetime.combine(self._min_departure_dt, time.min),
                number='some_number_2',
                facilities_ids=[],
                electronic_ticket=False,
                updated_at=datetime(2020, 9, 1),
            ),
        )

    def test_search_two_train_in_another_day(self):
        with closing(self._storage_store.get('master').get_session()) as session:
            session.add(self._create_train_info(
                departure_id=self._departure_express_id,
                arrival_id=self._arrival_express_id,
                departure_at=datetime.combine(self._min_departure_dt, time.min) + timedelta(days=-2),
                number='previous_day',
            ))
            session.add(self._create_train_info(
                departure_id=self._departure_express_id,
                arrival_id=self._arrival_express_id,
                departure_at=datetime.combine(self._min_departure_dt, time.min) + timedelta(days=2),
                number='next_day',
            ))
            session.commit()

        self._fake_express_system_provider.find_related_express_ids.side_effect = [
            {self._departure_express_id}, {self._arrival_express_id}
        ]

        future, context = self._provider.find(
            departure_point=self._departure_point,
            arrival_point=self._arrival_point,
            departure_date=self._min_departure_dt,
        )

        assert self._fake_logger.warn.call_count == 0
        self._fake_logger.info.assert_called_once_with(
            'Start search: [%s(%s)-%s(%s)-%s-%s]',
            'c10', {self._departure_express_id},
            'c1000', {self._arrival_express_id},
            self._min_departure_dt, 1
        )

        result = wait_for_future_and_build_info(self._provider, future, context, self._timeout)
        assert result == ()

    def test_search_two_train_in_window(self):
        with closing(self._storage_store.get('master').get_session()) as session:
            session.add(self._create_train_info(
                departure_id=self._departure_express_id,
                arrival_id=self._arrival_express_id,
                departure_at=datetime.combine(self._min_departure_dt, time.min) + timedelta(days=-1),
                number='some_number_1',
            ))
            session.add(self._create_train_info(
                departure_id=self._departure_express_id,
                arrival_id=self._arrival_express_id,
                departure_at=datetime.combine(self._min_departure_dt, time.min) + timedelta(days=1),
                number='some_number_2',
            ))
            session.commit()

        self._fake_express_system_provider.find_related_express_ids.side_effect = [
            {self._departure_express_id}, {self._arrival_express_id}
        ]

        future, context = self._provider.find(
            departure_point=self._departure_point,
            arrival_point=self._arrival_point,
            departure_date=self._min_departure_dt,
        )

        assert self._fake_logger.warn.call_count == 0
        self._fake_logger.info.assert_called_once_with(
            'Start search: [%s(%s)-%s(%s)-%s-%s]',
            'c10', {self._departure_express_id},
            'c1000', {self._arrival_express_id},
            self._min_departure_dt, 1
        )

        result = wait_for_future_and_build_info(self._provider, future, context, self._timeout)
        assert result == (
            TrainInfoModel(
                departure_at=datetime.combine(self._min_departure_dt + timedelta(days=-1), time.min),
                number='some_number_1',
                facilities_ids=[],
                electronic_ticket=False,
                updated_at=datetime(2020, 9, 1)
            ),
            TrainInfoModel(
                departure_at=datetime.combine(self._min_departure_dt + timedelta(days=1), time.min),
                number='some_number_2',
                facilities_ids=[],
                electronic_ticket=False,
                updated_at=datetime(2020, 9, 1)
            ),
        )
