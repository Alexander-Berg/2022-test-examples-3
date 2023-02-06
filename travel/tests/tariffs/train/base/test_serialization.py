# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from dateutil.tz import tzoffset
from marshmallow import ValidationError

from common.tester.factories import create_settlement
from common.utils.date import MSK_TZ
from travel.rasp.train_api.tariffs.train.base.serialization import TrainQuerySchema


@pytest.mark.dbuser
@pytest.mark.parametrize('params, expected_message', (
    ({}, 'startTime/endTime or date parameters required'),
    ({'startTime': '2000-01-01T11:00:00+03:00', 'endTime': '2000-01-01T12:00:00'},
     'startTime and endTime should have UTC offset'),
    ({'startTime': '2000-01-01T13:00:00+03:00', 'endTime': '2000-01-01T12:00:00+03:00'},
     'startTime should be less than endTime'),
))
def test_train_query_schema_validate_dates(params, expected_message):
    schema = TrainQuerySchema(strict=True)
    departure_point = create_settlement()
    arrival_point = create_settlement()

    with pytest.raises(ValidationError) as excinfo:
        schema.load(dict({'pointFrom': departure_point.point_key,
                          'pointTo': arrival_point.point_key,
                          'national_version': 'us'},
                         **params))
    assert excinfo.value.messages == {'_schema': [expected_message]}


@pytest.mark.dbuser
@pytest.mark.parametrize('params, expected_start_time, expected_end_time', (
    (
        {'startTime': '2000-01-01T12:00:00+05:00',
         'endTime': '2000-01-01T12:00:00+03:00',
         'date': ['2000-02-01', '2000-02-02', '2000-02-03']},
        datetime(2000, 1, 1, 12, 0, tzinfo=tzoffset(None, 18000)),
        datetime(2000, 1, 1, 12, 0, tzinfo=tzoffset(None, 10800))
    ),
    (
        {'date': ['2000-01-01']},
        MSK_TZ.localize(datetime(2000, 1, 1)),
        MSK_TZ.localize(datetime(2000, 1, 2))
    ),
    (
        {'startTime': '2000-01-01T12:00:00+05:00',
         'date': ['2000-02-03', '2000-02-01']},
        MSK_TZ.localize(datetime(2000, 2, 1)),
        MSK_TZ.localize(datetime(2000, 2, 4))
    )
))
def test_train_query_schema_process_dates(params, expected_start_time, expected_end_time):
    schema = TrainQuerySchema(strict=True)
    departure_point = create_settlement()
    arrival_point = create_settlement()
    data, _errors = schema.load(dict({'pointFrom': departure_point.point_key,
                                      'pointTo': arrival_point.point_key,
                                      'national_version': 'us',
                                      'experiment': True},
                                     **params))

    assert data['start_time'] == expected_start_time
    assert data['end_time'] == expected_end_time
    assert data['experiment']
