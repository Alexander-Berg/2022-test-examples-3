from datetime import datetime
from mock import Mock, mock

import pytest
from sqlalchemy import orm
from tornado import escape

from travel.avia.flight_extras.application import db, create_application
from travel.avia.flight_extras.application.handlers.flight import FlightHandler
from travel.avia.flight_extras.application.models import Flight, FlightPassengerExperience
from travel.avia.flight_extras.settings import REST_DATE_FORMAT


@mock.patch('travel.avia.flight_extras.application.db.db_slave')
@mock.patch('sqlalchemy.orm.Query')
def test_flight_get(mock_query, mock_db):
    # type: (orm.Query, db.DB) -> None

    exp = _create_flight_passenger_experience('SU', '100', '2018-01-01')
    _patch_query(mock_query, exp)

    mock_db.create_session.return_value = _create_session(mock_query)

    flight_handler = FlightHandler(_create_application(), Mock())
    flight_handler.get(exp.flight.company_iata, exp.flight.number, exp.departure_day.strftime(REST_DATE_FORMAT))

    assert flight_handler._write_buffer[0] == escape.json_encode(exp.as_dict()).encode('utf-8')


@mock.patch('travel.avia.flight_extras.application.db.db_slave')
@mock.patch('sqlalchemy.orm.Query')
def test_flight_get_not_found(mock_query, mock_db):
    # type: (orm.Query, db.DB) -> None

    _patch_query(mock_query, None)

    mock_db.create_session.return_value = _create_session(mock_query)

    flight_handler = FlightHandler(_create_application(), Mock())
    flight_handler.get('SU', '100', '2018-01-01')

    assert flight_handler._write_buffer[0] == escape.json_encode(FlightPassengerExperience().as_dict()).encode('utf-8')


@mock.patch('travel.avia.flight_extras.application.db.db_slave')
@mock.patch('sqlalchemy.orm.Query')
def test_flight_get_found_nearest_flight_same_day_of_week(mock_query, mock_db):
    # type: (orm.Query, db.DB) -> None

    exp = _create_flight_passenger_experience('SU', '100', '2018-01-01')
    _patch_query(mock_query, exp)

    mock_db.create_session.return_value = _create_session(mock_query)

    flight_handler = FlightHandler(_create_application(), Mock())
    flight_handler.get('SU', '100', '2018-01-08')

    assert flight_handler._write_buffer[0] == escape.json_encode(exp.as_dict()).encode('utf-8').encode('utf-8')


@pytest.skip('TODO: skip reason')
@mock.patch('travel.avia.flight_extras.application.db.db_slave')
@mock.patch('sqlalchemy.orm.Query')
def test_flight_get_not_found_nearest_flight_same_day_of_week(mock_query, mock_db):
    # type: (orm.Query, db.DB) -> None

    exp = _create_flight_passenger_experience('SU', '100', '2018-01-01')
    _patch_query(mock_query, exp)

    mock_db.create_session.return_value = _create_session(mock_query)

    flight_handler = FlightHandler(_create_application(), Mock())
    flight_handler.get('SU', '100', '2018-01-07')

    assert flight_handler._write_buffer[0] == escape.json_encode(FlightPassengerExperience().as_dict()).encode('utf-8')


@mock.patch('travel.avia.flight_extras.application.FlightExtrasApplication')
def _create_application(mock_application):
    mock_application.pre_cache = Mock()
    return create_application()


def _create_flight_passenger_experience(company_iata, number, departure_day):
    # type: (str, str, str) -> FlightPassengerExperience
    flight = Flight(
        company_iata=company_iata,
        number=number,
    )

    return FlightPassengerExperience(
        flight=flight,
        departure_day=datetime.strptime(departure_day, REST_DATE_FORMAT).date(),
    )


def _create_session(mock_query):
    mock_session = Mock()
    mock_session.query.return_value = mock_query

    return mock_session


def _patch_query(mock_query, exp):
    mock_query.join.return_value = mock_query
    mock_query.options.return_value = mock_query
    mock_query.filter.return_value = mock_query
    mock_query.order_by.return_value = mock_query
    mock_query.first.return_value = exp
