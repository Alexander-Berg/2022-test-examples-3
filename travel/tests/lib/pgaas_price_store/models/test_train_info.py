# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime
from contextlib2 import closing

from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.db_models import TrainInfo
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_place_info_pb2 import TrainPlaceInfo, TrainPlace
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_tariff_info_pb2 import TrainTariffInfo, TrainTariff
from travel.rasp.wizards.train_wizard_api.tests.helpers.postgres_test_case import TestCase


class TestTrainInfoTest(TestCase):
    def test_save_and_read(self):
        now = datetime(2017, 9, 1)
        departure_date = datetime(2018, 1, 1)

        tariffs_info = TrainTariffInfo(
            tariffs=[
                TrainTariff(coach_number=10)
            ]
        )

        places_info = TrainPlaceInfo(
            places=[
                TrainPlace(coach_number=100)
            ]
        )

        facilities_ids = (1, 42,)

        m = TrainInfo(
            departure_point_express_id=1,
            arrival_point_express_id=2,
            departure_at=departure_date,
            number='number',
            tariffs_info=tariffs_info,
            places_info=places_info,
            facilities_ids=facilities_ids,
            created_at=now,
            updated_at=now,
        )

        with closing(self._storage_store.get('slave').get_session()) as session:
            session.add(m)
            records = session.query(TrainInfo).all()

        assert len(records) == 1
        record = records[0]
        assert record.departure_point_express_id == m.departure_point_express_id
        assert record.arrival_point_express_id == m.arrival_point_express_id
        assert record.departure_at == m.departure_at
        assert record.number == m.number
        assert record.tariffs_info == m.tariffs_info
        assert record.places_info == m.places_info
        assert record.places_info == m.places_info
        assert record.facilities_ids == m.facilities_ids
        assert record.created_at == m.created_at
        assert record.updated_at == m.updated_at
