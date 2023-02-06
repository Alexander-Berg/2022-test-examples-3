# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, date

import pytest
from django.conf import settings
from hamcrest import assert_that, only_contains

from common.apps.suburban_events.factories import MovistaCancelRawFactory
from common.apps.suburban_events.models import MovistaCancelRaw
from common.apps.suburban_events.scripts.update_movista_raw_cancels import (
    filter_existing_cancels, get_snapshot_by_date, update_movista_raw_cancels
)
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date.environment import now


def _register_api(httpretty, request_answers):
    def request_callback(request, _, response_headers):
        response = request_answers[json.loads(request.body)['date']]
        return [200, response_headers, json.dumps(response)]

    httpretty.register_uri(
        httpretty.POST, '{}api/v1/timetable/cancels'.format(settings.MOVISTA_API_HOST),
        content_type='application/json', body=request_callback
    )


@pytest.mark.mongouser
def test_get_snapshot_by_date():
    # старая отмена
    MovistaCancelRawFactory(
        create_dt=datetime(2021, 4, 2, 7, 56),
        departure_date=date(2021, 4, 2),
        train_number='thread_1',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=11,
        to_express_id=21
    )

    # свежая отмена
    cancel_1 = MovistaCancelRawFactory(
        create_dt=datetime(2021, 4, 2, 8, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_1',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=11,
        to_express_id=22
    )

    # отмена по другой нитке
    cancel_2 = MovistaCancelRawFactory(
        create_dt=datetime(2021, 4, 2, 7, 58),
        departure_date=date(2021, 4, 2),
        train_number='thread_2',
        start_express_id=31,
        finish_express_id=41,
        from_express_id=32,
        to_express_id=42
    )

    # отмена отмены
    cancel_3 = MovistaCancelRawFactory(
        create_dt=datetime(2021, 4, 2, 8, 58),
        departure_date=date(2021, 4, 2),
        train_number='thread_3',
        start_express_id=51,
        finish_express_id=61,
        from_express_id=None,
        to_express_id=None
    )

    # отмена на другой день
    MovistaCancelRawFactory(
        create_dt=datetime(2021, 4, 1, 10, 0),
        departure_date=date(2021, 4, 1),
        train_number='thread_2',
        start_express_id=51,
        finish_express_id=61,
        from_express_id=51,
        to_express_id=61
    )

    dt = datetime(2021, 4, 2)
    snapshot = get_snapshot_by_date(dt)

    assert len(snapshot.keys()) == 3
    assert snapshot['thread_1'] == cancel_1
    assert snapshot['thread_2'] == cancel_2
    assert snapshot['thread_3'] == cancel_3


@pytest.mark.mongouser
@replace_now('2021-04-02 18:00:00')
def test_filter_existing_cancels():
    old_cancel_1 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 7, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_1',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=11,
        to_express_id=21
    )
    old_cancel_2 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 7, 30),
        departure_date=date(2021, 4, 2),
        train_number='thread_2',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=12,
        to_express_id=21
    )
    old_cancel_4 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 10, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_4',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=None,
        to_express_id=None
    )
    old_cancel_5 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 11, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_5',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=11,
        to_express_id=21
    )
    old_cancel_6 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 11, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_6',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=None,
        to_express_id=None
    )
    snapshot = {
        'thread_1': old_cancel_1,
        'thread_2': old_cancel_2,
        'thread_4': old_cancel_4,
        'thread_5': old_cancel_5,
        'thread_6': old_cancel_6
    }

    # отмена уже есть в базе - пропускаем
    new_cancel_1 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 7, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_1',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=11,
        to_express_id=21
    )

    # изменённая отмена - добавляем
    new_cancel_2 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 8, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_2',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=12,
        to_express_id=22
    )

    # в базе ещё нет отмен по этому номеру на эту дату - добавляем
    new_cancel_3 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 9, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_3',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=12,
        to_express_id=21
    )

    # в базе есть отмена отмены - добавляем
    new_cancel_4 = MovistaCancelRaw(
        create_dt=datetime(2021, 4, 2, 10, 0),
        departure_date=date(2021, 4, 2),
        train_number='thread_4',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=12,
        to_express_id=21
    )

    cancels = [new_cancel_1, new_cancel_2, new_cancel_3, new_cancel_4]

    # по thread_5 не пришло отмены - надо добавить отмену отмены
    annulled_cancel = MovistaCancelRaw(
        create_dt=now(),
        departure_date=date(2021, 4, 2),
        train_number='thread_5',
        start_express_id=11,
        finish_express_id=21,
        from_express_id=None,
        to_express_id=None
    )

    # по thread_6 уже была отмена. нового события не пришло - не добавляем отмену отмены отмены

    filtered_cancels = filter_existing_cancels(cancels, snapshot)
    assert len(filtered_cancels) == 4

    expected_cancels = [new_cancel_2, new_cancel_3, new_cancel_4, annulled_cancel]
    assert_that(filtered_cancels, only_contains(*expected_cancels))


