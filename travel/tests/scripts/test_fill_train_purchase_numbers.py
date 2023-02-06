# coding: utf-8
from __future__ import unicode_literals

from datetime import time

import pytest
from hamcrest import assert_that, contains_inanyorder
from mock import mock

from common.models.schedule import TrainPurchaseNumber
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_transport_subtype, create_station
from travel.rasp.admin.scripts.fill_train_purchase_numbers import TrainPurchaseNumberCreator


def get_creator():
    return TrainPurchaseNumberCreator(mock.Mock(), truncate_table=False)


@pytest.mark.dbuser
class TestTrainPurchaseNumberCreator(object):
    def test_clean_old_data(self):
        thread = create_thread(t_type=TransportType.SUBURBAN_ID)
        TrainPurchaseNumber.objects.create(thread=thread, number='7001')
        get_creator().run()
        assert TrainPurchaseNumber.objects.count() == 0

    def test_get_matched_trains(self):
        station_from = create_station()
        station_to = create_station()
        same_thread_kwargs = {
            'tz_start_time': time(12, 30),
            'schedule_v1': ((None, 0, station_from), (9, None, station_to))
        }
        subtype = create_transport_subtype(has_train_tariffs=True, t_type=TransportType.SUBURBAN_ID)
        suburban = create_thread(number='7011/7012/7013/7014', t_type=TransportType.SUBURBAN_ID,
                                 t_subtype=subtype, **same_thread_kwargs)
        matched_train = create_thread(t_type=TransportType.TRAIN_ID, **same_thread_kwargs)
        create_thread(t_type=TransportType.BUS_ID, **same_thread_kwargs)
        create_thread(t_type=TransportType.TRAIN_ID, tz_start_time=time(12, 31),
                      schedule_v1=((None, 0, station_from), (9, None, station_to)))
        create_thread(t_type=TransportType.TRAIN_ID, tz_start_time=time(12, 30),
                      schedule_v1=((None, 0, station_from), (12, None, station_to)))
        create_thread(t_type=TransportType.TRAIN_ID, tz_start_time=time(12, 30),
                      schedule_v1=((None, 0, station_from), (9, None, create_station())))
        trains = get_creator().get_matched_trains(suburban)
        assert len(trains) == 1
        assert trains[0] == matched_train

    def test_get_train_numbers(self):
        suburban = create_thread(number='7011/7012/7013/7014')
        valid_train_numbers = ['7011А', '7011', '813', '813А']
        other_numbers = ['8011', '713', '701', '70111']
        trains = [create_thread(number=number) for number in valid_train_numbers + other_numbers]
        numbers = [n.number for n in get_creator().get_train_numbers(suburban, trains)]
        assert len(numbers) == len(valid_train_numbers)
        assert_that(numbers, contains_inanyorder(*valid_train_numbers))

    def test_run_suburban_with_many_numbers(self):
        same_thread_kwargs = {
            'tz_start_time': time(12, 30),
            'schedule_v1': ((None, 0, create_station()), (9, None, create_station()))
        }
        subtype = create_transport_subtype(has_train_tariffs=True, t_type=TransportType.SUBURBAN_ID)
        create_thread(number='7011/7012/7013/7014', t_type=TransportType.SUBURBAN_ID, t_subtype=subtype,
                      **same_thread_kwargs)
        valid_train_numbers = ['7011А', '7011', '813', '813А']
        for number in valid_train_numbers:
            create_thread(number=number, t_type=TransportType.TRAIN_ID, **same_thread_kwargs)
        get_creator().run()
        assert_that([n.number for n in TrainPurchaseNumber.objects.all()], contains_inanyorder(*valid_train_numbers))
