# -*- coding: utf-8 -*-
import copy
import datetime

import pytest
from freezegun import freeze_time

from travel.avia.library.python.common.models.geo import StationType
from travel.avia.library.python.tester.factories import create_settlement, create_station
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.library.python.ticket_daemon.query_validation.validation import (
    validate_query, ValidationError, ErrorCodes
)
from travel.avia.ticket_daemon_api.jsonrpc.models_utils.geo import get_point_tuple_by_key
from travel.avia.ticket_daemon_api.jsonrpc.query import Query


@pytest.fixture(autouse=True)
def reset_caches():
    reset_all_caches()


@pytest.mark.dbuser
@freeze_time("2020-12-12")
def test_validation():
    create_settlement(id=213)
    create_settlement(id=2)

    query = Query(
        point_from=get_point_tuple_by_key('c213'),
        point_to=get_point_tuple_by_key('c2'),
        passengers={
            'adults': 1,
            'children': 0,
            'infants': 0,
        },
        date_forward=datetime.datetime.now().date(),
        national_version='ru',
        lang='ru',
        service='yeah',
    )
    validate_query(query)

    query2 = copy.deepcopy(query)
    query2.point_from = query2.point_to = get_point_tuple_by_key('c213')
    with pytest.raises(ValidationError):
        try:
            validate_query(query2)
        except ValidationError as e:
            assert ErrorCodes.SAME_CITY in e.data
            raise

    query3 = copy.deepcopy(query)
    query3.t_code = 'some_other'
    with pytest.raises(ValidationError):
        validate_query(query3)

    query4 = copy.deepcopy(query)
    query4.date_forward = datetime.datetime.now().date() - datetime.timedelta(days=1)
    with pytest.raises(ValidationError):
        validate_query(query4)


@pytest.mark.dbuser
@freeze_time("2020-12-12")
def test_validation_for_station():
    create_settlement(id=213)
    create_settlement(id=2)
    create_station(id=9600366, settlement_id=2, title='Station from',
                   station_type_id=StationType.AIRPORT_ID)
    create_station(id=9600215, settlement_id=213, title='Station to',
                   station_type_id=StationType.AIRPORT_ID)

    query = Query(
        point_from=get_point_tuple_by_key('s9600366'),
        point_to=get_point_tuple_by_key('s9600215'),
        passengers={
            'adults': 1,
            'children': 0,
            'infants': 0,
        },
        date_forward=datetime.datetime.now().date(),
        national_version='ru',
        lang='ru',
        service='yeah',
    )
    validate_query(query)

    query2 = copy.deepcopy(query)
    query2.point_from = query2.point_to = get_point_tuple_by_key('s9600366')
    with pytest.raises(ValidationError):
        validate_query(query2)


@pytest.mark.dbuser
@freeze_time("2020-12-12")
def test_validation_for_station_same_city():
    create_settlement(id=2)
    create_station(id=9600366, settlement_id=2, title='Station from',
                   station_type_id=StationType.AIRPORT_ID)
    create_station(id=9600215, settlement_id=2, title='Station to',
                   station_type_id=StationType.AIRPORT_ID)

    query = Query(
        point_from=get_point_tuple_by_key('s9600366'),
        point_to=get_point_tuple_by_key('s9600215'),
        passengers={
            'adults': 1,
            'children': 0,
            'infants': 0,
        },
        date_forward=datetime.datetime.now().date(),
        national_version='ru',
        lang='ru',
        service='yeah',
    )
    with pytest.raises(ValidationError):
        validate_query(query)


@pytest.mark.dbuser
@freeze_time("2020-12-12")
@pytest.mark.parametrize(('date_forward', 'date_backward', 'result'), [
    (datetime.date(2020, 12, 14), datetime.date(2020, 12, 16), True),
    (datetime.date(2020, 12, 16), datetime.date(2020, 12, 14), False),
    (datetime.date(2020, 12, 11), datetime.date(2020, 12, 16), False),
    (datetime.date(2022, 12, 11), datetime.date(2020, 12, 16), False),
])
def test_date_validation(date_forward, date_backward, result):
    create_settlement(id=213)
    create_settlement(id=2)

    query = Query(
        point_from=get_point_tuple_by_key('c213'),
        point_to=get_point_tuple_by_key('c2'),
        passengers={
            'adults': 1,
            'children': 0,
            'infants': 0,
        },
        date_forward=date_forward,
        date_backward=date_backward,
        national_version='ru',
        lang='ru',
        service='yeah',
    )
    if result:
        validate_query(query)
    else:
        with pytest.raises(ValidationError):
            validate_query(query)
