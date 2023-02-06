# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pymongo
import pytest
from pymongo.collection import Collection

from travel.rasp.train_api.monkey_patch import add_read_retry_to_pymongo
from travel.rasp.train_api.train_purchase.core.factories import TrainOrderFactory
from travel.rasp.train_api.train_purchase.core.models import TrainOrder

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


class FirstException(Exception):
    pass


class SecondException(Exception):
    pass


def _create_response():
    return iter([
        TrainOrderFactory().__dict__,
        TrainOrderFactory().__dict__,
    ])


def test_pymongo_version():
    assert pymongo.version == '3.12.3', 'Версия PyMongo изменилась, проверьте соответствие функций monkey_patch'


def test_pymongo():
    add_read_retry_to_pymongo(max_attempt=2, sleep_duration=0)

    order1 = TrainOrderFactory()
    order2 = TrainOrderFactory()

    assert TrainOrder.objects.count() == 2
    assert TrainOrder.objects.get(uid=order1.uid) == order1
    assert list(TrainOrder.objects.aggregate(*[{'$match': {'uid': order2.uid}}]))[0]['uid'] == order2.uid


def test_over_max_attempts():
    add_read_retry_to_pymongo(max_attempt=2, sleep_duration=0)

    with mock.patch.object(
            Collection, '_rasp_wrapped_find', side_effect=[FirstException, SecondException, _create_response()],
            autospec=True
    ):
        with pytest.raises(SecondException):
            list(TrainOrder.objects.filter(uid='some_uid'))


def test_max_attempts():
    add_read_retry_to_pymongo(max_attempt=3, sleep_duration=0)

    with mock.patch.object(
            Collection, '_rasp_wrapped_find', side_effect=[FirstException, SecondException, _create_response()],
            autospec=True
    ):
        orders = list(TrainOrder.objects.filter(uid='some_uid'))

        assert len(orders) == 2


def test_log_one_retry(caplog):
    add_read_retry_to_pymongo(max_attempt=3, sleep_duration=0)

    with mock.patch.object(
            Collection, '_rasp_wrapped_find', side_effect=[FirstException(), iter([])], autospec=True
    ):
        list(TrainOrder.objects.filter(uid='some_uid'))

        warnings = [record for record in caplog.records if record.levelname == 'WARNING']
        assert len(warnings) == 1


def test_log_no_retry(caplog):
    add_read_retry_to_pymongo(max_attempt=3, sleep_duration=0)

    with mock.patch.object(
            Collection, '_rasp_wrapped_find', side_effect=[iter([])], autospec=True
    ):
        list(TrainOrder.objects.filter(uid='some_uid'))

        warnings = [record for record in caplog.records if record.levelname == 'WARNING']
        assert len(warnings) == 0
