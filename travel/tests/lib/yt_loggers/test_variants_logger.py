# -*- coding: utf-8 -*-
import pytest
import pytz
from datetime import datetime
from mock import Mock

from travel.avia.library.python.common.utils import environment, date
from travel.avia.library.python.tester.factories import create_partner, create_dohop_vendor

from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import (
    create_query, create_variant, create_flight
)
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.variants_logger import VariantsLogger
from travel.avia.ticket_daemon.ticket_daemon.lib.yt_loggers.abstract_yt_logger import IObjectLogger


def setup_params():
    reset_all_caches()
    partner = create_partner(
        code='dohop'
    )
    vendor = create_dohop_vendor(
        dohop_id=1
    )

    query = create_query()
    fake_environment = Mock(environment)
    fake_environment.now_aware = Mock(
        return_value=pytz.UTC.localize(datetime(2017, 9, 1)).astimezone(date.MSK_TZ)
    )
    fake_logger = Mock(IObjectLogger)
    actual_logger_calls = []
    fake_logger.log_many = Mock(side_effect=actual_logger_calls.append)

    variants_logger = VariantsLogger(
        logger=fake_logger,
        environment=fake_environment
    )

    common_logged_data = {
        'timestamp': '2017-09-01 03:00:00',
        'timezone': '+0300',
        'partner': partner.code,
        'type': 'plane',
        'date_forward': '2017-09-01',
        'date_backward': None,
        'object_from_type': 'SettlementTuple',
        'object_from_id': query.point_from.id,
        'object_from_title': query.point_from.title,
        'object_to_type': 'SettlementTuple',
        'object_to_id': query.point_to.id,
        'object_to_title': query.point_to.title,
        'class_economy_seats': 1,
        'adults': 1,
        'children': 0,
        'infants': 0,
        'national_version': 'ru',
        'qid': query.id,
    }

    return (
        variants_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    )


@pytest.mark.dbuser
def test_empty():
    (
        variants_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()

    variants_logger.log(
        query=query,
        partner=partner,
        partner_variants=[]
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == []


@pytest.mark.dbuser
def test_common_skip_indirect_variants_one_way():
    (
        variants_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()

    v = create_variant(
        query=query,
        partner=partner,
        forward_flights=[
            create_flight(),
            create_flight()
        ]
    )

    variants_logger.log(
        query=query,
        partner=partner,
        partner_variants=[v, v]
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == []


@pytest.mark.dbuser
def test_common_skip_indirect_variants_return():
    (
        variants_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()
    query.date_backward = date.parse_date('2017-09-09')
    common_logged_data['date_backward'] = '2017-09-09'
    v = create_variant(
        query=query,
        partner=partner,
        forward_flights=[
            create_flight(),
        ],
        backward_flights=[
            create_flight(),
            create_flight(),
        ]
    )

    variants_logger.log(
        query=query,
        partner=partner,
        partner_variants=[v, v]
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == []


@pytest.mark.dbuser
def test_common_one_way():
    (
        variants_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()

    v = create_variant(
        query=query,
        partner=partner,
        forward_flights=[
            create_flight(),
        ]
    )

    variants_logger.log(
        query=query,
        partner=partner,
        partner_variants=[v, v]
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == [
        dict(common_logged_data, **{
            'route_uid': 'FROM-IATA TO-IATA',
            'baggages': '',
            'forward_stops': 0,
            'backward_stops': 0,
            'class_economy_price': '1000 RUR',
            'raw_tariffs': '{}'
        }),
        dict(common_logged_data, **{
            'route_uid': 'FROM-IATA TO-IATA',
            'baggages': '',
            'forward_stops': 0,
            'backward_stops': 0,
            'class_economy_price': '1000 RUR',
            'raw_tariffs': '{}'
        })
    ]


@pytest.mark.dbuser
def test_common_return():
    (
        variants_logger, fake_logger, query,
        partner, vendor, common_logged_data,
        actual_logger_calls
    ) = setup_params()
    query.date_backward = date.parse_date('2017-09-09')
    common_logged_data['date_backward'] = '2017-09-09'
    v = create_variant(
        query=query,
        partner=partner,
        forward_flights=[
            create_flight(),
        ],
        backward_flights=[
            create_flight(),
        ]
    )

    variants_logger.log(
        query=query,
        partner=partner,
        partner_variants=[v, v]
    )

    assert len(actual_logger_calls) == 1
    assert list(actual_logger_calls[0]) == [
        dict(common_logged_data, **{
            'route_uid': 'FROM-IATA TO-IATA;FROM-IATA TO-IATA',
            'baggages': ';',
            'forward_stops': 0,
            'backward_stops': 0,
            'class_economy_price': '1000 RUR',
            'raw_tariffs': '{}'
        }),
        dict(common_logged_data, **{
            'route_uid': 'FROM-IATA TO-IATA;FROM-IATA TO-IATA',
            'baggages': ';',
            'forward_stops': 0,
            'backward_stops': 0,
            'class_economy_price': '1000 RUR',
            'raw_tariffs': '{}'
        })
    ]
