# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from contextlib2 import closing
from mock import Mock

from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.train_info_saver import TrainInfoSaver
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.db_models import TrainInfo
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_place_info_pb2 import TrainPlaceInfo, TrainPlace
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_tariff_info_pb2 import TrainTariffInfo, TrainTariff
from travel.rasp.wizards.train_wizard_api.tests.helpers.postgres_test_case import TestCase
from travel.rasp.wizards.wizard_lib.tests_utils import utc_dt


class TestTrainInfoSaver(TestCase):
    def setUp(self):
        super(TestTrainInfoSaver, self).setUp()
        self._fake_environment = Mock()
        self._fake_environment.now_utc = Mock(return_value=datetime(2020, 1, 1))
        self._saver = TrainInfoSaver(
            storage_store=self._storage_store,
            environment=self._fake_environment
        )

    def test_save(self):
        tariffs_info = TrainTariffInfo(
            tariffs=[
                TrainTariff(seats=42)
            ]
        )
        places_info = TrainPlaceInfo(
            places=[
                TrainPlace(coach_number=999)
            ]
        )
        facilities_ids = (42, 13,)
        self._saver.save(
            query={
                'departure_point_express_id': 1,
                'arrival_point_express_id': 2,
                'departure_at': utc_dt(2017, 9, 1),
                'number': 'number',
            },
            tariffs_info=tariffs_info,
            places_info=places_info,
            facilities_ids=facilities_ids,
            electronic_ticket=True
        )

        with closing(self._storage_store.get('slave').get_session()) as session:
            records = session.query(TrainInfo).all()
        assert len(records) == 1
        record = records[0]
        assert record.tariffs_info == tariffs_info
        assert record.places_info == places_info
        assert record.facilities_ids == [42, 13]
        assert record.electronic_ticket is True