@pytest.mark.mongouser
@replace_now('2021-04-02 18:00:00')
def test_fetch_new_movista_cancels(httpretty):
    request_answers = {
        '2021-04-02': [
            {
                'createDate': '2021-04-02T10:30:00.000',
                'date': u'2021-04-02',
                'trainNumber': 'thread_11',
                'startExpressId': 11,
                'finishExpressId': 21,
                'fromExpressId': 11,
                'toExpressId': 21,
            },
            {
                'createDate': '2021-04-02T10:40:00.000',
                'date': u'2021-04-02',
                'trainNumber': 'thread_12',
                'startExpressId': 21,
                'finishExpressId': 11,
                'fromExpressId': 21,
                'toExpressId': 11,
            }],
        '2021-04-03': [
            {
                'createDate': '2021-04-03T10:30:00.000',
                'date': u'2021-04-03',
                'trainNumber': 'thread_21',
                'startExpressId': 31,
                'finishExpressId': 41,
                'fromExpressId': 31,
                'toExpressId': 41,
            }]
    }
    _register_api(httpretty, request_answers)

    update_movista_raw_cancels()
    cancels = list(MovistaCancelRaw.objects.all())
    assert len(cancels) == 3

    expected_cancels = [
        MovistaCancelRaw(
            create_dt=datetime(2021, 4, 2, 10, 30),
            departure_date=date(2021, 4, 2),
            train_number='thread_11',
            start_express_id=11,
            finish_express_id=21,
            from_express_id=11,
            to_express_id=21
        ),
        MovistaCancelRaw(
            create_dt=datetime(2021, 4, 2, 10, 40),
            departure_date=date(2021, 4, 2),
            train_number='thread_12',
            start_express_id=21,
            finish_express_id=11,
            from_express_id=21,
            to_express_id=11
        ),
        MovistaCancelRaw(
            create_dt=datetime(2021, 4, 3, 10, 30),
            departure_date=date(2021, 4, 3),
            train_number='thread_21',
            start_express_id=31,
            finish_express_id=41,
            from_express_id=31,
            to_express_id=41
        )
    ]
    assert_that(cancels, only_contains(*expected_cancels))

    request_answers = {
        '2021-04-02': [
            {
                'createDate': '2021-04-02T10:40:00.000',
                'date': u'2021-04-02',
                'trainNumber': 'thread_11',
                'startExpressId': 11,
                'finishExpressId': 21,
                'fromExpressId': 12,
                'toExpressId': 22,
            },
            {
                'createDate': '2021-04-02T10:50:00.000',
                'date': u'2021-04-02',
                'trainNumber': 'thread_13',
                'startExpressId': 21,
                'finishExpressId': 11,
                'fromExpressId': 21,
                'toExpressId': 11,
            }],
        '2021-04-03': [
            {
                'createDate': '2021-04-03T10:30:00.000',
                'date': u'2021-04-03',
                'trainNumber': 'thread_21',
                'startExpressId': 31,
                'finishExpressId': 41,
                'fromExpressId': 31,
                'toExpressId': 41,
            }]
    }
    _register_api(httpretty, request_answers)

    update_movista_raw_cancels()
    cancels = list(MovistaCancelRaw.objects.all())
    assert len(cancels) == 6

    expected_cancels.extend([
        MovistaCancelRaw(
            create_dt=datetime(2021, 4, 2, 10, 40),
            departure_date=date(2021, 4, 2),
            train_number='thread_11',
            start_express_id=11,
            finish_express_id=21,
            from_express_id=12,
            to_express_id=22
        ),
        MovistaCancelRaw(
            create_dt=now(),
            departure_date=date(2021, 4, 2),
            train_number='thread_12',
            start_express_id=21,
            finish_express_id=11,
            from_express_id=None,
            to_express_id=None
        ),
        MovistaCancelRaw(
            create_dt=datetime(2021, 4, 2, 10, 50),
            departure_date=date(2021, 4, 2),
            train_number='thread_13',
            start_express_id=21,
            finish_express_id=11,
            from_express_id=21,
            to_express_id=11
        )
    ])
    assert_that(cancels, only_contains(*expected_cancels))
